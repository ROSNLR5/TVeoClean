package com.example.core.packs

object CuratedStudioCatalog {

    val baselineTvPreview = TvLauncherThemePreviewSpec(
        backgroundHex = "#0B1020",
        surfaceHex = "#121A2B",
        accentHex = "#55E6C1",
        glowHex = "#1E3A5F",
        columns = 4,
        dockApps = listOf("YouTube", "Netflix", "Prime", "HDMI 1"),
        fillDock = true,
        ambientMotionEnabled = true
    )

    val samplePacks: List<TveoPackSummary> = listOf(
        TveoPackSummary(
            id = "tveoclean.official.brisa_nocturna",
            nameEs = "Brisa Nocturna",
            nameEn = "Night Breeze",
            version = "1.2.0",
            descriptionEs = "Tema declarativo sereno en azul tinta con resplandor turquesa suave, 5 columnas equilibradas y dock limpio sin anuncios.",
            descriptionEn = "Calm ink-blue declarative theme with soft turquoise glow, balanced 5-column grid, and ad-free dock.",
            author = "Equipo TVeoClean",
            license = "MIT",
            creditsEs = "Diseño original de TVeoClean · Esquema tveo.json v1.0.",
            type = PackType.THEME,
            target = PackTarget.TV_LAUNCHER,
            trustTier = PackTrustTier.OFFICIAL,
            sourceRepo = "https://github.com/tveoclean/official-packs",
            pinnedCommitSha = "8f3d9a1c4b27e0619a52d8c341f07e62b91a4c08",
            sha256Manifest = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            compatibilityLabelEs = "Android TV / Google TV 10+",
            themePreview = TvLauncherThemePreviewSpec(
                backgroundHex = "#0D1528",
                surfaceHex = "#16233D",
                accentHex = "#55E6C1",
                glowHex = "#1A4B56",
                columns = 5,
                dockApps = listOf("Streaming", "Cine", "Música", "Archivos", "HDMI 2"),
                fillDock = true,
                ambientMotionEnabled = true
            ),
            diffsFromDefault = listOf(
                PackDiffEntry("Color de fondo", "Background color", "#0B1020", "#0D1528"),
                PackDiffEntry("Columnas", "Grid columns", "4 col", "5 col"),
                PackDiffEntry("Halo superior", "Top ambient glow", "#1E3A5F", "#1A4B56"),
                PackDiffEntry("Dock inferior", "Bottom dock", "4 accesos", "5 accesos")
            )
        ),
        TveoPackSummary(
            id = "tveoclean.official.indigo_calma_mobile",
            nameEs = "Índigo Sereno (Móvil)",
            nameEn = "Calm Indigo (Mobile)",
            version = "1.0.4",
            descriptionEs = "Personaliza únicamente la app móvil TVeoClean con acentos índigo suave y cian sereno. Incluye snapshot local y reversión.",
            descriptionEn = "Styles only the TVeoClean mobile app with soft indigo and calm cyan accents. Includes local snapshot & revert.",
            author = "Equipo TVeoClean",
            license = "MIT",
            creditsEs = "Paleta accesible WCAG AA verificada.",
            type = PackType.THEME,
            target = PackTarget.MOBILE_UI,
            trustTier = PackTrustTier.OFFICIAL,
            sourceRepo = "https://github.com/tveoclean/official-packs",
            pinnedCommitSha = "4a91bc72d08f13e65a2109c874e12b3f9801dc42",
            sha256Manifest = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            compatibilityLabelEs = "Solo teléfono · 0 cambios en TV",
            mobilePrimaryDarkHex = "#8FA0FF",
            mobilePrimaryLightHex = "#3949AB",
            mobileSecondaryDarkHex = "#55E6C1",
            mobileSecondaryLightHex = "#0B755E",
            diffsFromDefault = listOf(
                PackDiffEntry("Acento oscuro", "Dark accent", "#55E6C1", "#8FA0FF"),
                PackDiffEntry("Acento claro", "Light accent", "#0B755E", "#3949AB"),
                PackDiffEntry("Ámbito", "Target scope", "Teléfono", "Teléfono (0 ADB)")
            )
        ),
        TveoPackSummary(
            id = "community.legacy.aurora_seis_columnas",
            nameEs = "Aurora 6‑Columnas (Heredado)",
            nameEn = "Aurora 6‑Columns (Legacy)",
            version = "0.9.2",
            descriptionEs = "Muestra de conversión local segura desde config.json + wallpaper.jpg heredados sin modificar el repositorio remoto.",
            descriptionEn = "Safe local conversion sample from legacy config.json + wallpaper.jpg without touching the remote repo.",
            author = "Comunidad Open TV",
            license = "MIT + OFL",
            creditsEs = "Compatible con config.json (MIT).",
            type = PackType.THEME,
            target = PackTarget.TV_LAUNCHER,
            trustTier = PackTrustTier.UNVERIFIED_LINK,
            sourceRepo = "https://github.com/community-sample/tv-themes",
            pinnedCommitSha = "c27f18b904d3e1127a98f4bc0175d9e2361a8b19",
            sha256Manifest = "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
            compatibilityLabelEs = "Convertido a tveo.json v1.0",
            isLegacyConverted = true,
            omittedLegacyKeys = listOf("experimental_shell_hook"),
            themePreview = TvLauncherThemePreviewSpec(
                backgroundHex = "#101A30",
                surfaceHex = "#1A2947",
                accentHex = "#7CE8FF",
                glowHex = "#34286E",
                columns = 6,
                dockApps = listOf("TV", "Plex", "Cine", "Kodi", "HDMI", "Ajustes"),
                fillDock = false,
                ambientMotionEnabled = false
            ),
            diffsFromDefault = listOf(
                PackDiffEntry("Fondo", "Background", "#0B1020", "#101A30"),
                PackDiffEntry("Cuadrícula", "Grid layout", "4 col", "6 col"),
                PackDiffEntry("Dock auto", "Auto fill dock", "true", "false"),
                PackDiffEntry("Claves filtradas", "Filtered keys", "0", "1 omitida")
            )
        ),
        TveoPackSummary(
            id = "tveoclean.verified.perfil_conservador_gtv",
            nameEs = "Perfil Conservador Google TV",
            nameEn = "Conservative Google TV Profile",
            version = "2026.10",
            descriptionEs = "Recomendaciones declarativas de paquetes promocionales secundarios. Nunca ejecuta comandos shell ni altera la guardia protegida.",
            descriptionEn = "Declarative recommendations for secondary promo packages. Never runs shell scripts or weakens protected guards.",
            author = "Catálogo Curado",
            license = "Apache-2.0",
            creditsEs = "Lista protegida v2026.10.1 prevalece siempre.",
            type = PackType.DEBLOAT_PROFILE,
            target = PackTarget.TV_PROFILE,
            trustTier = PackTrustTier.VERIFIED,
            sourceRepo = "https://github.com/tveoclean/verified-profiles",
            pinnedCommitSha = "71e09d43a8c5f2109b6e4d2817c3a9f502e4b110",
            sha256Manifest = "d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            compatibilityLabelEs = "Google TV / Android TV 11–14",
            debloatItems = listOf(
                DebloatRecommendationItem(
                    packageName = "com.google.android.tungsten.setupwraith",
                    reasonEs = "Asistente secundario de sugerencias iniciales.",
                    reasonEn = "Secondary setup recommendation helper.",
                    riskLevelEs = "Bajo"
                ),
                DebloatRecommendationItem(
                    packageName = "com.android.tv.feedback",
                    reasonEs = "Envío opcional de reseñas del sistema.",
                    reasonEn = "Optional vendor feedback sender.",
                    riskLevelEs = "Bajo"
                ),
                DebloatRecommendationItem(
                    packageName = "com.google.android.tvlauncher",
                    reasonEs = "Prueba de blindaje: bloqueado siempre por guardia de HOME.",
                    reasonEn = "Safety guard proof: unconditionally blocked by HOME guard.",
                    riskLevelEs = "Protegido"
                )
            ),
            diffsFromDefault = listOf(
                PackDiffEntry("Paquetes evaluados", "Evaluated packages", "0", "3 (1 protegido)"),
                PackDiffEntry("Aplicación", "Execution mode", "Manual", "Plan confirmado")
            )
        ),
        TveoPackSummary(
            id = "tveoclean.verified.tweak_animacion_calma",
            nameEs = "Ajuste Calma Sin Parpadeos",
            nameEn = "Calm Flicker-Free Tweak",
            version = "1.1.0",
            descriptionEs = "Ajuste declarativo para televisores de 1–1.5 GB de RAM: desactiva ciclos de fondo animados y prioriza foco nítido.",
            descriptionEn = "Declarative tweak for 1–1.5 GB RAM TVs: turns off animated background cycles and sharpens tile focus.",
            author = "Catálogo Curado",
            license = "MIT",
            creditsEs = "Optimizado para paneles modestos.",
            type = PackType.TWEAK,
            target = PackTarget.TV_LAUNCHER,
            trustTier = PackTrustTier.VERIFIED,
            sourceRepo = "https://github.com/tveoclean/verified-profiles",
            pinnedCommitSha = "19b4e7c6a203f8d15e49c7b201a6f3d8e92c4051",
            sha256Manifest = "4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce",
            compatibilityLabelEs = "Android TV 9+",
            themePreview = TvLauncherThemePreviewSpec(
                backgroundHex = "#0B1020",
                surfaceHex = "#141E33",
                accentHex = "#55E6C1",
                glowHex = "#0B1020",
                columns = 4,
                dockApps = listOf("TV", "Stream", "Galería", "HDMI 1"),
                fillDock = true,
                ambientMotionEnabled = false
            ),
            diffsFromDefault = listOf(
                PackDiffEntry("Movimiento ambiental", "Ambient motion", "Activo", "Inactivo"),
                PackDiffEntry("Halo superior", "Top glow", "#1E3A5F", "#0B1020")
            )
        )
    )
}
