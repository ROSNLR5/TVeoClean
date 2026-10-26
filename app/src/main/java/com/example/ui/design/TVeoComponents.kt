package com.example.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.packs.PackTarget
import com.example.core.packs.PackTrustTier
import com.example.core.packs.TvLauncherThemePreviewSpec
import com.example.core.packs.TveoPackSummary
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalTVeoExtendedColors

fun parseHexColorSafe(hex: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex.trim()))
    }.getOrDefault(fallback)
}

@Composable
fun ScreenHeaderBlock(
    title: String,
    subtitle: String,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrustTierBadge(
    tier: PackTrustTier,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val extended = LocalTVeoExtendedColors.current
    val label = tier.localizedTitle(language)
    val (bgColor, fgColor, icon) = when (tier) {
        PackTrustTier.OFFICIAL -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.Shield
        )
        PackTrustTier.VERIFIED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.Verified
        )
        PackTrustTier.UNVERIFIED_LINK -> Triple(
            extended.warningContainer,
            extended.warning,
            Icons.Filled.WarningAmber
        )
    }

    Surface(
        color = bgColor,
        contentColor = fgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.35f)),
        modifier = modifier.semantics {
            contentDescription = label
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TargetScopeBadge(
    target: PackTarget,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val bg = if (target.affectsTv) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (target.affectsTv) Icons.Filled.Tv else Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = target.localizedLabel(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    icon: ImageVector = Icons.Filled.Info,
    isConnected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isConnected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isConnected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Proportionate 16:9 TV Frame Preview capped at a comfortable width (`460.dp`)
 * so it never stretches awkwardly on wide screens and tiles remain crisp and balanced.
 */
@Composable
fun TvFrame16x9Preview(
    spec: TvLauncherThemePreviewSpec,
    captionLabel: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val bgColor = parseHexColorSafe(spec.backgroundHex, Color(0xFF0B1020))
    val tileColor = parseHexColorSafe(spec.surfaceHex, Color(0xFF16233D))
    val accentColor = parseHexColorSafe(spec.accentHex, Color(0xFF55E6C1))
    val glowColor = parseHexColorSafe(spec.glowHex, Color(0xFF1E3A5F))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tv_frame_preview")
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.5.dp,
                    color = Color(0xFF2B3B5E),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(glowColor, bgColor, bgColor)
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = captionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFF3F6FC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val motionText = if (spec.ambientMotionEnabled) strings.studioMotionFluid else strings.studioMotionStatic
                    Text(
                        text = "${spec.columns} col · $motionText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC5D1E6),
                        maxLines = 1
                    )
                }

                // Balanced 16:9 TV app tiles inside the screen
                val colCount = spec.columns.coerceIn(3, 6)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(colCount) { idx ->
                        val isFocused = idx == 0
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.45f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(tileColor)
                                .border(
                                    width = if (isFocused) 1.8.dp else 1.dp,
                                    color = if (isFocused) accentColor else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = spec.dockApps.getOrElse(idx) { "App ${idx + 1}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF3F6FC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom clean dock
                Surface(
                    color = tileColor.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        val dockCount = spec.dockApps.size.coerceIn(3, 6)
                        repeat(dockCount) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == 0) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx == 0) accentColor else Color(0xFF9FB0CC).copy(alpha = 0.6f)
                                    )
                            )
                        }
                        Text(
                            text = if (spec.fillDock) strings.studioDockAuto else strings.studioDockFixed,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD8E2F3),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(MaterialTheme.colorScheme.outline)
        )
    }
}

/**
 * Rich, immediate visual thumbnail rendered inside every Style Studio catalog card
 * so users can see at a glance how a TV theme, phone UI theme, or cleanup profile looks.
 */
@Composable
fun PackVisualCatalogThumbnail(
    pack: TveoPackSummary,
    language: AppLanguage,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val preview = pack.themePreview

    when {
        preview != null -> {
            val bgColor = parseHexColorSafe(preview.backgroundHex, Color(0xFF0B1020))
            val tileColor = parseHexColorSafe(preview.surfaceHex, Color(0xFF16233D))
            val accentColor = parseHexColorSafe(preview.accentHex, Color(0xFF55E6C1))
            val glowColor = parseHexColorSafe(preview.glowHex, Color(0xFF1E3A5F))
            val colCount = preview.columns.coerceIn(3, 6)

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(glowColor, bgColor, bgColor)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accentColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top mini TV bar: palette swatches + layout tag
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorSwatchRow(
                            colors = listOf(accentColor, glowColor, tileColor, bgColor)
                        )
                        Surface(
                            color = Color(0x99080E1C),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${preview.columns} col · ${if (preview.ambientMotionEnabled) strings.studioMotionFluid else strings.studioMotionStatic}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF3F6FC),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Mini TV launcher app tiles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(colCount) { idx ->
                            val focused = idx == 0
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tileColor)
                                    .border(
                                        width = if (focused) 1.6.dp else 1.dp,
                                        color = if (focused) accentColor else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 3.dp)
                            ) {
                                Text(
                                    text = preview.dockApps.getOrElse(idx) { "App" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3F6FC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Mini dock indicator
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(preview.dockApps.size.coerceIn(3, 6)) { idx ->
                                Box(
                                    modifier = Modifier
                                        .height(5.dp)
                                        .width(if (idx == 0) 16.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == 0) accentColor else Color(0x88C5D1E6))
                                )
                            }
                        }
                        Text(
                            text = if (isSelected) strings.studioCardSelectedBadge else strings.studioCardTapPreviewBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) accentColor else Color(0xFFD0DCF0)
                        )
                    }
                }
            }
        }

        pack.target == PackTarget.MOBILE_UI -> {
            val darkPrimary = parseHexColorSafe(pack.mobilePrimaryDarkHex ?: "#8FA0FF", Color(0xFF8FA0FF))
            val lightPrimary = parseHexColorSafe(pack.mobilePrimaryLightHex ?: "#3949AB", Color(0xFF3949AB))
            val darkSecondary = parseHexColorSafe(pack.mobileSecondaryDarkHex ?: "#55E6C1", Color(0xFF55E6C1))

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(122.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0E1528), Color(0xFF1C2744))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = darkPrimary.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Mini Dark Phone Preview
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B1020))
                            .border(1.dp, darkPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(9.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.settingsThemeDark,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF3F6FC)
                            )
                            ColorSwatchRow(colors = listOf(darkPrimary, darkSecondary))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(darkPrimary)
                        )
                    }

                    // Mini Light Phone Preview
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF7F8FC))
                            .border(1.dp, lightPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(9.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.settingsThemeLight,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF182033)
                            )
                            ColorSwatchRow(colors = listOf(lightPrimary, darkSecondary))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(lightPrimary)
                        )
                    }
                }
            }
        }

        else -> {
            // Visual card for Cleanup / Debloat Profiles
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0E1B2A), Color(0xFF132C38))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF55E6C1).copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = Color(0xFF55E6C1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pack.type.localizedLabel(language),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF3F6FC)
                            )
                        }
                        Text(
                            text = "${pack.debloatItems.size} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF55E6C1)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pack.debloatItems.take(3).forEach { rec ->
                            val shortPkg = rec.packageName.substringAfterLast('.')
                            Surface(
                                color = Color(0xAA0B1020),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = shortPkg,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD8E2F3),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSwatchRow(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        colors.forEach { swatch ->
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            )
        }
    }
}

@Composable
fun CalmSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}

/**
 * Clean collapsible section card to keep secondary reference details accessible
 * without cluttering primary functional screens.
 */
@Composable
fun CalmCollapsibleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = false,
    headerTestTag: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val reduceMotion = LocalTVeoExtendedColors.current.reduceMotion

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .then(if (headerTestTag != null) Modifier.testTag(headerTestTag) else Modifier)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(),
                exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    content()
                }
            }
        }
    }
}

@Composable
fun ProtectedBadgePill(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = strings.badgeProtected,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
