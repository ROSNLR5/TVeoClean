package com.example.core.adb

import com.example.ui.i18n.AppLanguage

object LanHostValidator {

    sealed interface ValidationResult {
        data class ValidLanTarget(val normalizedHost: String, val port: Int) : ValidationResult
        data class Rejected(val reasonEs: String, val reasonEn: String = reasonEs) : ValidationResult {
            fun localizedReason(lang: AppLanguage): String =
                if (lang == AppLanguage.ES) reasonEs else reasonEn
        }
    }

    private val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
    private val mdnsHostRegex = Regex("""^[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]?\.local$""")

    fun validate(rawHost: String, rawPort: String): ValidationResult {
        val host = rawHost.trim().lowercase()
        if (host.isEmpty()) {
            return ValidationResult.Rejected(
                reasonEs = "Introduce la dirección IP local de tu televisor (por ejemplo, 192.168.1.50).",
                reasonEn = "Enter your TV's private local IP address (for example, 192.168.1.50)."
            )
        }
        if (host.contains("://") || host.contains("/") || host.contains("@")) {
            return ValidationResult.Rejected(
                reasonEs = "Introduce únicamente la IP local o nombre .local, sin http://, rutas ni credenciales.",
                reasonEn = "Enter only the local IP or .local hostname, without http://, paths, or credentials."
            )
        }

        val port = rawPort.trim().toIntOrNull()
            ?: return ValidationResult.Rejected(
                reasonEs = "El puerto ADB debe ser un número entero válido (habitualmente 5555).",
                reasonEn = "ADB port must be a valid integer (typically 5555)."
            )

        if (port !in 1024..65535) {
            return ValidationResult.Rejected(
                reasonEs = "El puerto ADB debe estar entre 1024 y 65535.",
                reasonEn = "ADB port must be between 1024 and 65535."
            )
        }

        val ipv4Match = ipv4Regex.matchEntire(host)
        if (ipv4Match != null) {
            val octets = ipv4Match.groupValues.drop(1).map { it.toIntOrNull() ?: -1 }
            if (octets.any { it !in 0..255 }) {
                return ValidationResult.Rejected(
                    reasonEs = "La dirección IPv4 contiene octetos fuera del rango 0–255.",
                    reasonEn = "IPv4 address contains octets outside the 0–255 range."
                )
            }
            val (a, b, _, _) = octets
            val isPrivateRfc1918 = (a == 10) ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) ||
                (a == 169 && b == 254)

            if (!isPrivateRfc1918) {
                return ValidationResult.Rejected(
                    reasonEs = "Por seguridad, TVeoClean bloquea conexiones ADB hacia IPs públicas ($host). Usa solo tu red doméstica privada (192.168.x.x, 10.x.x.x o 172.16–31.x.x).",
                    reasonEn = "For safety, TVeoClean blocks ADB connections to public IPs ($host). Use only your private home LAN (192.168.x.x, 10.x.x.x, or 172.16–31.x.x)."
                )
            }
            return ValidationResult.ValidLanTarget(host, port)
        }

        if (mdnsHostRegex.matches(host)) {
            return ValidationResult.ValidLanTarget(host, port)
        }

        return ValidationResult.Rejected(
            reasonEs = "Formato no reconocido. Introduce una IPv4 privada (ej. 192.168.1.50) o un nombre terminado en .local.",
            reasonEn = "Unrecognized host format. Enter a private IPv4 (e.g. 192.168.1.50) or a .local hostname."
        )
    }
}
