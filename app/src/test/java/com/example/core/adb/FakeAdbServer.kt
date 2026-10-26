package com.example.core.adb

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Executors

/**
 * Real localhost TCP FakeAdbServer speaking Android's binary ADB v1.0 wire protocol
 * (`CNXN`, `AUTH`, `OPEN`, `OKAY`, `WRTE`, `CLSE`).
 *
 * Supports:
 * - Token signature auth, first-time RSA public key prompt acceptance, denial, timeout, TLS-only close
 * - Stateful `pm disable-user --user 0 <pkg>` and `pm enable --user 0 <pkg>` updating live enabled/disabled sets
 * - Binary PNG `screencap -p` payload, `input keyevent`, `input text`, and privacy `settings get` queries
 */
class FakeAdbServer(
    private val mode: ServerMode = ServerMode.ACCEPT_AFTER_PUBLIC_KEY,
    initialEnabledPackages: List<String> = DEFAULT_ENABLED_PACKAGES,
    initialDisabledPackages: List<String> = DEFAULT_DISABLED_PACKAGES,
    private val extraFixtures: Map<String, String> = emptyMap()
) : Closeable {

    enum class ServerMode {
        ACCEPT_SIGNATURE_IMMEDIATELY,
        ACCEPT_AFTER_PUBLIC_KEY,
        DENY_AUTHORIZATION,
        HANG_FOR_TIMEOUT,
        CLOSE_IMMEDIATELY_FOR_TLS_ONLY
    }

    companion object {
        val DEFAULT_ENABLED_PACKAGES = listOf(
            "com.google.android.apps.tv.launcherx",
            "com.android.tv.settings",
            "com.android.vending",
            "com.sony.dtv.promos",
            "com.google.android.tungsten.setupwraith",
            "com.vendor.unknown.customwidget"
        )
        val DEFAULT_DISABLED_PACKAGES = listOf(
            "com.android.tv.feedback"
        )

        // Minimal valid 1x1 PNG header + IHDR + IDAT + IEND (67 bytes)
        val MINIMAL_VALID_PNG_BYTES: ByteArray = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(), 0x89.toByte(),
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, 0x9C.toByte(), 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
            0x0D, 0x0A, 0x2D, 0xB4.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }

    private val enabledSet = Collections.synchronizedSet(LinkedHashSet(initialEnabledPackages))
    private val disabledSet = Collections.synchronizedSet(LinkedHashSet(initialDisabledPackages))

    private val serverSocket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val executor = Executors.newCachedThreadPool()

    val port: Int = serverSocket.localPort
    val executedShellCommands: MutableList<String> = Collections.synchronizedList(mutableListOf())
    @Volatile
    var receivedSignatureBytesCount: Int = 0
        private set
    @Volatile
    var receivedPublicKeyPayload: String? = null
        private set

    fun isPackageCurrentlyEnabled(pkg: String): Boolean = pkg in enabledSet

    fun start() {
        executor.submit {
            while (!serverSocket.isClosed) {
                val client = runCatching { serverSocket.accept() }.getOrNull() ?: break
                executor.submit { handleClient(client) }
            }
        }
    }

    private fun handleClient(client: Socket) {
        client.use { sock ->
            sock.tcpNoDelay = true
            val input = DataInputStream(sock.getInputStream())
            val output = DataOutputStream(sock.getOutputStream())

            if (mode == ServerMode.CLOSE_IMMEDIATELY_FOR_TLS_ONLY) {
                return
            }
            if (mode == ServerMode.HANG_FOR_TIMEOUT) {
                Thread.sleep(3_000)
                return
            }

            val cnxn = runCatching { AdbWireProtocol.readPacket(input) }.getOrNull() ?: return
            if (cnxn.command != AdbWireProtocol.A_CNXN) return

            val token = ByteArray(20) { idx -> (idx + 7).toByte() }
            AdbWireProtocol.writePacket(
                out = output,
                command = AdbWireProtocol.A_AUTH,
                arg0 = AdbWireProtocol.AUTH_TYPE_TOKEN,
                arg1 = 0,
                payload = token
            )

            val firstAuth = runCatching { AdbWireProtocol.readPacket(input) }.getOrNull() ?: return
            if (firstAuth.command != AdbWireProtocol.A_AUTH ||
                firstAuth.arg0 != AdbWireProtocol.AUTH_TYPE_SIGNATURE
            ) {
                return
            }
            receivedSignatureBytesCount = firstAuth.payload.size

            when (mode) {
                ServerMode.ACCEPT_SIGNATURE_IMMEDIATELY -> {
                    sendServerCnxn(output)
                }
                ServerMode.ACCEPT_AFTER_PUBLIC_KEY -> {
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_AUTH,
                        arg0 = AdbWireProtocol.AUTH_TYPE_TOKEN,
                        arg1 = 0,
                        payload = token
                    )
                    val pubKeyPkt = runCatching { AdbWireProtocol.readPacket(input) }.getOrNull() ?: return
                    if (pubKeyPkt.command != AdbWireProtocol.A_AUTH ||
                        pubKeyPkt.arg0 != AdbWireProtocol.AUTH_TYPE_RSAPUBLICKEY
                    ) {
                        return
                    }
                    receivedPublicKeyPayload = pubKeyPkt.payload.toString(Charsets.UTF_8).trimEnd('\u0000')
                    sendServerCnxn(output)
                }
                ServerMode.DENY_AUTHORIZATION -> {
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_AUTH,
                        arg0 = AdbWireProtocol.AUTH_TYPE_TOKEN,
                        arg1 = 0,
                        payload = token
                    )
                    runCatching { AdbWireProtocol.readPacket(input) }
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_CLSE,
                        arg0 = 0,
                        arg1 = 0
                    )
                    return
                }
                else -> return
            }

            var nextServerStreamId = 500
            while (!sock.isClosed) {
                val pkt = runCatching { AdbWireProtocol.readPacket(input) }.getOrNull() ?: break
                if (pkt.command == AdbWireProtocol.A_OPEN) {
                    val clientStreamId = pkt.arg0
                    val serverStreamId = nextServerStreamId++
                    val destination = pkt.payload.toString(Charsets.UTF_8).trimEnd('\u0000')
                    val shellCmd = destination.removePrefix("shell:")
                    executedShellCommands.add(shellCmd)

                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_OKAY,
                        arg0 = serverStreamId,
                        arg1 = clientStreamId
                    )

                    val body = computeShellResponseBytes(shellCmd)
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_WRTE,
                        arg0 = serverStreamId,
                        arg1 = clientStreamId,
                        payload = body
                    )

                    runCatching { AdbWireProtocol.readPacket(input) }

                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_CLSE,
                        arg0 = serverStreamId,
                        arg1 = clientStreamId
                    )
                    runCatching { AdbWireProtocol.readPacket(input) }
                }
            }
        }
    }

    private fun computeShellResponseBytes(shellCmd: String): ByteArray {
        extraFixtures[shellCmd]?.let { return it.toByteArray(Charsets.UTF_8) }

        if (shellCmd == "screencap -p") {
            return MINIMAL_VALID_PNG_BYTES
        }
        if (shellCmd.startsWith("pm disable-user --user 0 ")) {
            val pkg = shellCmd.removePrefix("pm disable-user --user 0 ").trim()
            enabledSet.remove(pkg)
            disabledSet.add(pkg)
            return "Package $pkg new state: disabled-user\n".toByteArray(Charsets.UTF_8)
        }
        if (shellCmd.startsWith("pm enable --user 0 ")) {
            val pkg = shellCmd.removePrefix("pm enable --user 0 ").trim()
            disabledSet.remove(pkg)
            enabledSet.add(pkg)
            return "Package $pkg new state: enabled\n".toByteArray(Charsets.UTF_8)
        }
        if (shellCmd.startsWith("input keyevent ") || shellCmd.startsWith("input text ")) {
            return ByteArray(0)
        }

        val responseText = when (shellCmd) {
            "getprop ro.product.manufacturer" -> "Sony\n"
            "getprop ro.product.model" -> "BRAVIA 4K VH22\n"
            "getprop ro.build.version.release" -> "12\n"
            "getprop ro.build.version.sdk" -> "31\n"
            "getprop ro.build.type" -> "user\n"
            "settings get global adb_enabled" -> "1\n"
            "settings get global package_verifier_enable" -> "1\n"
            "pm list packages -e" -> synchronized(enabledSet) {
                enabledSet.joinToString("\n") { "package:$it" } + "\n"
            }
            "pm list packages -d" -> synchronized(disabledSet) {
                disabledSet.joinToString("\n") { "package:$it" } + "\n"
            }
            "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" ->
                "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\ncom.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity\n"
            else -> ""
        }
        return responseText.toByteArray(Charsets.UTF_8)
    }

    private fun sendServerCnxn(output: DataOutputStream) {
        val banner = "device::ro.product.name=bravia_vh22;ro.product.model=BRAVIA 4K VH22;ro.product.device=vh22;\u0000"
            .toByteArray(Charsets.UTF_8)
        AdbWireProtocol.writePacket(
            out = output,
            command = AdbWireProtocol.A_CNXN,
            arg0 = AdbWireProtocol.A_VERSION,
            arg1 = AdbWireProtocol.MAX_ADB_DATA,
            payload = banner
        )
    }

    override fun close() {
        runCatching { serverSocket.close() }
        executor.shutdownNow()
    }
}
