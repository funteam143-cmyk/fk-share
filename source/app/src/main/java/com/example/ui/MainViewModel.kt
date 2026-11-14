package com.example.ui

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.TransferRecord
import com.example.data.db.TransferRepository
import com.example.data.model.PeerDevice
import com.example.data.model.SessionState
import com.example.data.model.TransferItem
import com.example.data.model.TransferProgressState
import com.example.data.model.TransferProposal
import com.example.data.model.TransferStatus
import com.example.network.P2pDiscoveryManager
import com.example.network.TransferEngine
import com.example.util.NetworkUtils
import com.example.util.StorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class Screen {
    MAIN,
    FILE_PICKER,
    DISCOVERY,
    RECEIVER_WAITING,
    TRANSFER,
    COMPLETED,
    HISTORY
}

enum class MediaTab {
    ALL,
    PHOTOS,
    VIDEOS,
    DOCUMENTS,
    AUDIO,
    APPS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransferRepository(AppDatabase.getInstance(application).transferRecordDao())
    val recentRecords: StateFlow<List<TransferRecord>> = repository.recentRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRecords: StateFlow<List<TransferRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveryManager = P2pDiscoveryManager(application, viewModelScope)
    val transferEngine = TransferEngine(application, viewModelScope)

    val peers: StateFlow<List<PeerDevice>> = discoveryManager.peers
    val transferProgress: StateFlow<TransferProgressState> = transferEngine.progressState

    private val _currentScreen = MutableStateFlow(Screen.MAIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<TransferItem>>(emptyList())
    val selectedFiles: StateFlow<List<TransferItem>> = _selectedFiles.asStateFlow()

    private val _localDeviceName = MutableStateFlow(NetworkUtils.getFriendlyDeviceName())
    val localDeviceName: StateFlow<String> = _localDeviceName.asStateFlow()

    private val _localIp = MutableStateFlow(NetworkUtils.getLocalIpAddress() ?: "Offline")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _scannedMedia = MutableStateFlow<List<TransferItem>>(emptyList())
    val scannedMedia: StateFlow<List<TransferItem>> = _scannedMedia.asStateFlow()

    private val _selectedTab = MutableStateFlow(MediaTab.PHOTOS)
    val selectedTab: StateFlow<MediaTab> = _selectedTab.asStateFlow()

    private val _isLoadingMedia = MutableStateFlow(false)
    val isLoadingMedia: StateFlow<Boolean> = _isLoadingMedia.asStateFlow()

    private val _lastCompletedTransfer = MutableStateFlow<TransferProgressState?>(null)
    val lastCompletedTransfer: StateFlow<TransferProgressState?> = _lastCompletedTransfer.asStateFlow()

    init {
        refreshNetworkState()
        loadLocalMedia(MediaTab.PHOTOS)
    }

    fun refreshNetworkState() {
        _localIp.value = NetworkUtils.getLocalIpAddress() ?: "Offline"
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.DISCOVERY) {
            refreshNetworkState()
            discoveryManager.startDiscovery(asReceiver = false)
        } else if (screen == Screen.RECEIVER_WAITING) {
            refreshNetworkState()
            discoveryManager.startDiscovery(asReceiver = true)
            startReceiverServer()
        } else {
            if (_currentScreen.value != Screen.TRANSFER) {
                discoveryManager.stopDiscovery()
            }
        }
    }

    fun selectTab(tab: MediaTab) {
        _selectedTab.value = tab
        loadLocalMedia(tab)
    }

    fun addFilesFromUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = getApplication<Application>().contentResolver
            val newItems = uris.map { uri ->
                val (name, size) = StorageHelper.resolveUriMetadata(contentResolver, uri)
                val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                TransferItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    size = size,
                    mimeType = mime,
                    uri = uri,
                    status = TransferStatus.PENDING
                )
            }
            val current = _selectedFiles.value.toMutableList()
            current.addAll(newItems)
            _selectedFiles.value = current
        }
    }

    fun toggleFileSelection(item: TransferItem) {
        val current = _selectedFiles.value.toMutableList()
        val existing = current.indexOfFirst { it.id == item.id || (it.uri != null && it.uri == item.uri) }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(item)
        }
        _selectedFiles.value = current
    }

    fun removeSelectedFile(item: TransferItem) {
        val current = _selectedFiles.value.toMutableList()
        current.removeAll { it.id == item.id }
        _selectedFiles.value = current
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun connectAndSend(peer: PeerDevice) {
        val filesToSend = _selectedFiles.value
        if (filesToSend.isEmpty()) return

        discoveryManager.stopDiscovery()
        _currentScreen.value = Screen.TRANSFER

        transferEngine.startSender(
            targetHost = peer.hostAddress,
            targetPort = peer.port,
            peerDeviceName = peer.name,
            selectedFiles = filesToSend,
            onTransferComplete = { finalState ->
                onTransferFinished(finalState)
            }
        )
    }

    private fun startReceiverServer() {
        transferEngine.startReceiver { finalState ->
            onTransferFinished(finalState)
        }
    }

    fun acceptIncomingTransfer() {
        transferEngine.respondToProposal(true)
        _currentScreen.value = Screen.TRANSFER
    }

    fun declineIncomingTransfer() {
        transferEngine.respondToProposal(false)
        _currentScreen.value = Screen.MAIN
    }

    fun cancelActiveTransfer() {
        transferEngine.cancelActiveTransfer()
    }

    private fun onTransferFinished(state: TransferProgressState) {
        _lastCompletedTransfer.value = state
        if (state.sessionState == SessionState.COMPLETED) {
            _currentScreen.value = Screen.COMPLETED
            // Persist to Room
            viewModelScope.launch(Dispatchers.IO) {
                val record = TransferRecord(
                    peerName = state.peerName.ifEmpty { "Unknown Peer" },
                    isOutgoing = state.isSender,
                    fileCount = state.items.size,
                    totalBytes = state.totalBytes,
                    averageSpeedMBps = state.averageSpeedMBps,
                    peakSpeedMBps = state.peakSpeedMBps,
                    durationSeconds = state.elapsedSeconds,
                    status = "COMPLETED",
                    fileNamesSummary = state.items.joinToString(", ") { it.name }.take(250)
                )
                repository.saveRecord(record)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    fun addDirectPeer(ip: String, port: Int = NetworkUtils.DEFAULT_TRANSFER_PORT) {
        discoveryManager.addDirectPeer(ip, port)
    }

    private fun loadLocalMedia(tab: MediaTab) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMedia.value = true
            val items = mutableListOf<TransferItem>()
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver

            try {
                when (tab) {
                    MediaTab.PHOTOS -> {
                        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.MIME_TYPE)
                        contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            null,
                            null,
                            "${MediaStore.Images.Media.DATE_ADDED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                            val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                            var count = 0
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idCol)
                                val name = cursor.getString(nameCol) ?: "Image_$id"
                                val size = cursor.getLong(sizeCol)
                                val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                                items.add(TransferItem(id = id.toString(), name = name, size = size, mimeType = mime, uri = uri))
                                count++
                            }
                        }
                    }
                    MediaTab.VIDEOS -> {
                        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.MIME_TYPE)
                        contentResolver.query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            null,
                            null,
                            "${MediaStore.Video.Media.DATE_ADDED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                            val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                            var count = 0
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idCol)
                                val name = cursor.getString(nameCol) ?: "Video_$id"
                                val size = cursor.getLong(sizeCol)
                                val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                                items.add(TransferItem(id = id.toString(), name = name, size = size, mimeType = mime, uri = uri))
                                count++
                            }
                        }
                    }
                    MediaTab.AUDIO -> {
                        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.MIME_TYPE)
                        contentResolver.query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            null,
                            null,
                            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                            val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                            var count = 0
                            while (cursor.moveToNext() && count < 40) {
                                val id = cursor.getLong(idCol)
                                val name = cursor.getString(nameCol) ?: "Audio_$id"
                                val size = cursor.getLong(sizeCol)
                                val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                                items.add(TransferItem(id = id.toString(), name = name, size = size, mimeType = mime, uri = uri))
                                count++
                            }
                        }
                    }
                    MediaTab.DOCUMENTS -> {
                        val collection = MediaStore.Files.getContentUri("external")
                        val projection = arrayOf(
                            MediaStore.Files.FileColumns._ID,
                            MediaStore.Files.FileColumns.DISPLAY_NAME,
                            MediaStore.Files.FileColumns.SIZE,
                            MediaStore.Files.FileColumns.MIME_TYPE
                        )
                        val documentMimes = arrayOf(
                            "application/pdf", "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "text/plain", "text/csv", "application/rtf"
                        )
                        val placeholders = documentMimes.joinToString(",") { "?" }
                        contentResolver.query(
                            collection, projection,
                            "${MediaStore.Files.FileColumns.MIME_TYPE} IN ($placeholders)",
                            documentMimes, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                            var count = 0
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idCol)
                                val uri = ContentUris.withAppendedId(collection, id)
                                items.add(TransferItem(
                                    id = "doc_$id",
                                    name = cursor.getString(nameCol) ?: "Document_$id",
                                    size = cursor.getLong(sizeCol),
                                    mimeType = cursor.getString(mimeCol) ?: "application/octet-stream",
                                    uri = uri
                                ))
                                count++
                            }
                        }
                    }
                    MediaTab.APPS -> {
                        val pm = context.packageManager
                        val apps = if (Build.VERSION.SDK_INT >= 33) {
                            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getInstalledApplications(0)
                        }
                        apps.asSequence()
                            .filter { app -> app.sourceDir != null && File(app.sourceDir).isFile && (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
                            .toList()
                            .forEach { app ->
                                val apk = File(app.sourceDir)
                                val label = pm.getApplicationLabel(app).toString()
                                items.add(TransferItem(
                                    id = "app_${app.packageName}",
                                    name = label,
                                    size = apk.length(),
                                    mimeType = "application/vnd.android.package-archive",
                                    uri = Uri.fromFile(apk)
                                ))
                            }
                    }
                    MediaTab.ALL -> {
                        val collections = listOf(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "image",
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "video",
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to "audio"
                        )
                        collections.forEach { (collection, kind) ->
                            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.MIME_TYPE)
                            contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                                var count = 0
                                while (cursor.moveToNext()) {
                                    val id = cursor.getLong(idCol)
                                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "${kind}/*" else "${kind}/*"
                                    items.add(TransferItem(id = "${kind}_$id", name = cursor.getString(nameCol) ?: "${kind}_$id", size = cursor.getLong(sizeCol), mimeType = mime, uri = ContentUris.withAppendedId(collection, id)))
                                    count++
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Media query failed: ${e.message}")
            } finally {
                _scannedMedia.value = items
                _isLoadingMedia.value = false
            }
        }
    }
}
