package com.example.core.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class DiscoveredAdbServiceKind(val serviceType: String, val requiresPairingCode: Boolean) {
    CLASSIC_ADB_TCP("_adb._tcp.", requiresPairingCode = false),
    TLS_CONNECT("_adb-tls-connect._tcp.", requiresPairingCode = false),
    TLS_PAIRING("_adb-tls-pairing._tcp.", requiresPairingCode = true)
}

data class DiscoveredLanTv(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
    val kind: DiscoveredAdbServiceKind
) {
    val endpointKey: String get() = "$hostAddress:$port"
}

interface LanTvDiscoveryScanner {
    fun observeDiscoveredDevices(): Flow<List<DiscoveredLanTv>>
}

/**
 * Real Android NsdManager mDNS scanner discovering `_adb._tcp.`, `_adb-tls-connect._tcp.`,
 * and `_adb-tls-pairing._tcp.` services on the home Wi-Fi network without requiring GPS/Location permissions.
 */
class AndroidNsdTvDiscoveryScanner(
    private val context: Context
) : LanTvDiscoveryScanner {

    override fun observeDiscoveredDevices(): Flow<List<DiscoveredLanTv>> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        if (nsdManager == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = runCatching {
            wifiManager?.createMulticastLock("tveoclean_mdns_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        }.getOrNull()

        val foundMap = LinkedHashMap<String, DiscoveredLanTv>()
        val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

        fun emitSnapshot() {
            synchronized(foundMap) {
                trySend(foundMap.values.toList())
            }
        }

        DiscoveredAdbServiceKind.entries.forEach { serviceKind ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                @Suppress("DEPRECATION")
                                val hostIp = resolved.host?.hostAddress ?: return
                                val port = resolved.port
                                val check = LanHostValidator.validate(hostIp, port.toString())
                                if (check is LanHostValidator.ValidationResult.ValidLanTarget) {
                                    val entry = DiscoveredLanTv(
                                        serviceName = resolved.serviceName ?: "Android TV",
                                        hostAddress = check.normalizedHost,
                                        port = check.port,
                                        kind = serviceKind
                                    )
                                    synchronized(foundMap) {
                                        foundMap[entry.endpointKey] = entry
                                    }
                                    emitSnapshot()
                                }
                            }
                        })
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val name = serviceInfo.serviceName ?: return
                    synchronized(foundMap) {
                        val toRemove = foundMap.values.filter { it.serviceName == name }.map { it.endpointKey }
                        toRemove.forEach { foundMap.remove(it) }
                    }
                    emitSnapshot()
                }
            }

            runCatching {
                nsdManager.discoverServices(serviceKind.serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                activeListeners.add(listener)
            }
        }

        emitSnapshot()

        awaitClose {
            activeListeners.forEach { listener ->
                runCatching { nsdManager.stopServiceDiscovery(listener) }
            }
            runCatching {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            }
        }
    }
}

class EmptyLanTvDiscoveryScanner : LanTvDiscoveryScanner {
    override fun observeDiscoveredDevices(): Flow<List<DiscoveredLanTv>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
}
