package com.rpeters.jellyfin.ui.player.dlna

import androidx.media3.common.util.UnstableApi
import com.rpeters.jellyfin.utils.SecureLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@UnstableApi
@Singleton
class DlnaDiscoveryController @Inject constructor() {
    companion object {
        private const val TAG = "DlnaDiscovery"
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val DISCOVERY_WINDOW_MS = 4_000L
        private const val RECEIVE_TIMEOUT_MS = 700
        private const val DISPLAY_PREFIX = "DLNA: "
    }

    private var discoveryJob: Job? = null
    @Volatile
    private var lastDevices: List<DlnaDevice> = emptyList()

    fun startDiscovery(scope: CoroutineScope, onDevicesUpdated: (List<DlnaDevice>) -> Unit) {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            val devices = discoverDevices()
            lastDevices = devices
            onDevicesUpdated(devices)
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    fun getDisplayNames(): List<String> = lastDevices.map { "$DISPLAY_PREFIX${it.friendlyName}" }

    fun findByDisplayName(displayName: String): DlnaDevice? {
        if (!displayName.startsWith(DISPLAY_PREFIX)) return null
        val rawName = displayName.removePrefix(DISPLAY_PREFIX)
        return lastDevices.firstOrNull { it.friendlyName == rawName }
    }

    private suspend fun discoverDevices(): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val locations = linkedSetOf<String>()
        val request = buildMSearchRequest()
        val endAt = System.currentTimeMillis() + DISCOVERY_WINDOW_MS

        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = RECEIVE_TIMEOUT_MS
                val packet = DatagramPacket(
                    request,
                    request.size,
                    InetAddress.getByName(SSDP_ADDRESS),
                    SSDP_PORT,
                )
                socket.send(packet)

                while (isActive && System.currentTimeMillis() < endAt) {
                    val buffer = ByteArray(8192)
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(responsePacket)
                        val response = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                        parseLocationHeader(response)?.let { locations.add(it) }
                    } catch (_: SocketTimeoutException) {
                        delay(80)
                    }
                }
            }
        }.onFailure { e ->
            SecureLogger.w(TAG, "DLNA discovery failed", e)
        }

        val devices = locations.mapNotNull { location ->
            runCatching { buildDeviceFromLocation(location) }
                .onFailure { e -> SecureLogger.w(TAG, "Failed to parse DLNA description at $location", e) }
                .getOrNull()
        }.distinctBy { it.udn }

        SecureLogger.d(TAG, "Discovered ${devices.size} DLNA device(s)")
        devices
    }

    private fun buildMSearchRequest(): ByteArray {
        val request = """
            M-SEARCH * HTTP/1.1
            HOST: 239.255.255.250:1900
            MAN: "ssdp:discover"
            MX: 2
            ST: urn:schemas-upnp-org:device:MediaRenderer:1
            
        """.trimIndent().replace("\n", "\r\n")
        return request.toByteArray(Charsets.UTF_8)
    }

    private fun parseLocationHeader(response: String): String? {
        return response.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("location:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
    }

    private fun buildDeviceFromLocation(location: String): DlnaDevice? {
        val xml = URL(location).openStream().use { input ->
            val out = ByteArrayOutputStream()
            input.copyTo(out)
            out.toString(Charsets.UTF_8.name())
        }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())

        val rootDevice = doc.getElementsByTagName("device").item(0) as? Element ?: return null
        val friendlyName = rootDevice.getElementsByTagName("friendlyName").item(0)?.textContent?.trim()
            ?: return null
        val udn = rootDevice.getElementsByTagName("UDN").item(0)?.textContent?.trim().orEmpty()

        val services = rootDevice.getElementsByTagName("service")
        var avTransportControlUrl: String? = null
        for (i in 0 until services.length) {
            val service = services.item(i) as? Element ?: continue
            val serviceType = service.getElementsByTagName("serviceType").item(0)?.textContent.orEmpty()
            if (!serviceType.contains("AVTransport", ignoreCase = true)) continue
            val controlUrl = service.getElementsByTagName("controlURL").item(0)?.textContent?.trim().orEmpty()
            if (controlUrl.isNotBlank()) {
                avTransportControlUrl = resolveUrl(location, controlUrl)
                break
            }
        }

        if (avTransportControlUrl.isNullOrBlank()) return null
        return DlnaDevice(
            friendlyName = friendlyName,
            udn = udn.ifBlank { location },
            locationUrl = location,
            avTransportControlUrl = avTransportControlUrl,
        )
    }

    private fun resolveUrl(base: String, maybeRelative: String): String {
        return try {
            URI(base).resolve(maybeRelative).toString()
        } catch (_: Exception) {
            maybeRelative
        }
    }
}

