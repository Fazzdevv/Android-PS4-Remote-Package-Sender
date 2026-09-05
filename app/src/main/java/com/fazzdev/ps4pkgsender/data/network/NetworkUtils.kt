package com.fazzdev.ps4pkgsender.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Resolves local IPv4 address using ConnectivityManager & LinkProperties (Android 10+ compliant).
     * Does NOT require ACCESS_FINE_LOCATION permission (unlike WifiManager.getConnectionInfo()).
     */
    fun getLocalIpAddress(context: Context): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return getFallbackIpAddress()

        val activeNetwork = connectivityManager.activeNetwork ?: return getFallbackIpAddress()
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return getFallbackIpAddress()

        // Prioritize Wi-Fi or Ethernet IPv4 address
        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is Inet4Address && !address.isLoopbackAddress) {
                val hostAddress = address.hostAddress
                if (hostAddress != null && !hostAddress.startsWith("127.")) {
                    return hostAddress
                }
            }
        }

        return getFallbackIpAddress()
    }

    /**
     * Fallback to scanning NetworkInterfaces directly if ConnectivityManager returns null.
     */
    private fun getFallbackIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // Avoid loopback, virtual, or inactive interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address: InetAddress = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress
                        if (ip != null && !ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    /**
     * Checks if the device is currently connected to a Wi-Fi or Ethernet network.
     */
    fun isConnectedToWifiOrLan(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
