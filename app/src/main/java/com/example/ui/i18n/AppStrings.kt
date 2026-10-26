package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String) {
    ES("es"),
    EN("en")
}

interface TVeoStrings {
    val appName: String
    val appTagline: String

    // Navigation
    val navHome: String
    val navMyTv: String
    val navStudio: String
    val navHistory: String
    val navSettings: String

    // Friendly 3-Step Intro Guide (Onboarding)
    val onboardingSkip: String
    val onboardingBack: String
    val onboardingNext: String
    val onboardingStart: String
    fun onboardingStepBadge(step: Int, total: Int): String
    val onboardingStep1Title: String
    val onboardingStep1Desc: String
    val onboardingStep1Bullets: List<String>
    val onboardingStep2Title: String
    val onboardingStep2Desc: String
    val onboardingStep2Bullets: List<String>
    val onboardingStep3Title: String
    val onboardingStep3Desc: String
    val onboardingStep3Bullets: List<String>

    // Status
    val statusDisconnected: String
    val statusConnecting: String
    val statusConnected: String
    val statusAnalyzing: String

    // Home (Clean & Uncluttered)
    val homeHeroReadyPillOffline: String
    val homeHeroReadyPillOnline: String
    val homeHeroDisconnectedTitle: String
    val homeHeroDisconnectedBody: String
    fun homeHeroConnectedTitle(brand: String, model: String): String
    fun homeHeroConnectedSubtitle(endpoint: String, androidVer: String): String
    val homeCtaConnectTv: String
    val homeCtaAnalyzeNow: String
    val homeQuickActionsTitle: String
    val homeActionAnalyze: String
    val homeActionAnalyzeSubOffline: String
    val homeActionAnalyzeSubOnline: String
    val homeActionStudio: String
    val homeActionStudioSub: String
    val homeActionRestore: String
    fun homeActionRestoreSub(count: Int): String
    val homeActionRemote: String
    val homeActionRemoteSubOffline: String
    val homeActionRemoteSubOnline: String
    val homeVerifiedStateTitle: String
    val homeVerifiedEmpty: String
    fun homeVerifiedReportSummary(total: Int, enabled: Int, disabled: Int): String
    fun homeActiveMobileThemeBanner(packName: String): String

    // My TV: Auto Network Scanner + Manual + Saved TVs + Clean Engine
    val myTvTitle: String
    val myTvSubtitle: String
    val myTvLanPrivatePill: String
    val myTvModeAutoTab: String
    val myTvModeManualTab: String
    val myTvAutoScanTitle: String
    val myTvAutoScanDesc: String
    val myTvStartNetworkScanButton: String
    val myTvStopNetworkScanButton: String
    fun myTvScanProgressLabel(subnet: String, scanned: Int, total: Int): String
    val myTvSubplotPrefixLabel: String
    val myTvBadgeAdbOpen: String
    val myTvBadgeCastOnly: String
    val myTvConnectOneTap: String
    val myTvSavedDevicesTitle: String
    val myTvDiscoveryTitle: String
    val myTvDiscoveryEmpty: String
    val myTvUseDeviceButton: String
    val myTvManualConnectTitle: String
    val myTvConnectNote: String
    val myTvHostLabel: String
    val myTvPortLabel: String
    val myTvConnectLiveButton: String
    val myTvDisconnectButton: String
    val myTvCancelConnectButton: String
    val myTvRunReadOnlyButton: String
    val myTvOpenRemoteToolsButton: String
    val myTvOpenPrivacyReportButton: String
    val myTvPairingGuideButton: String
    val myTvSecuritySectionTitle: String
    val myTvSecuritySectionSubtitle: String
    val myTvKeystoreTitle: String
    fun myTvKeystoreFingerprint(fp: String): String
    val myTvRevokeKeyButton: String
    val myTvProtectedGuardTitle: String
    val myTvProtectedGuardDesc: String
    val myTvPlatformMatrixTitle: String
    val myTvPlatformMatrixSubtitle: String
    val badgeAdbLocal: String
    val badgeNoAdb: String
    val capReadOnlyInspect: String
    val capReversibleDisable: String
    val capCustomLauncher: String
    val capStyleStudio: String
    val badgeProtected: String

    // Inspection Report & Transactional Reversible Clean Plan
    val reportSectionTitle: String
    val reportZeroChangesGuarantee: String
    fun reportDeviceHeader(brand: String, model: String, androidVer: String, sdk: Int): String
    fun reportActiveHomeLabel(homePkg: String): String
    val reportFilterAll: String
    val reportFilterRecommended: String
    val reportFilterProtected: String
    val reportFilterUnknown: String
    fun reportRestoreHint(cmd: String): String
    val reportSelectRecommendedButton: String
    val reportClearSelectionButton: String
    fun reportReviewPlanButton(count: Int): String
    val planDialogTitle: String
    val planDialogSafetyNotice: String
    val planConfirmExecuteButton: String

    // Remote Control & Verifiable Privacy Dialogs
    val remoteDialogTitle: String
    val remoteDisconnectedNotice: String
    val remoteSearchInputLabel: String
    val remoteSendTextButton: String
    val remoteScreenshotButton: String
    val remoteScreenshotSavedLabel: String
    val privacyDialogTitle: String
    val privacyDialogSubtitle: String
    val privacyRefreshButton: String
    val privacyCopyRedactedButton: String

    // Pairing & Guide Dialog
    val pairingDialogTitle: String
    val pairingDialogGuideText: String
    val pairingPortLabel: String
    val pairingPinLabel: String
    val pairingCheckButton: String
    val pairingLimitationNote: String

    // Style Studio & Live GitHub SHA Importer
    val studioTitle: String
    val studioSubtitle: String
    val studioImportGithub: String
    val studioSearchHint: String
    val studioTabDiscover: String
    val studioTabThemes: String
    val studioTabTweaks: String
    val studioTabProfiles: String
    val studioTabInstalled: String
    fun studioProposedCaption(name: String): String
    val studioCurrentCaption: String
    val studioMotionFluid: String
    val studioMotionStatic: String
    val studioDockAuto: String
    val studioDockFixed: String
    val studioCompareCurrent: String
    val studioCompareProposed: String
    val studioTargetMobileNotice: String
    val studioTargetTvNotice: String
    fun studioLegacyOmittedWarning(keys: String): String
    val studioVerifiedDiffsTitle: String
    val studioDeclaredPackagesTitle: String
    fun studioAuthorAndCommit(author: String, shortSha: String): String
    fun studioManifestSha(sha256: String): String
    val studioApplyMobileButton: String
    val studioApplyTvProfileButton: String
    val studioApplyTvThemeButton: String
    val studioConnectTvFirstButton: String
    val studioUnverifiedConsentLabel: String
    fun studioCatalogCountTitle(count: Int): String
    val studioCatalogSubtitle: String
    val studioCardSelectedBadge: String
    val studioCardTapPreviewBadge: String
    val studioEmptyFilterMessage: String

    // GitHub Import Dialog
    val githubDialogTitle: String
    val githubDialogDescription: String
    val githubUrlInputLabel: String
    fun githubUrlValidSummary(owner: String, repo: String, ref: String, path: String): String
    val githubVerifyButton: String
    val githubImportLiveButton: String
    val githubImportingProgress: String
    val dialogClose: String

    // History & Restore
    val historyTitle: String
    val historySubtitle: String
    fun historySnapshotsCountTitle(count: Int): String
    val historyEmptySnapshotsTitle: String
    val historyEmptySnapshotsBody: String
    fun historyPreviousStateLabel(summary: String): String
    fun historyIntegrityShaLabel(sha: String): String
    val historyRevertButton: String
    val historyCopyRecoveryScriptButton: String
    val historyDeleteSnapshotButton: String
    fun historyAuditLogCountTitle(count: Int): String
    fun historyTargetPrefix(target: String): String

    // Settings, Developer Credits, Language & Licenses
    val settingsTitle: String
    val settingsSubtitle: String
    val settingsDeveloperCreatedBy: String
    val settingsDeveloperName: String
    val settingsDeveloperRole: String
    val settingsDeveloperDomainTags: List<String>
    val settingsDeveloperGithubButton: String
    val settingsDeveloperCopyUrlButton: String
    val settingsAppearanceSection: String
    val settingsThemeLabel: String
    val settingsThemeDark: String
    val settingsThemeLight: String
    val settingsThemeSystem: String
    val settingsLanguageSection: String
    val settingsLanguageDesc: String
    val settingsLangSpanish: String
    val settingsLangEnglish: String
    val settingsReduceMotionTitle: String
    val settingsReduceMotionDesc: String
    val settingsAdvancedAdbTitle: String
    val settingsAdvancedAdbDesc: String
    val settingsReopenOnboarding: String
    val settingsPrivacySection: String
    val settingsPrivacyStatement: String
    val settingsLicensesSection: String
    val license1Project: String
    val license1Notice: String
    val license2Project: String
    val license2Notice: String
    val license3Project: String
    val license3Notice: String
    val license4Project: String
    val license4Notice: String
}

object SpanishTVeoStrings : TVeoStrings {
    override val appName = "TVeoClean"
    override val appTagline = "Centro de control seguro para tu televisor"

    override val navHome = "Inicio"
    override val navMyTv = "Mi TV"
    override val navStudio = "Studio"
    override val navHistory = "Historial"
    override val navSettings = "Ajustes"

    override val onboardingSkip = "Omitir"
    override val onboardingBack = "Anterior"
    override val onboardingNext = "Continuar"
    override val onboardingStart = "Empezar ahora"
    override fun onboardingStepBadge(step: Int, total: Int) = "Guía rápida · Paso $step de $total"
    override val onboardingStep1Title = "Conecta tu TV en segundos"
    override val onboardingStep1Desc = "En la pestaña Mi TV, busca automáticamente los televisores en tu Wi‑Fi o escribe su IP local."
    override val onboardingStep1Bullets = listOf(
        "Escáner Wi‑Fi automático o conexión directa por IP",
        "Solo acepta el aviso en la pantalla de tu TV con tu mando",
        "Conexión 100 % privada dentro de la red de tu casa"
    )
    override val onboardingStep2Title = "Limpia apps sin riesgo"
    override val onboardingStep2Desc = "Elige qué apps opcionales desactivar para ganar fluidez. Nada cambia sin tu confirmación."
    override val onboardingStep2Bullets = listOf(
        "Las funciones vitales del televisor nunca se tocan",
        "Desactivación reversible sin borrar datos del sistema",
        "Incluye Control Remoto en vivo y captura de pantalla"
    )
    override val onboardingStep3Title = "Personaliza y vuelve atrás cuando quieras"
    override val onboardingStep3Desc = "Explora temas visuales en Studio y restaura cualquier cambio con un toque desde Historial."
    override val onboardingStep3Bullets = listOf(
        "Mira a simple vista cómo quedará cada tema antes de aplicar",
        "Importa paquetes desde GitHub verificados de forma segura",
        "Punto de restauración automático antes de cada cambio"
    )

    override val statusDisconnected = "Sin vincular"
    override val statusConnecting = "Conectando…"
    override val statusConnected = "TV conectada"
    override val statusAnalyzing = "Analizando…"

    override val homeHeroReadyPillOffline = "Red Wi‑Fi local · Listo para conectar"
    override val homeHeroReadyPillOnline = "Conexión local verificada"
    override val homeHeroDisconnectedTitle = "Conecta tu televisor"
    override val homeHeroDisconnectedBody = "Descubre automáticamente tu Android TV o Google TV en tu red Wi‑Fi para limpiar apps innecesarias, usar el mando y personalizar su aspecto."
    override fun homeHeroConnectedTitle(brand: String, model: String) = "$brand $model"
    override fun homeHeroConnectedSubtitle(endpoint: String, androidVer: String) = "En línea ($endpoint) · Android $androidVer"
    override val homeCtaConnectTv = "Buscar mi TV en Wi‑Fi"
    override val homeCtaAnalyzeNow = "Administrar mi televisor"
    override val homeQuickActionsTitle = "Acceso directo"
    override val homeActionAnalyze = "Mi TV y Limpieza"
    override val homeActionAnalyzeSubOffline = "Escáner Wi‑Fi y conexión"
    override val homeActionAnalyzeSubOnline = "Apps y limpieza segura"
    override val homeActionStudio = "Style Studio"
    override val homeActionStudioSub = "Catálogo visual y GitHub"
    override val homeActionRestore = "Restaurar"
    override fun homeActionRestoreSub(count: Int) = if (count == 0) "Copias de seguridad" else "$count puntos guardados"
    override val homeActionRemote = "Control remoto"
    override val homeActionRemoteSubOffline = "Mando D‑Pad y captura"
    override val homeActionRemoteSubOnline = "Mando activo en vivo"
    override val homeVerifiedStateTitle = "Resumen de tu TV"
    override val homeVerifiedEmpty = "Todavía no has vinculado un televisor. Pulsa «Buscar mi TV en Wi‑Fi» para empezar."
    override fun homeVerifiedReportSummary(total: Int, enabled: Int, disabled: Int) =
        "$total aplicaciones detectadas · $enabled activas · $disabled desactivadas."
    override fun homeActiveMobileThemeBanner(packName: String) = "Estilo activo en teléfono: $packName"

    override val myTvTitle = "Mi TV"
    override val myTvSubtitle = "Conecta tu televisor por Wi‑Fi y gestiona sus aplicaciones con respaldo automático."
    override val myTvLanPrivatePill = "Conexión privada en tu Wi‑Fi local · Sin servidores externos"
    override val myTvModeAutoTab = "Escáner Wi‑Fi"
    override val myTvModeManualTab = "IP Manual"
    override val myTvAutoScanTitle = "Buscar televisores automáticamente"
    override val myTvAutoScanDesc = "Explora tu red Wi‑Fi doméstica para encontrar televisores Android TV y Google TV disponibles."
    override val myTvStartNetworkScanButton = "Escanear red Wi‑Fi"
    override val myTvStopNetworkScanButton = "Detener búsqueda"
    override fun myTvScanProgressLabel(subnet: String, scanned: Int, total: Int) =
        "Buscando en $subnet.1–254 · $scanned de $total comprobados"
    override val myTvSubplotPrefixLabel = "Subred local (ej. 192.168.1)"
    override val myTvBadgeAdbOpen = "Listo para conectar"
    override val myTvBadgeCastOnly = "TV detectada · Activa Depuración ADB"
    override val myTvConnectOneTap = "Conectar"
    override val myTvSavedDevicesTitle = "Televisores recientes"
    override val myTvDiscoveryTitle = "Dispositivos detectados en vivo"
    override val myTvDiscoveryEmpty = "Pulsa «Escanear red Wi‑Fi» arriba para localizar todos los televisores de tu red."
    override val myTvUseDeviceButton = "Elegir"
    override val myTvManualConnectTitle = "Conectar por dirección IP"
    override val myTvConnectNote = "La primera vez, acepta el aviso «Permitir siempre» en la pantalla del televisor con tu control remoto."
    override val myTvHostLabel = "Dirección IP de tu TV (ej. 192.168.1.50)"
    override val myTvPortLabel = "Puerto"
    override val myTvConnectLiveButton = "Conectar TV"
    override val myTvDisconnectButton = "Desconectar"
    override val myTvCancelConnectButton = "Cancelar"
    override val myTvRunReadOnlyButton = "Actualizar apps"
    override val myTvOpenRemoteToolsButton = "Control remoto"
    override val myTvOpenPrivacyReportButton = "Privacidad"
    override val myTvPairingGuideButton = "Ayuda ADB"
    override val myTvSecuritySectionTitle = "Seguridad de conexión y llave ADB"
    override val myTvSecuritySectionSubtitle = "Credencial cifrada en tu teléfono y protección del sistema"
    override val myTvKeystoreTitle = "Llave de autorización ADB (Android Keystore)"
    override fun myTvKeystoreFingerprint(fp: String) = "Huella pública: ${fp.take(29)}…"
    override val myTvRevokeKeyButton = "Renovar llave de seguridad"
    override val myTvProtectedGuardTitle = "Protección automática del sistema"
    override val myTvProtectedGuardDesc = "Wi‑Fi, mando Bluetooth, Play Store, entradas HDMI y la pantalla de inicio están siempre protegidos."
    override val myTvPlatformMatrixTitle = "Compatibilidad por marca y sistema"
    override val myTvPlatformMatrixSubtitle = "Ver qué funciones admite Android TV, Fire TV, Samsung, LG o Roku"
    override val badgeAdbLocal = "ADB Local"
    override val badgeNoAdb = "Sin ADB"
    override val capReadOnlyInspect = "Lectura de apps"
    override val capReversibleDisable = "Limpieza reversible"
    override val capCustomLauncher = "Cambio de inicio"
    override val capStyleStudio = "Temas Studio"
    override val badgeProtected = "Esencial"

    override val reportSectionTitle = "Aplicaciones del televisor"
    override val reportZeroChangesGuarantee = "Marca las apps que no uses. Se creará un punto de restauración automático antes de aplicar."
    override fun reportDeviceHeader(brand: String, model: String, androidVer: String, sdk: Int) =
        "$brand $model · Android $androidVer (SDK $sdk)"
    override fun reportActiveHomeLabel(homePkg: String) = "Pantalla de inicio activa: $homePkg"
    override val reportFilterAll = "Todas"
    override val reportFilterRecommended = "Sugeridas"
    override val reportFilterProtected = "Sistema"
    override val reportFilterUnknown = "Otras"
    override fun reportRestoreHint(cmd: String) = "Restauración: $cmd"
    override val reportSelectRecommendedButton = "Elegir sugeridas seguras"
    override val reportClearSelectionButton = "Limpiar"
    override fun reportReviewPlanButton(count: Int) = "Revisar cambios ($count)"
    override val planDialogTitle = "Confirmar cambios reversibles"
    override val planDialogSafetyNotice = "Se guardará una copia de respaldo automática antes de empezar para que puedas revertir en cualquier momento."
    override val planConfirmExecuteButton = "Guardar respaldo y aplicar"

    override val remoteDialogTitle = "Control Remoto y Captura"
    override val remoteDisconnectedNotice = "Conecta tu televisor en la pestaña «Mi TV» para usar los botones del mando y capturar pantalla."
    override val remoteSearchInputLabel = "Escribir texto en el buscador de la TV"
    override val remoteSendTextButton = "Enviar"
    override val remoteScreenshotButton = "Capturar pantalla de la TV"
    override val remoteScreenshotSavedLabel = "Captura guardada en tu teléfono:"
    override val privacyDialogTitle = "Informe de Privacidad"
    override val privacyDialogSubtitle = "Datos reales consultados en tu televisor frente a elementos no comprobables por red."
    override val privacyRefreshButton = "Actualizar"
    override val privacyCopyRedactedButton = "Copiar resumen"

    override val pairingDialogTitle = "Cómo activar la conexión en tu TV"
    override val pairingDialogGuideText = "1. En tu televisor ve a Ajustes → Sistema → Información.\n2. Pulsa 7 veces sobre «Compilación del SO Android TV» hasta activar Opciones de desarrollador.\n3. Entra en Opciones de desarrollador y activa «Depuración por red»."
    override val pairingPortLabel = "Puerto de código"
    override val pairingPinLabel = "PIN (6 dígitos)"
    override val pairingCheckButton = "Verificar PIN"
    override val pairingLimitationNote = "En televisores con Android 11+ que solo muestran código inalámbrico sin puerto 5555 activo, activa «Depuración por red / USB» en el menú de desarrollador."

    override val studioTitle = "Style Studio"
    override val studioSubtitle = "Mira cómo queda cada estilo a simple vista o importa paquetes seguros desde GitHub."
    override val studioImportGithub = "Importar GitHub"
    override val studioSearchHint = "Buscar tema, perfil o autor…"
    override val studioTabDiscover = "Todo"
    override val studioTabThemes = "Temas"
    override val studioTabTweaks = "Ajustes"
    override val studioTabProfiles = "Perfiles"
    override val studioTabInstalled = "Guardados"
    override fun studioProposedCaption(name: String) = "Vista previa: $name"
    override val studioCurrentCaption = "Diseño base actual"
    override val studioMotionFluid = "Animado"
    override val studioMotionStatic = "Sin animación"
    override val studioDockAuto = "Dock auto"
    override val studioDockFixed = "Dock fijo"
    override val studioCompareCurrent = "Actual"
    override val studioCompareProposed = "Así quedará"
    override val studioTargetMobileNotice = "Aplica solo a esta app en tu teléfono · No modifica el televisor."
    override val studioTargetTvNotice = "Diseñado para televisor · Crea respaldo automático antes de aplicar."
    override fun studioLegacyOmittedWarning(keys: String) = "Tema convertido: se ignoró clave no permitida ($keys)"
    override val studioVerifiedDiffsTitle = "Qué cambia este estilo"
    override val studioDeclaredPackagesTitle = "Apps incluidas en este perfil"
    override fun studioAuthorAndCommit(author: String, shortSha: String) = "Por $author · Versión fija $shortSha"
    override fun studioManifestSha(sha256: String) = "SHA‑256: $sha256"
    override val studioApplyMobileButton = "Aplicar estilo en mi teléfono"
    override val studioApplyTvProfileButton = "Cargar este perfil en Mi TV"
    override val studioApplyTvThemeButton = "Aplicar estilo en mi TV"
    override val studioConnectTvFirstButton = "Conectar mi TV para aplicar"
    override val studioUnverifiedConsentLabel = "He revisado la vista previa de este enlace externo y deseo continuar."
    override fun studioCatalogCountTitle(count: Int) = "Catálogo visual ($count)"
    override val studioCatalogSubtitle = "Toca cualquier tarjeta para inspeccionar detalles o comparar el antes y después."
    override val studioCardSelectedBadge = "En vista previa"
    override val studioCardTapPreviewBadge = "Tocar para probar"
    override val studioEmptyFilterMessage = "No encontramos estilos con esa búsqueda."

    override val githubDialogTitle = "Importar desde GitHub"
    override val githubDialogDescription = "Pega el enlace de un tema o perfil en GitHub. TVeoClean bloquea scripts ejecutables, fija la versión exacta y comprueba cada archivo."
    override val githubUrlInputLabel = "Enlace HTTPS de github.com"
    override fun githubUrlValidSummary(owner: String, repo: String, ref: String, path: String) =
        "Enlace válido: $owner/$repo ($ref · ${path.ifEmpty { "raíz" }}). Pulsa «Importar» para descargar y previsualizar."
    override val githubVerifyButton = "Comprobar"
    override val githubImportLiveButton = "Importar"
    override val githubImportingProgress = "Verificando archivos y fijando versión segura…"
    override val dialogClose = "Cerrar"

    override val historyTitle = "Historial"
    override val historySubtitle = "Tus copias de seguridad automáticas para volver al estado anterior con un solo toque."
    override fun historySnapshotsCountTitle(count: Int) = "Puntos de restauración ($count)"
    override val historyEmptySnapshotsTitle = "Aún no hay copias guardadas"
    override val historyEmptySnapshotsBody = "Cada vez que limpies apps o apliques un estilo, aquí aparecerá un punto de restauración automático."
    override fun historyPreviousStateLabel(summary: String) = "Respaldo: $summary"
    override fun historyIntegrityShaLabel(sha: String) = "SHA‑256: $sha"
    override val historyRevertButton = "Restaurar este estado"
    override val historyCopyRecoveryScriptButton = "Copiar comandos ADB"
    override val historyDeleteSnapshotButton = "Borrar"
    override fun historyAuditLogCountTitle(count: Int) = "Actividad reciente ($count)"
    override fun historyTargetPrefix(target: String) = "Destino: $target"

    override val settingsTitle = "Ajustes"
    override val settingsSubtitle = "Idioma, apariencia, guía de uso, privacidad y créditos del proyecto."
    override val settingsDeveloperCreatedBy = "Creado y desarrollado por"
    override val settingsDeveloperName = "Ingeniero de Sistemas Rosmer Santaella"
    override val settingsDeveloperRole = "Arquitectura de Software · Android Nativo & Seguridad ADB"
    override val settingsDeveloperDomainTags = listOf("Kotlin & Compose", "Protocolo ADB", "Seguridad por diseño")
    override val settingsDeveloperGithubButton = "github.com/ROSNLR5"
    override val settingsDeveloperCopyUrlButton = "Copiar GitHub"
    override val settingsAppearanceSection = "Apariencia y comodidad visual"
    override val settingsThemeLabel = "Tema de color"
    override val settingsThemeDark = "Oscuro"
    override val settingsThemeLight = "Claro"
    override val settingsThemeSystem = "Automático"
    override val settingsLanguageSection = "Idioma"
    override val settingsLanguageDesc = "Español predeterminado. Puedes cambiar a inglés en cualquier momento."
    override val settingsLangSpanish = "Español"
    override val settingsLangEnglish = "English"
    override val settingsReduceMotionTitle = "Reducir animaciones"
    override val settingsReduceMotionDesc = "Usa transiciones instantáneas y suaves para mayor comodidad visual."
    override val settingsAdvancedAdbTitle = "Modo técnico avanzado"
    override val settingsAdvancedAdbDesc = "Muestra identificadores internos de paquetes y huellas SHA‑256."
    override val settingsReopenOnboarding = "Ver guía inicial de 3 pasos"
    override val settingsPrivacySection = "Privacidad transparente"
    override val settingsPrivacyStatement = "Sin cuentas, sin anuncios ni rastreadores. Todo funciona directamente entre tu teléfono y tu televisor en tu red local."
    override val settingsLicensesSection = "Licencias y atribuciones"
    override val license1Project = "TVeoClean — Ing. de Sistemas Rosmer Santaella"
    override val license1Notice = "Diseño, arquitectura modular Android y motor reversible ADB original (https://github.com/ROSNLR5)."
    override val license2Project = "Inspiración funcional: Clean Your TV (MIT)"
    override val license2Notice = "Reconocimiento a Gathin23/cleanyourtv y udhaykumarbala/cleanyourtv-app por el flujo reversible y temas config.json."
    override val license3Project = "Tipografías Plus Jakarta Sans, Inter & JetBrains Mono"
    override val license3Notice = "Incluidas localmente bajo SIL Open Font License 1.1 y Apache-2.0."
    override val license4Project = "Android Jetpack, Material 3, Room, DataStore & OkHttp"
    override val license4Notice = "Copyright Google LLC y Square, Inc. Bajo Apache License 2.0."
}

object EnglishTVeoStrings : TVeoStrings {
    override val appName = "TVeoClean"
    override val appTagline = "Safe, effortless control center for your TV"

    override val navHome = "Home"
    override val navMyTv = "My TV"
    override val navStudio = "Studio"
    override val navHistory = "History"
    override val navSettings = "Settings"

    override val onboardingSkip = "Skip"
    override val onboardingBack = "Back"
    override val onboardingNext = "Continue"
    override val onboardingStart = "Get started"
    override fun onboardingStepBadge(step: Int, total: Int) = "Quick guide · Step $step of $total"
    override val onboardingStep1Title = "Connect your TV in seconds"
    override val onboardingStep1Desc = "In the My TV tab, automatically scan your home Wi‑Fi for TVs or enter a local IP address."
    override val onboardingStep1Bullets = listOf(
        "Automatic Wi‑Fi scanner or direct IP connection",
        "Simply approve the prompt on your TV screen with your remote",
        "100% private inside your home network"
    )
    override val onboardingStep2Title = "Clean up apps safely"
    override val onboardingStep2Desc = "Choose optional bloatware to disable for smoother performance. Nothing runs until you confirm."
    override val onboardingStep2Bullets = listOf(
        "Essential TV system apps are protected automatically",
        "Reversible disable without wiping system data",
        "Built-in live Remote Control and TV screenshot tool"
    )
    override val onboardingStep3Title = "Customize & restore anytime"
    override val onboardingStep3Desc = "Preview themes visually at a glance in Studio and undo any change in one tap from History."
    override val onboardingStep3Bullets = listOf(
        "See how every theme looks right from the catalog",
        "Safely import verified community packs from GitHub",
        "Automatic restore point saved before every single change"
    )

    override val statusDisconnected = "Not linked"
    override val statusConnecting = "Connecting…"
    override val statusConnected = "TV connected"
    override val statusAnalyzing = "Analyzing…"

    override val homeHeroReadyPillOffline = "Local Wi‑Fi · Ready to link"
    override val homeHeroReadyPillOnline = "Verified local connection"
    override val homeHeroDisconnectedTitle = "Connect your television"
    override val homeHeroDisconnectedBody = "Automatically discover your Android TV or Google TV on Wi‑Fi to clean up unwanted apps, use the live remote, and customize its look."
    override fun homeHeroConnectedTitle(brand: String, model: String) = "$brand $model"
    override fun homeHeroConnectedSubtitle(endpoint: String, androidVer: String) = "Online ($endpoint) · Android $androidVer"
    override val homeCtaConnectTv = "Find my TV on Wi‑Fi"
    override val homeCtaAnalyzeNow = "Manage my television"
    override val homeQuickActionsTitle = "Quick access"
    override val homeActionAnalyze = "My TV & Cleanup"
    override val homeActionAnalyzeSubOffline = "Wi‑Fi scanner & link"
    override val homeActionAnalyzeSubOnline = "Apps & safe cleanup"
    override val homeActionStudio = "Style Studio"
    override val homeActionStudioSub = "Visual catalog & GitHub"
    override val homeActionRestore = "Restore"
    override fun homeActionRestoreSub(count: Int) = if (count == 0) "Safety backups" else "$count saved points"
    override val homeActionRemote = "Remote control"
    override val homeActionRemoteSubOffline = "D‑Pad & screenshot"
    override val homeActionRemoteSubOnline = "Live remote ready"
    override val homeVerifiedStateTitle = "TV overview"
    override val homeVerifiedEmpty = "No television linked yet. Tap “Find my TV on Wi‑Fi” above to begin."
    override fun homeVerifiedReportSummary(total: Int, enabled: Int, disabled: Int) =
        "$total apps detected · $enabled enabled · $disabled disabled."
    override fun homeActiveMobileThemeBanner(packName: String) = "Active phone style: $packName"

    override val myTvTitle = "My TV"
    override val myTvSubtitle = "Connect your TV over Wi‑Fi and manage apps with automatic restore points."
    override val myTvLanPrivatePill = "Private home Wi‑Fi connection · Zero external servers"
    override val myTvModeAutoTab = "Wi‑Fi Scanner"
    override val myTvModeManualTab = "Manual IP"
    override val myTvAutoScanTitle = "Find televisions automatically"
    override val myTvAutoScanDesc = "Scans your home Wi‑Fi network to locate available Android TV and Google TV devices."
    override val myTvStartNetworkScanButton = "Scan Wi‑Fi network"
    override val myTvStopNetworkScanButton = "Stop scan"
    override fun myTvScanProgressLabel(subnet: String, scanned: Int, total: Int) =
        "Scanning $subnet.1–254 · $scanned of $total checked"
    override val myTvSubplotPrefixLabel = "Local subnet (e.g. 192.168.1)"
    override val myTvBadgeAdbOpen = "Ready to connect"
    override val myTvBadgeCastOnly = "TV found · Enable ADB Debugging"
    override val myTvConnectOneTap = "Connect"
    override val myTvSavedDevicesTitle = "Recent televisions"
    override val myTvDiscoveryTitle = "Live network announcements"
    override val myTvDiscoveryEmpty = "Tap “Scan Wi‑Fi network” above to discover televisions across your home Wi‑Fi."
    override val myTvUseDeviceButton = "Select"
    override val myTvManualConnectTitle = "Connect via IP address"
    override val myTvConnectNote = "The first time you connect, approve “Always allow” on your TV screen using your remote."
    override val myTvHostLabel = "TV IP address (e.g. 192.168.1.50)"
    override val myTvPortLabel = "Port"
    override val myTvConnectLiveButton = "Connect TV"
    override val myTvDisconnectButton = "Disconnect"
    override val myTvCancelConnectButton = "Cancel"
    override val myTvRunReadOnlyButton = "Refresh apps"
    override val myTvOpenRemoteToolsButton = "Remote control"
    override val myTvOpenPrivacyReportButton = "Privacy"
    override val myTvPairingGuideButton = "ADB Help"
    override val myTvSecuritySectionTitle = "Connection security & ADB key"
    override val myTvSecuritySectionSubtitle = "Hardware-backed phone credential & automatic system protection"
    override val myTvKeystoreTitle = "ADB Authorization Key (Android Keystore)"
    override fun myTvKeystoreFingerprint(fp: String) = "Public fingerprint: ${fp.take(29)}…"
    override val myTvRevokeKeyButton = "Rotate security key"
    override val myTvProtectedGuardTitle = "Automatic system protection"
    override val myTvProtectedGuardDesc = "Wi‑Fi, Bluetooth remotes, Play Store, HDMI inputs, and the active Home screen are always protected."
    override val myTvPlatformMatrixTitle = "Compatibility by TV brand & OS"
    override val myTvPlatformMatrixSubtitle = "Check supported features across Android TV, Fire TV, Samsung, LG, and Roku"
    override val badgeAdbLocal = "Local ADB"
    override val badgeNoAdb = "No ADB"
    override val capReadOnlyInspect = "App inspection"
    override val capReversibleDisable = "Reversible cleanup"
    override val capCustomLauncher = "Home switch"
    override val capStyleStudio = "Studio themes"
    override val badgeProtected = "Essential"

    override val reportSectionTitle = "Television applications"
    override val reportZeroChangesGuarantee = "Check unused optional apps below. An automatic restore point is saved before any change."
    override fun reportDeviceHeader(brand: String, model: String, androidVer: String, sdk: Int) =
        "$brand $model · Android $androidVer (SDK $sdk)"
    override fun reportActiveHomeLabel(homePkg: String) = "Active Home launcher: $homePkg"
    override val reportFilterAll = "All"
    override val reportFilterRecommended = "Suggested"
    override val reportFilterProtected = "System"
    override val reportFilterUnknown = "Other"
    override fun reportRestoreHint(cmd: String) = "Restore: $cmd"
    override val reportSelectRecommendedButton = "Select safe suggestions"
    override val reportClearSelectionButton = "Clear"
    override fun reportReviewPlanButton(count: Int) = "Review changes ($count)"
    override val planDialogTitle = "Confirm reversible changes"
    override val planDialogSafetyNotice = "An automatic backup snapshot will be saved first so you can revert anytime in one tap."
    override val planConfirmExecuteButton = "Save backup & apply"

    override val remoteDialogTitle = "Remote Control & Screenshot"
    override val remoteDisconnectedNotice = "Connect your television in the “My TV” tab to send live remote commands and capture screenshots."
    override val remoteSearchInputLabel = "Send search text to TV"
    override val remoteSendTextButton = "Send"
    override val remoteScreenshotButton = "Capture TV screenshot"
    override val remoteScreenshotSavedLabel = "Screenshot saved privately on your phone:"
    override val privacyDialogTitle = "Privacy Report"
    override val privacyDialogSubtitle = "Real facts read from your TV separated from hardware signals that cannot be verified remotely."
    override val privacyRefreshButton = "Refresh"
    override val privacyCopyRedactedButton = "Copy summary"

    override val pairingDialogTitle = "How to enable TV connection"
    override val pairingDialogGuideText = "1. On your TV open Settings → System → About.\n2. Press “Android TV OS build” 7 times to unlock Developer options.\n3. Open Developer options and turn on “Network debugging”."
    override val pairingPortLabel = "Pairing port"
    override val pairingPinLabel = "6-digit PIN"
    override val pairingCheckButton = "Check PIN"
    override val pairingLimitationNote = "On Android 11+ TVs showing wireless code pairing without port 5555 enabled, turn on “Network/USB debugging” in Developer options."

    override val studioTitle = "Style Studio"
    override val studioSubtitle = "Preview how every style looks at a glance or safely import packs from GitHub."
    override val studioImportGithub = "GitHub Import"
    override val studioSearchHint = "Search theme, profile, or creator…"
    override val studioTabDiscover = "All"
    override val studioTabThemes = "Themes"
    override val studioTabTweaks = "Tweaks"
    override val studioTabProfiles = "Profiles"
    override val studioTabInstalled = "Saved"
    override fun studioProposedCaption(name: String) = "Preview: $name"
    override val studioCurrentCaption = "Current baseline"
    override val studioMotionFluid = "Animated"
    override val studioMotionStatic = "Static"
    override val studioDockAuto = "Auto dock"
    override val studioDockFixed = "Fixed dock"
    override val studioCompareCurrent = "Current"
    override val studioCompareProposed = "How it looks"
    override val studioTargetMobileNotice = "Applies only to this phone app · Never modifies your TV."
    override val studioTargetTvNotice = "Designed for television · Saves an automatic backup before applying."
    override fun studioLegacyOmittedWarning(keys: String) = "Converted theme: ignored unsupported key ($keys)"
    override val studioVerifiedDiffsTitle = "What this style changes"
    override val studioDeclaredPackagesTitle = "Apps included in this profile"
    override fun studioAuthorAndCommit(author: String, shortSha: String) = "By $author · Pinned $shortSha"
    override fun studioManifestSha(sha256: String) = "SHA‑256: $sha256"
    override val studioApplyMobileButton = "Apply style on my phone"
    override val studioApplyTvProfileButton = "Load this profile in My TV"
    override val studioApplyTvThemeButton = "Apply style on my TV"
    override val studioConnectTvFirstButton = "Connect my TV to apply"
    override val studioUnverifiedConsentLabel = "I reviewed the preview for this external link and want to proceed."
    override fun studioCatalogCountTitle(count: Int) = "Visual catalog ($count)"
    override val studioCatalogSubtitle = "Tap any card to inspect details or compare before & after."
    override val studioCardSelectedBadge = "Previewing"
    override val studioCardTapPreviewBadge = "Tap to preview"
    override val studioEmptyFilterMessage = "No styles match your search."

    override val githubDialogTitle = "Import from GitHub"
    override val githubDialogDescription = "Paste a GitHub theme or profile URL. TVeoClean blocks executable scripts, pins the exact commit, and verifies every file."
    override val githubUrlInputLabel = "HTTPS github.com URL"
    override fun githubUrlValidSummary(owner: String, repo: String, ref: String, path: String) =
        "Valid link: $owner/$repo ($ref · ${path.ifEmpty { "root" }}). Tap “Import” to verify and preview."
    override val githubVerifyButton = "Check URL"
    override val githubImportLiveButton = "Import"
    override val githubImportingProgress = "Verifying files and pinning immutable version…"
    override val dialogClose = "Close"

    override val historyTitle = "History"
    override val historySubtitle = "Your automatic safety backups to restore any previous state in a single tap."
    override fun historySnapshotsCountTitle(count: Int) = "Restore points ($count)"
    override val historyEmptySnapshotsTitle = "No backups saved yet"
    override val historyEmptySnapshotsBody = "Whenever you clean up TV apps or apply a style, an automatic restore point will appear right here."
    override fun historyPreviousStateLabel(summary: String) = "Backup: $summary"
    override fun historyIntegrityShaLabel(sha: String) = "SHA‑256: $sha"
    override val historyRevertButton = "Restore this state"
    override val historyCopyRecoveryScriptButton = "Copy ADB script"
    override val historyDeleteSnapshotButton = "Delete"
    override fun historyAuditLogCountTitle(count: Int) = "Recent activity ($count)"
    override fun historyTargetPrefix(target: String) = "Target: $target"

    override val settingsTitle = "Settings"
    override val settingsSubtitle = "Language, visual theme, intro guide, privacy, and project authorship."
    override val settingsDeveloperCreatedBy = "Created & engineered by"
    override val settingsDeveloperName = "Systems Engineer Rosmer Santaella"
    override val settingsDeveloperRole = "Software Architecture · Native Android & ADB Security"
    override val settingsDeveloperDomainTags = listOf("Kotlin & Compose", "ADB Protocol", "Security by Design")
    override val settingsDeveloperGithubButton = "github.com/ROSNLR5"
    override val settingsDeveloperCopyUrlButton = "Copy GitHub"
    override val settingsAppearanceSection = "Appearance & visual comfort"
    override val settingsThemeLabel = "Color theme"
    override val settingsThemeDark = "Dark"
    override val settingsThemeLight = "Light"
    override val settingsThemeSystem = "Auto"
    override val settingsLanguageSection = "Language"
    override val settingsLanguageDesc = "Spanish is default. Switch to English anytime with one tap."
    override val settingsLangSpanish = "Español"
    override val settingsLangEnglish = "English"
    override val settingsReduceMotionTitle = "Reduce animations"
    override val settingsReduceMotionDesc = "Uses instant, calm transitions for maximum visual comfort."
    override val settingsAdvancedAdbTitle = "Advanced technical mode"
    override val settingsAdvancedAdbDesc = "Displays internal package identifiers and SHA‑256 hashes."
    override val settingsReopenOnboarding = "Replay 3-step intro guide"
    override val settingsPrivacySection = "Transparent privacy"
    override val settingsPrivacyStatement = "No accounts, no ads, and zero hidden trackers. Everything runs directly between your phone and your TV over local Wi‑Fi."
    override val settingsLicensesSection = "Open Source Licenses & Credits"
    override val license1Project = "TVeoClean — Systems Engineer Rosmer Santaella"
    override val license1Notice = "Original product design, modular Android architecture, and reversible ADB engine (https://github.com/ROSNLR5)."
    override val license2Project = "Functional inspiration: Clean Your TV (MIT)"
    override val license2Notice = "Acknowledges Gathin23/cleanyourtv and udhaykumarbala/cleanyourtv-app for reversible disable workflows and config.json themes."
    override val license3Project = "Plus Jakarta Sans, Inter & JetBrains Mono fonts"
    override val license3Notice = "Bundled locally under SIL Open Font License 1.1 and Apache-2.0."
    override val license4Project = "Android Jetpack, Material 3, Room, DataStore & OkHttp"
    override val license4Notice = "Copyright Google LLC and Square, Inc. Licensed under Apache License 2.0."
}

val LocalAppStrings = staticCompositionLocalOf<TVeoStrings> { SpanishTVeoStrings }

fun stringsForLanguage(language: AppLanguage): TVeoStrings = when (language) {
    AppLanguage.ES -> SpanishTVeoStrings
    AppLanguage.EN -> EnglishTVeoStrings
}
