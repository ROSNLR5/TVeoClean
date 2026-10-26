package com.example.core.adb

import com.example.core.engine.ProtectedPackageRegistry
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Pure Kotlin Android Debug Bridge (ADB) TCP v1.0 wire protocol implementation.
 */
object AdbWireProtocol {
    const val A_CNXN = 0x4e584e43
    const val A_AUTH = 0x48545541
    const val A_OPEN = 0x4e45504f
    const val A_OKAY = 0x59414b4f
    const val A_CLSE = 0x45534c43
    const val A_WRTE = 0x45545257

    const val A_VERSION = 0x01000000
    const val MAX_ADB_DATA = 256 * 1024

    const val AUTH_TYPE_TOKEN = 1
    const val AUTH_TYPE_SIGNATURE = 2
    const val AUTH_TYPE_RSAPUBLICKEY = 3

    data class Packet(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val payload: ByteArray
    )

    fun checksum(payload: ByteArray): Int {
        var sum = 0
        for (b in payload) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    fun writePacket(out: DataOutputStream, command: Int, arg0: Int, arg1: Int, payload: ByteArray = ByteArray(0)) {
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(command)
            .putInt(arg0)
            .putInt(arg1)
            .putInt(payload.size)
            .putInt(checksum(payload))
            .putInt(command xor -0x1)
            .array()
        synchronized(out) {
            out.write(header)
            if (payload.isNotEmpty()) {
                out.write(payload)
            }
            out.flush()
        }
    }

    fun readPacket(input: DataInputStream): Packet {
        val headerBytes = ByteArray(24)
        input.readFully(headerBytes)
        val header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val command = header.int
        val arg0 = header.int
        val arg1 = header.int
        val length = header.int
        val check = header.int
        val magic = header.int

        if (magic != (command xor -0x1)) {
            throw IOException("Cabecera ADB inválida (magic mismatch).")
        }
        if (length < 0 || length > MAX_ADB_DATA * 4) {
            throw IOException("Tamaño de carga ADB fuera de límites seguros ($length bytes).")
        }
        val payload = ByteArray(length)
        if (length > 0) {
            input.readFully(payload)
        }
        if (check != 0 && check != checksum(payload)) {
            throw IOException("Error de integridad en paquete ADB (checksum mismatch).")
        }
        return Packet(command, arg0, arg1, payload)
    }
}

sealed interface AdbSessionOutcome {
    data class Connected(
        val host: String,
        val port: Int,
        val banner: String
    ) : AdbSessionOutcome

    data class Failed(
        val errorCode: AdbErrorKind,
        val messageEs: String,
        val messageEn: String
    ) : AdbSessionOutcome
}

enum class AdbErrorKind {
    INVALID_LAN_HOST,
    TIMEOUT_OR_UNREACHABLE,
    AUTHORIZATION_PROMPT_TIMED_OUT,
    CONNECTION_REFUSED,
    TLS_PAIRING_REQUIRED,
    PROTOCOL_ERROR,
    DISCONNECTED
}

/**
 * Closed set of verified Android KeyEvent codes for TV Remote Control (never free-form shell).
 */
enum class TvRemoteKey(val keyCode: Int, val labelEs: String, val labelEn: String) {
    DPAD_UP(19, "Arriba", "Up"),
    DPAD_DOWN(20, "Abajo", "Down"),
    DPAD_LEFT(21, "Izquierda", "Left"),
    DPAD_RIGHT(22, "Derecha", "Right"),
    DPAD_CENTER(23, "OK", "OK"),
    BACK(4, "Atrás", "Back"),
    HOME(3, "Inicio", "Home"),
    MENU(82, "Menú", "Menu"),
    VOLUME_UP(24, "Vol +", "Vol +"),
    VOLUME_DOWN(25, "Vol -", "Vol -"),
    MUTE(164, "Silencio", "Mute"),
    PLAY_PAUSE(85, "Play/Pausa", "Play/Pause")
}

interface AdbTransportClient {
    val isConnected: Boolean
    val connectedEndpoint: String?
    val serverBanner: String?
    suspend fun connect(host: String, port: Int, connectTimeoutMs: Int = 6_000, authTimeoutMs: Int = 20_000): AdbSessionOutcome
    suspend fun executeReadOnlyShell(command: String, timeoutMs: Int = 8_000): Result<String>
    suspend fun setPackageEnabledState(
        packageName: String,
        enabled: Boolean,
        activeHomePackage: String? = null,
        timeoutMs: Int = 10_000
    ): Result<String>
    suspend fun sendRemoteKeyEvent(key: TvRemoteKey, timeoutMs: Int = 4_000): Result<Unit>
    suspend fun sendSafeSearchText(rawText: String, timeoutMs: Int = 5_000): Result<Unit>
    suspend fun captureScreenshotPng(timeoutMs: Int = 12_000): Result<ByteArray>
    suspend fun disconnect()
}

/**
 * Production TCP socket ADB client enforcing:
 * 1. Strict read-only allowlist on `executeReadOnlyShell`.
 * 2. Closed, parameterized, reversible operations (`pm disable-user --user 0` / `pm enable --user 0`)
 *    that unconditionally block `ProtectedPackageRegistry` and active HOME launchers, and NEVER execute
 *    `pm uninstall`, `rm`, or arbitrary user/repo shell commands.
 */
class SocketAdbTransportClient(
    private val keyManager: AdbKeyManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val allowLoopbackForTesting: Boolean = false
) : AdbTransportClient {

    companion object {
        private const val MAX_SCREENSHOT_BYTES = 12 * 1024 * 1024 // 12 MB cap
        private val VALID_PACKAGE_REGEX = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$""")
        private val SAFE_TEXT_INPUT_REGEX = Regex("""^[a-zA-Z0-9 .,_\-@:/?+=]{1,120}$""")

        private val READ_ONLY_COMMAND_PREFIXES = listOf(
            "getprop",
            "pm list packages",
            "cmd package resolve-activity",
            "dumpsys package",
            "settings get",
            "uname",
            "id"
        )

        fun isStrictlyReadOnlyCommand(cmd: String): Boolean {
            val normalized = cmd.trim()
            if (normalized.isEmpty()) return false
            if (normalized.contains(";") ||
                normalized.contains("&&") ||
                normalized.contains("||") ||
                normalized.contains("|") ||
                normalized.contains(">") ||
                normalized.contains("`") ||
                normalized.contains("$(")
            ) {
                return false
            }
            return READ_ONLY_COMMAND_PREFIXES.any { prefix ->
                normalized == prefix || normalized.startsWith("$prefix ")
            }
        }

        fun isValidAndroidPackageName(pkg: String): Boolean =
            pkg.length in 3..180 && VALID_PACKAGE_REGEX.matches(pkg)
    }

    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null
    private var nextLocalStreamId: Int = 10

    @Volatile
    override var connectedEndpoint: String? = null
        private set

    @Volatile
    override var serverBanner: String? = null
        private set

    override val isConnected: Boolean
        get() = socket?.let { it.isConnected && !it.isClosed } == true && connectedEndpoint != null

    override suspend fun connect(
        host: String,
        port: Int,
        connectTimeoutMs: Int,
        authTimeoutMs: Int
    ): AdbSessionOutcome = withContext(ioDispatcher) {
        disconnect()

        val trimmedHost = host.trim()
        val validTarget = if (allowLoopbackForTesting && (trimmedHost == "127.0.0.1" || trimmedHost == "localhost") && port in 1..65535) {
            LanHostValidator.ValidationResult.ValidLanTarget(normalizedHost = "127.0.0.1", port = port)
        } else {
            val lanCheck = LanHostValidator.validate(host, port.toString())
            if (lanCheck is LanHostValidator.ValidationResult.Rejected) {
                return@withContext AdbSessionOutcome.Failed(
                    errorCode = AdbErrorKind.INVALID_LAN_HOST,
                    messageEs = lanCheck.reasonEs,
                    messageEn = lanCheck.reasonEn
                )
            }
            lanCheck as LanHostValidator.ValidationResult.ValidLanTarget
        }

        val rawSocket = Socket()
        try {
            rawSocket.tcpNoDelay = true
            rawSocket.connect(InetSocketAddress(validTarget.normalizedHost, validTarget.port), connectTimeoutMs)
            rawSocket.soTimeout = authTimeoutMs

            val input = DataInputStream(rawSocket.getInputStream())
            val output = DataOutputStream(rawSocket.getOutputStream())

            val cnxnBanner = "host::features=shell_v2,cmd\u0000".toByteArray(Charsets.UTF_8)
            AdbWireProtocol.writePacket(
                out = output,
                command = AdbWireProtocol.A_CNXN,
                arg0 = AdbWireProtocol.A_VERSION,
                arg1 = AdbWireProtocol.MAX_ADB_DATA,
                payload = cnxnBanner
            )

            var packet = AdbWireProtocol.readPacket(input)
            var sentSignatureOnce = false

            while (packet.command == AdbWireProtocol.A_AUTH) {
                if (packet.arg0 != AdbWireProtocol.AUTH_TYPE_TOKEN) {
                    throw IOException("Tipo AUTH inesperado (${packet.arg0}).")
                }
                if (!sentSignatureOnce) {
                    sentSignatureOnce = true
                    val signature = keyManager.signAdbToken(packet.payload)
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_AUTH,
                        arg0 = AdbWireProtocol.AUTH_TYPE_SIGNATURE,
                        arg1 = 0,
                        payload = signature
                    )
                } else {
                    val pubKeyBytes = keyManager.adbPublicKeyPayloadBytes()
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_AUTH,
                        arg0 = AdbWireProtocol.AUTH_TYPE_RSAPUBLICKEY,
                        arg1 = 0,
                        payload = pubKeyBytes
                    )
                    packet = AdbWireProtocol.readPacket(input)
                    break
                }
                packet = AdbWireProtocol.readPacket(input)
            }

            if (packet.command != AdbWireProtocol.A_CNXN) {
                rawSocket.close()
                return@withContext AdbSessionOutcome.Failed(
                    errorCode = AdbErrorKind.PROTOCOL_ERROR,
                    messageEs = "El televisor rechazó la autorización ADB. Acepta el diálogo en la pantalla de tu TV y vuelve a intentarlo.",
                    messageEn = "The TV declined ADB authorization. Accept the prompt on your TV screen and try again."
                )
            }

            val bannerText = packet.payload.toString(Charsets.UTF_8).trimEnd('\u0000')
            socket = rawSocket
            inputStream = input
            outputStream = output
            connectedEndpoint = "${validTarget.normalizedHost}:${validTarget.port}"
            serverBanner = bannerText

            AdbSessionOutcome.Connected(
                host = validTarget.normalizedHost,
                port = validTarget.port,
                banner = bannerText
            )
        } catch (_: SocketTimeoutException) {
            runCatching { rawSocket.close() }
            AdbSessionOutcome.Failed(
                errorCode = AdbErrorKind.AUTHORIZATION_PROMPT_TIMED_OUT,
                messageEs = "Tiempo agotado esperando respuesta en $host:$port. Comprueba que tu TV esté encendida, en la misma red Wi‑Fi, con Depuración por red activa y acepta el aviso en pantalla.",
                messageEn = "Timed out waiting for $host:$port. Ensure your TV is on the same Wi‑Fi, Network Debugging is enabled, and accept the prompt on your TV."
            )
        } catch (_: EOFException) {
            runCatching { rawSocket.close() }
            AdbSessionOutcome.Failed(
                errorCode = AdbErrorKind.TLS_PAIRING_REQUIRED,
                messageEs = "El televisor cerró el canal TCP clásico. En Android TV 11+ con Depuración inalámbrica TLS, empareja primero con código de 6 dígitos o habilita ADB TCP (5555).",
                messageEn = "The TV closed the classic TCP channel. On Android TV 11+ with Wireless Debugging TLS, pair with a 6-digit code or enable TCP ADB (5555)."
            )
        } catch (e: IOException) {
            runCatching { rawSocket.close() }
            val msg = e.message.orEmpty()
            val refused = msg.contains("ECONNREFUSED", ignoreCase = true) || msg.contains("refused", ignoreCase = true)
            if (refused) {
                AdbSessionOutcome.Failed(
                    errorCode = AdbErrorKind.CONNECTION_REFUSED,
                    messageEs = "Conexión rechazada en $host:$port. Activa 'Opciones de desarrollador → Depuración por red / inalámbrica' en tu televisor.",
                    messageEn = "Connection refused on $host:$port. Enable 'Developer options → Network / Wireless debugging' on your TV."
                )
            } else {
                AdbSessionOutcome.Failed(
                    errorCode = AdbErrorKind.TIMEOUT_OR_UNREACHABLE,
                    messageEs = "No se pudo conectar con $host:$port en tu red local. No se realizó ningún cambio.",
                    messageEn = "Could not reach $host:$port on your local network. Nothing was modified."
                )
            }
        }
    }

    override suspend fun executeReadOnlyShell(command: String, timeoutMs: Int): Result<String> =
        withContext(ioDispatcher) {
            if (!isStrictlyReadOnlyCommand(command)) {
                return@withContext Result.failure(
                    SecurityException("Comando bloqueado por guardia de solo lectura: '$command'")
                )
            }
            executeValidatedShellBytes(command, timeoutMs, maxBytes = 1024 * 1024)
                .map { it.toString(Charsets.UTF_8) }
        }

    override suspend fun setPackageEnabledState(
        packageName: String,
        enabled: Boolean,
        activeHomePackage: String?,
        timeoutMs: Int
    ): Result<String> = withContext(ioDispatcher) {
        val cleanPkg = packageName.trim()
        if (!isValidAndroidPackageName(cleanPkg)) {
            return@withContext Result.failure(
                SecurityException("Nombre de paquete inválido rechazado: '$cleanPkg'")
            )
        }
        if (!enabled) {
            if (ProtectedPackageRegistry.isProtected(cleanPkg) ||
                (!activeHomePackage.isNullOrBlank() && cleanPkg == activeHomePackage)
            ) {
                return@withContext Result.failure(
                    SecurityException("Operación bloqueada: '$cleanPkg' es un paquete crítico protegido o el launcher HOME activo.")
                )
            }
        }
        val controlledCmd = if (enabled) {
            "pm enable --user 0 $cleanPkg"
        } else {
            "pm disable-user --user 0 $cleanPkg"
        }
        executeValidatedShellBytes(controlledCmd, timeoutMs, maxBytes = 64 * 1024)
            .map { it.toString(Charsets.UTF_8).trim() }
    }

    override suspend fun sendRemoteKeyEvent(key: TvRemoteKey, timeoutMs: Int): Result<Unit> =
        withContext(ioDispatcher) {
            executeValidatedShellBytes("input keyevent ${key.keyCode}", timeoutMs, maxBytes = 16 * 1024)
                .map { Unit }
        }

    override suspend fun sendSafeSearchText(rawText: String, timeoutMs: Int): Result<Unit> =
        withContext(ioDispatcher) {
            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) return@withContext Result.success(Unit)
            if (!SAFE_TEXT_INPUT_REGEX.matches(trimmed)) {
                return@withContext Result.failure(
                    SecurityException("Texto rechazado por contener caracteres especiales o de control no permitidos.")
                )
            }
            // In Android `input text`, spaces are encoded as %s
            val escaped = trimmed.replace(" ", "%s")
            executeValidatedShellBytes("input text $escaped", timeoutMs, maxBytes = 16 * 1024)
                .map { Unit }
        }

    override suspend fun captureScreenshotPng(timeoutMs: Int): Result<ByteArray> =
        withContext(ioDispatcher) {
            executeValidatedShellBytes("screencap -p", timeoutMs, maxBytes = MAX_SCREENSHOT_BYTES)
                .mapCatching { rawBytes ->
                    val pngBytes = normalizePngStream(rawBytes)
                    if (pngBytes.size < 8 ||
                        pngBytes[0] != 0x89.toByte() ||
                        pngBytes[1] != 0x50.toByte() ||
                        pngBytes[2] != 0x4E.toByte() ||
                        pngBytes[3] != 0x47.toByte()
                    ) {
                        throw IOException("El televisor no devolvió una imagen PNG válida (captura restringida por DRM o sistema).")
                    }
                    pngBytes
                }
        }

    private suspend fun executeValidatedShellBytes(
        validatedCommand: String,
        timeoutMs: Int,
        maxBytes: Int
    ): Result<ByteArray> {
        val activeSocket = socket
        val input = inputStream
        val output = outputStream
        if (activeSocket == null || input == null || output == null || !isConnected) {
            return Result.failure(IOException("No hay sesión ADB activa."))
        }

        return try {
            withTimeout(timeoutMs.toLong()) {
                synchronized(this@SocketAdbTransportClient) {
                    activeSocket.soTimeout = timeoutMs
                    val localId = nextLocalStreamId++
                    val openDestination = "shell:$validatedCommand\u0000".toByteArray(Charsets.UTF_8)
                    AdbWireProtocol.writePacket(
                        out = output,
                        command = AdbWireProtocol.A_OPEN,
                        arg0 = localId,
                        arg1 = 0,
                        payload = openDestination
                    )

                    var remoteId = 0
                    val byteBuffer = ByteArrayOutputStream()

                    while (true) {
                        val pkt = AdbWireProtocol.readPacket(input)
                        when (pkt.command) {
                            AdbWireProtocol.A_OKAY -> {
                                remoteId = pkt.arg0
                            }
                            AdbWireProtocol.A_WRTE -> {
                                remoteId = pkt.arg0
                                if (byteBuffer.size() + pkt.payload.size > maxBytes) {
                                    throw IOException("La respuesta ADB superó el límite seguro ($maxBytes bytes).")
                                }
                                byteBuffer.write(pkt.payload)
                                AdbWireProtocol.writePacket(
                                    out = output,
                                    command = AdbWireProtocol.A_OKAY,
                                    arg0 = localId,
                                    arg1 = remoteId
                                )
                            }
                            AdbWireProtocol.A_CLSE -> {
                                AdbWireProtocol.writePacket(
                                    out = output,
                                    command = AdbWireProtocol.A_CLSE,
                                    arg0 = localId,
                                    arg1 = remoteId
                                )
                                break
                            }
                            else -> throw IOException("Paquete inesperado durante operación ADB.")
                        }
                    }
                    Result.success(byteBuffer.toByteArray())
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Older classic `shell:screencap -p` PTY streams on some Android TVs translate `\n` to `\r\n`.
     * If PNG signature starts with `89 50 4E 47 0D 0D 0A 1A 0D 0A`, normalize CRLF back to LF.
     */
    private fun normalizePngStream(raw: ByteArray): ByteArray {
        if (raw.size >= 10 &&
            raw[0] == 0x89.toByte() &&
            raw[1] == 0x50.toByte() &&
            raw[2] == 0x4E.toByte() &&
            raw[3] == 0x47.toByte() &&
            raw[4] == 0x0D.toByte() &&
            raw[5] == 0x0D.toByte() &&
            raw[6] == 0x0A.toByte()
        ) {
            val out = ByteArrayOutputStream(raw.size)
            var i = 0
            while (i < raw.size) {
                if (i + 1 < raw.size && raw[i] == 0x0D.toByte() && raw[i + 1] == 0x0A.toByte()) {
                    out.write(0x0A)
                    i += 2
                } else {
                    out.write(raw[i].toInt())
                    i++
                }
            }
            return out.toByteArray()
        }
        return raw
    }

    override suspend fun disconnect() = withContext(ioDispatcher) {
        runCatching { socket?.close() }
        socket = null
        inputStream = null
        outputStream = null
        connectedEndpoint = null
        serverBanner = null
    }
}
