package com.example.util

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object NetworkUtils {
    const val DEFAULT_BEACON_PORT = 52345
    const val DEFAULT_TRANSFER_PORT = 52346

    /**
     * Resolves the primary local IPv4 address of the active Wi-Fi or Hotspot interface.
     */
    fun getLocalIpAddress(): String? {
        return try {
            preferredWifiInterface()?.inetAddresses?.let { addresses ->
                Collections.list(addresses).filterIsInstance<Inet4Address>()
                    .firstOrNull { !it.isLoopbackAddress && !it.hostAddress.orEmpty().startsWith("169.254.") }
                    ?.hostAddress
            }
        } catch (_: Exception) { null }
    }

    private fun preferredWifiInterface(): NetworkInterface? {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback && isWifiOrP2pInterface(it.name) }
        return interfaces.sortedByDescending { networkInterface ->
            val name = networkInterface.name.lowercase(Locale.ROOT)
            name.startsWith("wlan") || name.startsWith("swlan") || name.startsWith("ap") || name.contains("wifi")
        }.firstOrNull()
    }

    private fun isWifiOrP2pInterface(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower.contains("wlan") || lower.contains("swlan") ||
            lower.startsWith("ap") || lower.contains("p2p") || lower.contains("wifi")
    }

    /**
     * Derives a friendly device name (e.g. "FK-Pixel 8", "FK-Galaxy S24").
     */
    fun getFriendlyDeviceName(): String {
        val model = Build.MODEL ?: "Android"
        val manufacturer = Build.MANUFACTURER ?: ""
        val combined = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }.trim()
        val clean = combined.replace(Regex("[^a-zA-Z0-9 -]"), "").take(20)
        return "FK-$clean"
    }

    /**
     * Returns broadcast address for the local subnet.
     */
    fun getBroadcastAddress(): InetAddress {
        try {
            val broadcast = preferredWifiInterface()?.interfaceAddresses
                ?.mapNotNull { it.broadcast as? Inet4Address }?.firstOrNull()
            if (broadcast != null) return broadcast
        } catch (_: Exception) {}
        return InetAddress.getByName("255.255.255.255")
    }
}
