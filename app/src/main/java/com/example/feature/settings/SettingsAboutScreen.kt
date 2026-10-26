package com.example.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.TVeoCleanUiState
import com.example.ui.design.CalmCollapsibleCard
import com.example.ui.design.CalmSectionCard
import com.example.ui.design.ScreenHeaderBlock
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ThemeMode

private const val AUTHOR_GITHUB_URL = "https://github.com/ROSNLR5"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAboutScreen(
    uiState: TVeoCleanUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleReduceMotion: (Boolean) -> Unit,
    onToggleAdvancedAdb: (Boolean) -> Unit,
    onReopenOnboarding: () -> Unit,
    onCopiedNotice: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uriHandler = LocalUriHandler.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val secondaryAccent = MaterialTheme.colorScheme.secondary

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .testTag("settings_lazy_list")
        ) {
            item {
                ScreenHeaderBlock(
                    title = strings.settingsTitle,
                    subtitle = strings.settingsSubtitle
                )
            }

            // Architectural Signature Developer Card — Ingeniero de Sistemas Rosmer Santaella
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, primaryAccent.copy(alpha = 0.48f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_credits_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            primaryAccent.copy(alpha = 0.16f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.9f, size.height * 0.12f),
                                        radius = size.width * 0.55f
                                    ),
                                    radius = size.width * 0.55f,
                                    center = Offset(size.width * 0.9f, size.height * 0.12f)
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            secondaryAccent.copy(alpha = 0.11f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.1f, size.height * 0.9f),
                                        radius = size.width * 0.45f
                                    ),
                                    radius = size.width * 0.45f,
                                    center = Offset(size.width * 0.1f, size.height * 0.9f)
                                )
                            }
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Top signature badge + identity block
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Architectural double-ring monogram emblem
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    primaryAccent.copy(alpha = 0.24f),
                                                    secondaryAccent.copy(alpha = 0.20f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.4.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(primaryAccent, secondaryAccent)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Filled.Terminal,
                                            contentDescription = null,
                                            tint = primaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "RS",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = strings.settingsDeveloperCreatedBy,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = null,
                                            tint = primaryAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.settingsDeveloperName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.settingsDeveloperRole,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Crisp, minimalist engineering specialty chips (instead of heavy bio paragraphs)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                strings.settingsDeveloperDomainTags.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                        shape = CircleShape,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            // Action bar: Direct GitHub Profile CTA + 1-Tap Copy Link
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        runCatching { uriHandler.openUri(AUTHOR_GITHUB_URL) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("developer_github_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.settingsDeveloperGithubButton,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(AUTHOR_GITHUB_URL))
                                    onCopiedNotice(
                                        "GitHub copiado: $AUTHOR_GITHUB_URL",
                                        "GitHub copied: $AUTHOR_GITHUB_URL"
                                    )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("developer_copy_github_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = strings.settingsDeveloperCopyUrlButton,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language Selector (100% Spanish default + instant English toggle)
            item {
                CalmSectionCard(modifier = Modifier.testTag("language_selector_card")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsLanguageSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.settingsLanguageDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedChoiceButton(
                            label = strings.settingsLangSpanish,
                            selected = uiState.preferences.language == AppLanguage.ES,
                            onClick = { onSelectLanguage(AppLanguage.ES) },
                            testTag = "lang_option_es",
                            modifier = Modifier.weight(1f)
                        )
                        SegmentedChoiceButton(
                            label = strings.settingsLangEnglish,
                            selected = uiState.preferences.language == AppLanguage.EN,
                            onClick = { onSelectLanguage(AppLanguage.EN) },
                            testTag = "lang_option_en",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Theme, Accessibility & 3-Step Intro Guide Replay
            item {
                CalmSectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsAppearanceSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedChoiceButton(
                            label = strings.settingsThemeDark,
                            selected = uiState.preferences.themeMode == ThemeMode.DARK,
                            onClick = { onSelectThemeMode(ThemeMode.DARK) },
                            testTag = "theme_chip_dark",
                            modifier = Modifier.weight(1f)
                        )
                        SegmentedChoiceButton(
                            label = strings.settingsThemeLight,
                            selected = uiState.preferences.themeMode == ThemeMode.LIGHT,
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                            testTag = "theme_chip_light",
                            modifier = Modifier.weight(1f)
                        )
                        SegmentedChoiceButton(
                            label = strings.settingsThemeSystem,
                            selected = uiState.preferences.themeMode == ThemeMode.SYSTEM,
                            onClick = { onSelectThemeMode(ThemeMode.SYSTEM) },
                            testTag = "theme_chip_system",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    SettingSwitchRow(
                        title = strings.settingsReduceMotionTitle,
                        description = strings.settingsReduceMotionDesc,
                        checked = uiState.preferences.reduceMotion,
                        onCheckedChange = onToggleReduceMotion,
                        switchTag = "switch_reduce_motion"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    SettingSwitchRow(
                        title = strings.settingsAdvancedAdbTitle,
                        description = strings.settingsAdvancedAdbDesc,
                        checked = uiState.preferences.showAdvancedAdbDetails,
                        onCheckedChange = onToggleAdvancedAdb,
                        switchTag = "switch_advanced_adb"
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = onReopenOnboarding,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reopen_onboarding_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsReopenOnboarding,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Concise Privacy Card
            item {
                CalmSectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsPrivacySection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.settingsPrivacyStatement,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Collapsible Open Source Licenses & Honest Attributions (Keeps Settings clean)
            item {
                CalmCollapsibleCard(
                    title = strings.settingsLicensesSection,
                    subtitle = "TVeoClean · MIT · SIL OFL · Apache 2.0",
                    icon = Icons.Filled.Gavel,
                    headerTestTag = "licenses_collapsible_header",
                    modifier = Modifier.testTag("licenses_card")
                ) {
                    AttributionBlock(
                        project = strings.license1Project,
                        license = "https://github.com/ROSNLR5",
                        notice = strings.license1Notice
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                    AttributionBlock(
                        project = strings.license2Project,
                        license = "MIT License",
                        notice = strings.license2Notice
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                    AttributionBlock(
                        project = strings.license3Project,
                        license = "SIL Open Font License 1.1 / Apache-2.0",
                        notice = strings.license3Notice
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                    AttributionBlock(
                        project = strings.license4Project,
                        license = "Apache License 2.0",
                        notice = strings.license4Notice
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(switchTag)
        )
    }
}

@Composable
private fun AttributionBlock(
    project: String,
    license: String,
    notice: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = project,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = license,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = notice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
