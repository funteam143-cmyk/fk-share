package com.example.util

import android.os.Build
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object NetworkUtils {
    const val DEFAULT_BEACON_PORT = 52345
    const val DEFAULT_TRANSFER_PORT = 52346

    /** Find a usable local IPv4 address, preferring Wi-Fi/hotspot/P2P interfaces. */
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { priority(it.name) }
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                val candidate = addresses.filterIsInstance<Inet4Address>().firstOrNull {
                    !it.isLoopbackAddress && !it.isLinkLocalAddress &&
                        !it.hostAddress.orEmpty().startsWith("169.254.")
                }
                if (candidate != null) return candidate.hostAddress
            }
            null
        } catch (_: Exception) { null }
    }

    private fun priority(name: String): Int {
        val n = name.lowercase(Locale.ROOT)
        return when {
            n.startsWith("wlan") || n.startsWith("swlan") || n.contains("wifi") -> 0
            n.startsWith("ap") || n.contains("p2p") -> 1
            else -> 2
        }
    }

    private fun preferredWifiInterface(): NetworkInterface? {
        return try {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .filter { it.isUp && !it.isLoopback && isWifiOrP2pInterface(it.name) }
                .sortedBy { priority(it.name) }
                .firstOrNull()
        } catch (_: Exception) { null }
    }

    private fun isWifiOrP2pInterface(name: String): Boolean {
        val n = name.lowercase(Locale.ROOT)
        return n.contains("wlan") || n.contains("swlan") || n.startsWith("ap") ||
            n.contains("p2p") || n.contains("wifi")
    }

    fun getFriendlyDeviceName(): String {
        val model = Build.MODEL ?: "Android"
        val manufacturer = Build.MANUFACTURER ?: ""
        val combined = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        val clean = combined.trim().replace(Regex("[^a-zA-Z0-9 -]"), "").take(20)
        return "FK-$clean"
    }

    fun getBroadcastAddress(): InetAddress {
        try {
            val broadcast = preferredWifiInterface()?.interfaceAddresses
                ?.mapNotNull { it.broadcast as? Inet4Address }?.firstOrNull()
            if (broadcast != null) return broadcast
        } catch (_: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }
}
