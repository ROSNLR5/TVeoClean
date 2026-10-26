package com.example.core.adb

import android.content.Context
import android.net.ConnectivityManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.Collections
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

import com.example.ui.i18n.AppLanguage

data class LanScanDeviceHit(
    val ipAddress: String,
    val adbPort: Int = 5555,
    val isAdbPortOpen: Boolean,
    val isCastTvDetected: Boolean,
    val friendlyName: String,
    val modelHint: String? = null
)

data class LanScanProgress(
    val isScanning: Boolean = false,
    val localIp: String? = null,
    val subnetPrefix: String? = null,
    val scannedHosts: Int = 0,
    val totalHosts: Int = 254,
    val discoveredHosts: List<LanScanDeviceHit> = emptyList(),
    val errorEs: String? = null,
    val errorEn: String? = null
) {
    val progressFraction: Float
        get() = if (totalHosts <= 0) 0f else (scannedHosts.toFloat() / totalHosts.toFloat()).coerceIn(0f, 1f)

    fun localizedError(lang: AppLanguage): String? =
        if (lang == AppLanguage.ES) errorEs else errorEn
}

interface ActiveNetworkScanner {
    val scanState: StateFlow<LanScanProgress>
    fun detectLocalPrivateIpv4(): String?
    suspend fun scanSubnet(customSubnetPrefix: String? = null)
    fun resetError()
}

/**
 * Production automatic LAN /24 scanner that discovers Android TV / Google TV / Fire TV devices
 * even when mDNS (`_adb._tcp`) is silenced by the router or TV firmware.
 *
 * Probes inside private RFC1918 LANs ONLY:
 * - TCP 5555 (Standard ADB over Network)
 * - TCP 8008 / 8009 (Android TV / Google Cast presence + local `/setup/eureka_info` friendly name)
 */
class ActiveLanSubnetScanner(
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 260,
    private val maxConcurrency: Int = 36
) : ActiveNetworkScanner {

    private val _scanState = MutableStateFlow(
        LanScanProgress(
            localIp = detectLocalPrivateIpv4(),
            subnetPrefix = detectLocalPrivateIpv4()?.substringBeforeLast('.')
        )
    )
    override val scanState: StateFlow<LanScanProgress> = _scanState.asStateFlow()

    override fun detectLocalPrivateIpv4(): String? {
        // 1. Prefer active Wi-Fi / Ethernet LinkProperties via ConnectivityManager
        runCatching {
            val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val linkProps = if (activeNet != null) cm.getLinkProperties(activeNet) else null
            linkProps?.linkAddresses?.forEach { linkAddr ->
                val addr = linkAddr.address
                if (addr is Inet4Address && !addr.isLoopbackAddress && isPrivateRfc1918(addr.hostAddress ?: "")) {
                    return addr.hostAddress
                }
            }
        }
        // 2. Fallback to NetworkInterface enumeration
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching null
            for (nif in Collections.list(interfaces)) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in Collections.list(nif.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (isPrivateRfc1918(ip)) return@runCatching ip
                    }
                }
            }
            null
        }.getOrNull()
    }

    override suspend fun scanSubnet(customSubnetPrefix: String?) = withContext(ioDispatcher) {
        val detectedIp = detectLocalPrivateIpv4()
        val rawPrefix = customSubnetPrefix?.trim()?.removeSuffix(".")
            ?: detectedIp?.substringBeforeLast('.')

        if (rawPrefix.isNullOrBlank() || !isValidPrivatePrefix(rawPrefix)) {
            _scanState.value = LanScanProgress(
                isScanning = false,
                localIp = detectedIp,
                subnetPrefix = rawPrefix,
                errorEs = "No se detectó una subred Wi‑Fi privada activa (ej. 192.168.1). Conéctate al Wi‑Fi de tu casa o indica los 3 primeros bloques.",
                errorEn = "No active private Wi‑Fi subnet detected (e.g. 192.168.1). Connect to your home Wi‑Fi or enter the first 3 octets."
            )
            return@withContext
        }

        val found = Collections.synchronizedList(mutableListOf<LanScanDeviceHit>())
        _scanState.value = LanScanProgress(
            isScanning = true,
            localIp = detectedIp,
            subnetPrefix = rawPrefix,
            scannedHosts = 0,
            totalHosts = 254,
            discoveredHosts = emptyList()
        )

        val semaphore = Semaphore(maxConcurrency)
        var completed = 0
        val lock = Any()

        try {
            coroutineScope {
                val jobs = (1..254).map { hostOctet ->
                    async {
                        semaphore.withPermit {
                            ensureActive()
                            val candidateIp = "$rawPrefix.$hostOctet"
                            if (candidateIp != detectedIp) {
                                probeSingleHost(candidateIp)?.let { hit ->
                                    found.add(hit)
                                }
                            }
                            synchronized(lock) {
                                completed++
                                if (completed % 6 == 0 || completed == 254 || found.isNotEmpty()) {
                                    _scanState.value = _scanState.value.copy(
                                        scannedHosts = completed,
                                        discoveredHosts = found.sortedWith(
                                            compareByDescending<LanScanDeviceHit> { it.isAdbPortOpen }
                                                .thenBy { it.ipAddress.substringAfterLast('.').toIntOrNull() ?: 999 }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }
            _scanState.value = _scanState.value.copy(
                isScanning = false,
                scannedHosts = 254,
                discoveredHosts = found.sortedWith(
                    compareByDescending<LanScanDeviceHit> { it.isAdbPortOpen }
                        .thenBy { it.ipAddress.substringAfterLast('.').toIntOrNull() ?: 999 }
                )
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            _scanState.value = _scanState.value.copy(isScanning = false)
            throw cancelled
        } catch (e: Exception) {
            _scanState.value = _scanState.value.copy(
                isScanning = false,
                errorEs = "El escaneo de red finalizó con aviso: ${e.message ?: "Error de red local"}.",
                errorEn = "Network scan stopped: ${e.message ?: "Local network error"}."
            )
        }
    }

    override fun resetError() {
        _scanState.value = _scanState.value.copy(errorEs = null, errorEn = null)
    }

    private fun probeSingleHost(ip: String): LanScanDeviceHit? {
        val adbOpen = isTcpPortReachable(ip, 5555, connectTimeoutMs)
        val cast8008Open = isTcpPortReachable(ip, 8008, connectTimeoutMs)
        val cast8009Open = if (!cast8008Open) isTcpPortReachable(ip, 8009, connectTimeoutMs) else true

        if (!adbOpen && !cast8008Open && !cast8009Open) return null

        val eurekaMeta = if (cast8008Open) queryLocalEurekaInfo(ip) else null
        val reverseName = runCatching {
            val addr = Inet4Address.getByName(ip)
            val host = addr.canonicalHostName
            if (host != null && host != ip) host else null
        }.getOrNull()

        val friendly = eurekaMeta?.first
            ?: reverseName
            ?: if (adbOpen && (cast8008Open || cast8009Open)) "Android TV / Google TV ($ip)"
            else if (adbOpen) "Dispositivo ADB ($ip)"
            else "Smart TV / Google Cast ($ip)"

        return LanScanDeviceHit(
            ipAddress = ip,
            adbPort = 5555,
            isAdbPortOpen = adbOpen,
            isCastTvDetected = cast8008Open || cast8009Open,
            friendlyName = friendly,
            modelHint = eurekaMeta?.second
        )
    }

    private fun isTcpPortReachable(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun queryLocalEurekaInfo(ip: String): Pair<String?, String?>? {
        return runCatching {
            val conn = (URL("http://$ip:8008/setup/eureka_info?options=detail").openConnection() as HttpURLConnection).apply {
                connectTimeout = 450
                readTimeout = 450
                requestMethod = "GET"
                instanceFollowRedirects = false
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    val sb = StringBuilder()
                    val buf = CharArray(2048)
                    var read = reader.read(buf)
                    while (read > 0 && sb.length < 16_384) {
                        sb.append(buf, 0, read)
                        read = reader.read(buf)
                    }
                    sb.toString()
                }
                val json = JSONObject(text)
                val name = json.optString("name").takeIf { it.isNotBlank() }
                val detail = json.optJSONObject("detail")
                val model = detail?.optString("model_name")?.takeIf { it.isNotBlank() }
                if (name == null && model == null) null else Pair(name, model)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        fun isPrivateRfc1918(ip: String): Boolean {
            val parts = ip.split('.').map { it.toIntOrNull() ?: return false }
            if (parts.size != 4 || parts.any { it !in 0..255 }) return false
            return when {
                parts[0] == 10 -> true
                parts[0] == 192 && parts[1] == 168 -> true
                parts[0] == 172 && parts[1] in 16..31 -> true
                else -> false
            }
        }

        fun isValidPrivatePrefix(prefix: String): Boolean {
            val parts = prefix.split('.').map { it.toIntOrNull() ?: return false }
            if (parts.size != 3 || parts.any { it !in 0..255 }) return false
            return when {
                parts[0] == 10 -> true
                parts[0] == 192 && parts[1] == 168 -> true
                parts[0] == 172 && parts[1] in 16..31 -> true
                else -> false
            }
        }
    }
}
