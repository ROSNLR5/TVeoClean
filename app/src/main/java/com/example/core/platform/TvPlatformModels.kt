package com.example.core.platform

import com.example.ui.i18n.AppLanguage

enum class OperationLifecycleState {
    IDLE,
    CONNECTING,
    CONNECTED,
    ANALYZING,
    PLAN_READY,
    APPLYING,
    VERIFYING,
    SUCCEEDED,
    PARTIALLY_SUCCEEDED,
    ROLLING_BACK,
    ROLLED_BACK,
    WAITING_FOR_DEVICE,
    UNSUPPORTED,
    FAILED
}

enum class TvPlatformFamily(
    val displayNameEs: String,
    val displayNameEn: String,
    val supportsAdb: Boolean
) {
    ANDROID_GOOGLE_TV("Android TV / Google TV", "Android TV / Google TV", supportsAdb = true),
    CHROMECAST_STREAMER("Chromecast / Google TV Streamer", "Chromecast / Google TV Streamer", supportsAdb = true),
    AMAZON_FIRE_OS("Amazon Fire TV / Fire OS", "Amazon Fire TV / Fire OS", supportsAdb = true),
    SAMSUNG_TIZEN("Samsung Tizen OS", "Samsung Tizen OS", supportsAdb = false),
    LG_WEBOS("LG webOS", "LG webOS", supportsAdb = false),
    ROKU_OS("Roku OS", "Roku OS", supportsAdb = false);

    fun localizedName(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) displayNameEs else displayNameEn
}

enum class CapabilitySupportLevel(val labelEs: String, val labelEn: String) {
    SUPPORTED("Soportado con ADB", "Supported via ADB"),
    CONSERVATIVE_LIMITED("Modo conservador", "Conservative mode"),
    BLOCKED_FOR_SAFETY("Bloqueado (Protección HOME)", "Blocked (HOME safety)"),
    UNAVAILABLE("No disponible", "Unavailable");

    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) labelEs else labelEn
}

data class PlatformCapabilityMatrix(
    val family: TvPlatformFamily,
    val readOnlyInspect: CapabilitySupportLevel,
    val reversibleAppDisable: CapabilitySupportLevel,
    val customLauncherSwitch: CapabilitySupportLevel,
    val styleStudioThemes: CapabilitySupportLevel,
    val snapshotRestore: CapabilitySupportLevel,
    val remoteUtilities: CapabilitySupportLevel,
    val honestNotesEs: String,
    val honestNotesEn: String
) {
    fun localizedNotes(lang: AppLanguage): String =
        if (lang == AppLanguage.ES) honestNotesEs else honestNotesEn
}

object HonestPlatformRegistry {
    val matrices: List<PlatformCapabilityMatrix> = listOf(
        PlatformCapabilityMatrix(
            family = TvPlatformFamily.ANDROID_GOOGLE_TV,
            readOnlyInspect = CapabilitySupportLevel.SUPPORTED,
            reversibleAppDisable = CapabilitySupportLevel.SUPPORTED,
            customLauncherSwitch = CapabilitySupportLevel.SUPPORTED,
            styleStudioThemes = CapabilitySupportLevel.SUPPORTED,
            snapshotRestore = CapabilitySupportLevel.SUPPORTED,
            remoteUtilities = CapabilitySupportLevel.SUPPORTED,
            honestNotesEs = "Plataforma principal de TVeoClean. Las opciones varían según fabricante (Sony, TCL, Hisense, Philips, Xiaomi) y firmware. Siempre exige confirmación y snapshot.",
            honestNotesEn = "Primary TVeoClean platform. Exact options vary by OEM (Sony, TCL, Hisense, Philips, Xiaomi) and firmware. Always requires confirmation and snapshot."
        ),
        PlatformCapabilityMatrix(
            family = TvPlatformFamily.CHROMECAST_STREAMER,
            readOnlyInspect = CapabilitySupportLevel.SUPPORTED,
            reversibleAppDisable = CapabilitySupportLevel.CONSERVATIVE_LIMITED,
            customLauncherSwitch = CapabilitySupportLevel.BLOCKED_FOR_SAFETY,
            styleStudioThemes = CapabilitySupportLevel.SUPPORTED,
            snapshotRestore = CapabilitySupportLevel.SUPPORTED,
            remoteUtilities = CapabilitySupportLevel.SUPPORTED,
            honestNotesEs = "Desactivar Google TV Home puede afectar Google Cast o la selección de perfiles; TVeoClean protege el Home original salvo recuperación verificada.",
            honestNotesEn = "Disabling Google TV Home can disrupt Google Cast or profile switching; TVeoClean guards stock Home unless safe recovery is verified."
        ),
        PlatformCapabilityMatrix(
            family = TvPlatformFamily.AMAZON_FIRE_OS,
            readOnlyInspect = CapabilitySupportLevel.CONSERVATIVE_LIMITED,
            reversibleAppDisable = CapabilitySupportLevel.CONSERVATIVE_LIMITED,
            customLauncherSwitch = CapabilitySupportLevel.BLOCKED_FOR_SAFETY,
            styleStudioThemes = CapabilitySupportLevel.CONSERVATIVE_LIMITED,
            snapshotRestore = CapabilitySupportLevel.SUPPORTED,
            remoteUtilities = CapabilitySupportLevel.CONSERVATIVE_LIMITED,
            honestNotesEs = "Fire OS bloquea el reemplazo del launcher y protege paquetes internos en versiones recientes. Solo se habilitan acciones seguras sin exploits.",
            honestNotesEn = "Fire OS blocks launcher replacement and guards internal packages on recent firmware. Only verified safe actions without exploits are enabled."
        ),
        PlatformCapabilityMatrix(
            family = TvPlatformFamily.SAMSUNG_TIZEN,
            readOnlyInspect = CapabilitySupportLevel.UNAVAILABLE,
            reversibleAppDisable = CapabilitySupportLevel.UNAVAILABLE,
            customLauncherSwitch = CapabilitySupportLevel.UNAVAILABLE,
            styleStudioThemes = CapabilitySupportLevel.UNAVAILABLE,
            snapshotRestore = CapabilitySupportLevel.UNAVAILABLE,
            remoteUtilities = CapabilitySupportLevel.UNAVAILABLE,
            honestNotesEs = "Samsung Tizen no utiliza Android ni soporta protocolo ADB. Se muestra ficha informativa honesta sin controles simulados.",
            honestNotesEn = "Samsung Tizen does not run Android or support ADB. Honest informational card only, with zero simulated controls."
        ),
        PlatformCapabilityMatrix(
            family = TvPlatformFamily.LG_WEBOS,
            readOnlyInspect = CapabilitySupportLevel.UNAVAILABLE,
            reversibleAppDisable = CapabilitySupportLevel.UNAVAILABLE,
            customLauncherSwitch = CapabilitySupportLevel.UNAVAILABLE,
            styleStudioThemes = CapabilitySupportLevel.UNAVAILABLE,
            snapshotRestore = CapabilitySupportLevel.UNAVAILABLE,
            remoteUtilities = CapabilitySupportLevel.UNAVAILABLE,
            honestNotesEs = "LG webOS no ejecuta Android ni admite depuración ADB. Todas las operaciones ADB permanecen marcadas como No disponibles.",
            honestNotesEn = "LG webOS does not run Android or support ADB debugging. All ADB operations remain marked as Unavailable."
        ),
        PlatformCapabilityMatrix(
            family = TvPlatformFamily.ROKU_OS,
            readOnlyInspect = CapabilitySupportLevel.UNAVAILABLE,
            reversibleAppDisable = CapabilitySupportLevel.UNAVAILABLE,
            customLauncherSwitch = CapabilitySupportLevel.UNAVAILABLE,
            styleStudioThemes = CapabilitySupportLevel.UNAVAILABLE,
            snapshotRestore = CapabilitySupportLevel.UNAVAILABLE,
            remoteUtilities = CapabilitySupportLevel.UNAVAILABLE,
            honestNotesEs = "Roku OS es un sistema propietario cerrado sin ADB Android. No se emulan limpiezas ni cambios de launcher.",
            honestNotesEn = "Roku OS is a proprietary closed OS without Android ADB. No cleanup or launcher operations are simulated."
        )
    )
}
