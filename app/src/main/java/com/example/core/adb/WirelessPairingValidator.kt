package com.example.core.adb

/**
 * Validator and transparent capability explainer for Android 11+ Wireless Debugging
 * pairing codes (`_adb-tls-pairing._tcp.`).
 *
 * Honesty note (Section 1, 7 & Fase 2):
 * Android 11+ Wireless Pairing uses SPAKE2+ password-authenticated key exchange over TLS 1.3
 * (BoringSSL custom extension `adb-label`), which is NOT available in standard Android `javax.net.ssl`
 * without native BoringSSL/conscrypt internals. TVeoClean validates pairing inputs cleanly,
 * connects directly over standard authorized ADB TCP/IP (`5555` or active `adb tcpip 5555` port),
 * and clearly documents how to enable standard Network Debugging on TVs that expose both modes,
 * marking SPAKE2+ native pairing as transparently documented rather than faking a pairing success.
 */
object WirelessPairingValidator {

    sealed interface PairingInputCheck {
        data class ValidFormat(
            val host: String,
            val pairingPort: Int,
            val sixDigitPin: String
        ) : PairingInputCheck

        data class Invalid(val messageEs: String, val messageEn: String) : PairingInputCheck
    }

    private val sixDigitRegex = Regex("""^\d{6}$""")

    fun validateInputs(rawHost: String, rawPairingPort: String, rawPin: String): PairingInputCheck {
        val hostCheck = LanHostValidator.validate(rawHost, rawPairingPort)
        if (hostCheck is LanHostValidator.ValidationResult.Rejected) {
            return PairingInputCheck.Invalid(hostCheck.reasonEs, hostCheck.reasonEn)
        }
        val validLan = hostCheck as LanHostValidator.ValidationResult.ValidLanTarget
        val pin = rawPin.trim()
        if (!sixDigitRegex.matches(pin)) {
            return PairingInputCheck.Invalid(
                messageEs = "El código de emparejamiento inalámbrico debe tener exactamente 6 dígitos numéricos.",
                messageEn = "Wireless pairing code must consist of exactly 6 numeric digits."
            )
        }
        return PairingInputCheck.ValidFormat(
            host = validLan.normalizedHost,
            pairingPort = validLan.port,
            sixDigitPin = pin
        )
    }
}
