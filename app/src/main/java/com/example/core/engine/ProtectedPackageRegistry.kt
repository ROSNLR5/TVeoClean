package com.example.core.engine

/**
 * Immutable, versioned registry of critical Android TV / Google TV system packages.
 * Third-party debloat profiles can NEVER remove, override, or weaken these rules.
 */
object ProtectedPackageRegistry {
    const val REGISTRY_VERSION = "2026.10.1"

    data class ProtectedEntry(
        val packageName: String,
        val categoryEs: String,
        val reasonEs: String
    )

    private val exactProtected: Map<String, ProtectedEntry> = listOf(
        ProtectedEntry("android", "Núcleo Android", "Proceso base del sistema operativo."),
        ProtectedEntry("com.android.systemui", "Interfaz del sistema", "Barras de sistema, diálogos de permisos y volumen."),
        ProtectedEntry("com.android.tv.settings", "Ajustes de TV", "Menú esencial para configurar Wi‑Fi, mando y depuración ADB."),
        ProtectedEntry("com.android.settings", "Ajustes base", " Proveedor crítico de configuración del dispositivo."),
        ProtectedEntry("com.android.packageinstaller", "Instalador de paquetes", "Necesario para gestionar permisos e instalaciones."),
        ProtectedEntry("com.google.android.packageinstaller", "Instalador de paquetes Google", "Gestión de permisos en tiempo de ejecución y paquetes."),
        ProtectedEntry("com.android.vending", "Google Play Store", "Tienda y verificación de actualizaciones críticas."),
        ProtectedEntry("com.google.android.gms", "Google Play Services", "Autenticación, Cast y servicios base de Android TV."),
        ProtectedEntry("com.google.android.gsf", "Google Services Framework", "Identidad y mensajería base del sistema."),
        ProtectedEntry("com.google.android.katniss", "Asistente / Voz del mando", "Búsqueda por voz e integración del micrófono del mando Bluetooth."),
        ProtectedEntry("com.google.android.apps.mediashell", "Google Cast Receiver", "Recepción de contenido enviado desde el móvil."),
        ProtectedEntry("com.google.android.tvlauncher", "Android TV Home", "Launcher principal: protegido hasta verificar reemplazo seguro con retorno."),
        ProtectedEntry("com.google.android.apps.tv.launcherx", "Google TV Home", "Launcher principal de Google TV: protegido contra pérdida de HOME."),
        ProtectedEntry("com.android.bluetooth", "Bluetooth y Mando", "Imprescindible para no desconectar el mando a distancia inalámbrico."),
        ProtectedEntry("com.android.providers.settings", "Almacén de configuración", "Base de datos interna de ajustes del sistema y ADB."),
        ProtectedEntry("com.google.android.marvin.talkback", "Accesibilidad TalkBack", "Lector de pantalla esencial de accesibilidad."),
        ProtectedEntry("com.android.tv.input", "Entradas HDMI / TV", "Conmutación de puertos HDMI y fuentes externas.")
    ).associateBy { it.packageName }

    fun inspectProtection(packageName: String): ProtectedEntry? {
        val normalized = packageName.trim()
        exactProtected[normalized]?.let { return it }
        if (normalized.startsWith("com.android.wifi") || normalized.contains(".networkstack")) {
            return ProtectedEntry(
                packageName = normalized,
                categoryEs = "Conectividad Wi‑Fi / Red",
                reasonEs = "Desactivar la pila de red cortaría inmediatamente la sesión ADB y el acceso Wi‑Fi."
            )
        }
        return null
    }

    fun isProtected(packageName: String): Boolean = inspectProtection(packageName) != null

    fun allEntries(): List<ProtectedEntry> = exactProtected.values.toList()
}
