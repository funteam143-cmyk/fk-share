package com.example.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.*
import com.example.util.NetworkUtils
import com.example.util.StorageHelper
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NearbyTransferManager(private val context: Context, private val scope: CoroutineScope) {
    companion object { private const val TAG = "FKNearby"; private const val SERVICE_ID = "com.aistudio.fkshare.offline.cfzkua.nearby" }
    private val client = Nearby.getConnectionsClient(context)
    private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peers = _peers.asStateFlow()
    private val _progress = MutableStateFlow(TransferProgressState())
    val progress = _progress.asStateFlow()
    private var endpoint: String? = null
    private var outgoing = emptyList<TransferItem>()
    private var incomingCount = 0
    private var receivedCount = 0
    private val payloadIndexes = ConcurrentHashMap<Long, Int>()
    private val tempFiles = ConcurrentHashMap<Long, File>()

    private fun fail(message: String) {
        Log.e(TAG, message)
        _progress.value = _progress.value.copy(sessionState = SessionState.ERROR, errorMessage = message)
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val json = runCatching { JSONObject(String(payload.asBytes() ?: byteArrayOf(), Charsets.UTF_8)) }.getOrNull() ?: return
                when (json.optString("kind")) {
                    "proposal" -> {
                        val array = json.optJSONArray("files") ?: JSONArray()
                        val files = (0 until array.length()).map { i ->
                            val f = array.getJSONObject(i)
                            FileMeta(f.optString("name"), f.optLong("size"), f.optString("mime"))
                        }
                        incomingCount = files.size
                        receivedCount = 0
                        _progress.value = TransferProgressState(
                            sessionState = SessionState.WAITING_ACCEPTANCE, isSender = false,
                            peerName = json.optString("sender"), totalBytes = json.optLong("total"),
                            items = files.map { TransferItem(UUID.randomUUID().toString(), it.name, it.size, it.mimeType) },
                            pendingProposal = TransferProposal(json.optString("sender"), files.size, json.optLong("total"), files)
                        )
                    }
                    "accept" -> if (json.optBoolean("yes")) sendFiles(endpointId) else fail("Receiver declined transfer")
                    "done" -> complete()
                }
            } else if (payload.type == Payload.Type.FILE) {
                val filePayload = payload.asFile() ?: return
                val name = StorageHelper.sanitizeFileName(filePayload.fileName ?: "received_file")
                val uri = filePayload.asUri()
                scope.launch(Dispatchers.IO) {
                    try {
                        val output = StorageHelper.createReceivedFile(context, name)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(output).use { out -> input.copyTo(out, 256 * 1024) }
                        } ?: throw IllegalStateException("Cannot open received payload")
                        receivedCount++
                        val items = _progress.value.items.toMutableList()
                        val i = items.indexOfFirst { it.name == name && it.status != TransferStatus.COMPLETED }
                        if (i >= 0) items[i] = items[i].copy(status = TransferStatus.COMPLETED, progress = 1f, localUri = Uri.fromFile(output))
                        _progress.value = _progress.value.copy(items = items, totalTransferredBytes = items.sumOf { it.size })
                    } catch (e: Exception) { fail("Saving " + name + " failed: " + e.message) }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.FAILURE) fail("Nearby payload transfer failed")
            if (update.status == PayloadTransferUpdate.Status.IN_PROGRESS) {
                val i = payloadIndexes[update.payloadId] ?: return
                val items = _progress.value.items.toMutableList()
                if (i in items.indices) items[i] = items[i].copy(status = TransferStatus.TRANSFERRING,
                    progress = if (update.totalBytes > 0) update.bytesTransferred.toFloat() / update.totalBytes else 0f,
                    transferredBytes = update.bytesTransferred)
                _progress.value = _progress.value.copy(items = items, totalTransferredBytes = items.sumOf { it.transferredBytes })
            }
            if (update.status == PayloadTransferUpdate.Status.SUCCESS && payloadIndexes.containsKey(update.payloadId)) {
                val i = payloadIndexes.remove(update.payloadId)!!
                val items = _progress.value.items.toMutableList()
                if (i in items.indices) items[i] = items[i].copy(status = TransferStatus.COMPLETED, progress = 1f, transferredBytes = items[i].size)
                _progress.value = _progress.value.copy(items = items, totalTransferredBytes = items.sumOf { it.transferredBytes })
                if (payloadIndexes.isEmpty() && items.isNotEmpty()) {
                    val target = endpoint
                    if (target != null) {
                        client.sendPayload(target, Payload.fromBytes(JSONObject().put("kind", "done").toString().toByteArray()))
                            .addOnSuccessListener { complete() }
                            .addOnFailureListener { fail("Final transfer acknowledgement failed: " + it.message) }
                    } else complete()
                }
            }
        }
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) { client.acceptConnection(id, payloadCallback) }
        override fun onConnectionResult(id: String, result: ConnectionResolution) {
            if (result.status.statusCode != ConnectionsStatusCodes.STATUS_OK) {
                fail("Nearby connection failed: " + result.status.statusCode); return
            }
            endpoint = id
            if (_progress.value.isSender) sendProposal(id)
            else _progress.value = _progress.value.copy(sessionState = SessionState.CONNECTING, peerName = "Connected")
        }
        override fun onDisconnected(id: String) {
            if (_progress.value.sessionState != SessionState.COMPLETED) fail("Nearby device disconnected")
            endpoint = null
        }
    }

    fun startDiscovery() {
        stopDiscovery()
        _progress.value = TransferProgressState(sessionState = SessionState.DISCOVERING, isSender = true)
        client.startDiscovery(SERVICE_ID, object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                _peers.value = _peers.value.filterNot { it.id == id } +
                    PeerDevice(id, info.endpointName, "", connectionType = ConnectionType.WIFI_DIRECT)
            }
            override fun onEndpointLost(id: String) { _peers.value = _peers.value.filterNot { it.id == id } }
        }, DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
            .addOnFailureListener { fail("Nearby discovery error: " + it.message) }
    }

    fun startReceiver() {
        stopDiscovery()
        _progress.value = TransferProgressState(sessionState = SessionState.DISCOVERING, isSender = false, peerName = "Waiting for sender")
        client.startAdvertising(NetworkUtils.getFriendlyDeviceName(), SERVICE_ID, lifecycle,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
            .addOnFailureListener { fail("Nearby advertising error: " + it.message) }
    }

    fun connectAndSend(peer: PeerDevice, files: List<TransferItem>) {
        outgoing = files
        _progress.value = TransferProgressState(sessionState = SessionState.CONNECTING, isSender = true,
            peerName = peer.name, items = files, totalBytes = files.sumOf { it.size })
        client.requestConnection(NetworkUtils.getFriendlyDeviceName(), peer.id, lifecycle)
            .addOnFailureListener { fail("Connection request failed: " + it.message) }
    }

    private fun sendProposal(id: String) {
        val files = JSONArray()
        outgoing.forEach { files.put(JSONObject().put("name", it.name).put("size", it.size).put("mime", it.mimeType)) }
        val json = JSONObject().put("kind", "proposal").put("sender", NetworkUtils.getFriendlyDeviceName())
            .put("total", outgoing.sumOf { it.size }).put("files", files)
        client.sendPayload(id, Payload.fromBytes(json.toString().toByteArray(Charsets.UTF_8)))
            .addOnFailureListener { fail("Proposal send failed: " + it.message) }
    }

    fun respondToProposal(accept: Boolean) {
        val id = endpoint ?: return
        val data = JSONObject().put("kind", "accept").put("yes", accept).toString().toByteArray()
        client.sendPayload(id, Payload.fromBytes(data)).addOnFailureListener { fail(it.message ?: "Response failed") }
        if (accept) _progress.value = _progress.value.copy(sessionState = SessionState.TRANSFERRING, pendingProposal = null)
        else { _progress.value = _progress.value.copy(sessionState = SessionState.CANCELLED); stopAll() }
    }

    private fun sendFiles(id: String) {
        _progress.value = _progress.value.copy(sessionState = SessionState.TRANSFERRING)
        scope.launch(Dispatchers.IO) {
            try {
                outgoing.forEachIndexed { index, item ->
                    val uri = item.uri ?: throw IllegalStateException("Missing URI for " + item.name)
                    val temp = File(context.cacheDir, "fknearby_" + System.nanoTime() + "_" + StorageHelper.sanitizeFileName(item.name))
                    context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { out -> input.copyTo(out, 256 * 1024) } }
                        ?: throw IllegalStateException("Cannot read " + item.name)
                    val payload = Payload.fromFile(temp)
                    payloadIndexes[payload.id] = index
                    tempFiles[payload.id] = temp
                    client.sendPayload(id, payload).addOnFailureListener { fail("File send failed: " + it.message) }
                }
            } catch (e: Exception) { fail(e.message ?: "Could not prepare files") }
        }
    }

    private fun complete() {
        tempFiles.values.forEach { it.delete() }; tempFiles.clear()
        _progress.value = _progress.value.copy(sessionState = SessionState.COMPLETED, pendingProposal = null, currentSpeedMBps = 0.0)
        stopAll()
    }

    fun cancelActiveTransfer() {
        _progress.value = _progress.value.copy(sessionState = SessionState.CANCELLED, errorMessage = "Transfer cancelled")
        stopAll()
    }

    fun stopDiscovery() { client.stopDiscovery(); client.stopAdvertising(); _peers.value = emptyList() }
    private fun stopAll() {
        client.stopDiscovery(); client.stopAdvertising()
        endpoint?.let { client.disconnectFromEndpoint(it) }
        endpoint = null
        tempFiles.values.forEach { it.delete() }; tempFiles.clear()
    }
}
