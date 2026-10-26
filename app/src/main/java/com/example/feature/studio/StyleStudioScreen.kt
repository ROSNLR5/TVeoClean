package com.example.feature.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.engine.ProtectedPackageRegistry
import com.example.core.packs.CuratedStudioCatalog
import com.example.core.packs.PackTarget
import com.example.core.packs.TveoPackSummary
import com.example.ui.GitHubImportCheckState
import com.example.ui.StudioTab
import com.example.ui.TVeoCleanUiState
import com.example.ui.design.CalmSectionCard
import com.example.ui.design.PackVisualCatalogThumbnail
import com.example.ui.design.ProtectedBadgePill
import com.example.ui.design.ScreenHeaderBlock
import com.example.ui.design.TargetScopeBadge
import com.example.ui.design.TrustTierBadge
import com.example.ui.design.TvFrame16x9Preview
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalTVeoExtendedColors
import kotlinx.coroutines.launch

@Composable
fun StyleStudioScreen(
    uiState: TVeoCleanUiState,
    onSearchChange: (String) -> Unit,
    onSelectTab: (StudioTab) -> Unit,
    onSelectPack: (String) -> Unit,
    onToggleUnverifiedConsent: (Boolean) -> Unit,
    onToggleCompareProposed: (Boolean) -> Unit,
    onApplyPack: (TveoPackSummary) -> Unit,
    onOpenGitHubImport: () -> Unit,
    onCloseGitHubImport: () -> Unit,
    onUpdateGitHubUrl: (String) -> Unit,
    onVerifyGitHubUrl: () -> Unit,
    onImportFromGitHubLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = uiState.preferences.language
    val selectedPack = uiState.selectedPack
    val extendedColors = LocalTVeoExtendedColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_studio")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .testTag("studio_lazy_list")
        ) {
            // Clean Header + GitHub Import Button
            item {
                ScreenHeaderBlock(
                    title = strings.studioTitle,
                    subtitle = strings.studioSubtitle,
                    trailingContent = {
                        OutlinedButton(
                            onClick = onOpenGitHubImport,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("studio_import_github_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.studioImportGithub,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                )
            }

            // Search bar + FilterChips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.studioSearchQuery,
                        onValueChange = onSearchChange,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = {
                            Text(
                                text = strings.studioSearchHint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_search_input")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StudioTab.entries) { tab ->
                            FilterChip(
                                selected = uiState.selectedStudioTab == tab,
                                onClick = { onSelectTab(tab) },
                                label = {
                                    Text(
                                        text = tab.localizedLabel(strings),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("studio_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Live 16:9 TV Frame Preview & Apply Card (Focused, uncluttered)
            item {
                CalmSectionCard(modifier = Modifier.testTag("studio_selected_inspector")) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPack.localizedName(lang),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TargetScopeBadge(target = selectedPack.target, language = lang)
                                Text(
                                    text = "v${selectedPack.version}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TrustTierBadge(tier = selectedPack.trustTier, language = lang)
                    }

                    val previewSpec = if (uiState.compareProposedInPreview) {
                        selectedPack.themePreview ?: CuratedStudioCatalog.baselineTvPreview
                    } else {
                        CuratedStudioCatalog.baselineTvPreview
                    }
                    TvFrame16x9Preview(
                        spec = previewSpec,
                        captionLabel = if (uiState.compareProposedInPreview) {
                            strings.studioProposedCaption(selectedPack.localizedName(lang))
                        } else {
                            strings.studioCurrentCaption
                        }
                    )

                    // Before / After Comparison Toggle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!uiState.compareProposedInPreview) {
                            Button(
                                onClick = { onToggleCompareProposed(false) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("studio_compare_current_button")
                            ) {
                                Text(strings.studioCompareCurrent, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onToggleCompareProposed(true) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("studio_compare_proposed_button")
                            ) {
                                Text(strings.studioCompareProposed, maxLines = 1)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onToggleCompareProposed(false) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("studio_compare_current_button")
                            ) {
                                Text(strings.studioCompareCurrent, maxLines = 1)
                            }
                            Button(
                                onClick = { onToggleCompareProposed(true) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("studio_compare_proposed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CompareArrows,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.studioCompareProposed, maxLines = 1)
                            }
                        }
                    }

                    Text(
                        text = selectedPack.localizedDescription(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedPack.target == PackTarget.MOBILE_UI) {
                                    Icons.Filled.PhoneAndroid
                                } else {
                                    Icons.Filled.Tv
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedPack.target == PackTarget.MOBILE_UI) {
                                    strings.studioTargetMobileNotice
                                } else {
                                    strings.studioTargetTvNotice
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (selectedPack.isLegacyConverted && selectedPack.omittedLegacyKeys.isNotEmpty()) {
                        Surface(
                            color = extendedColors.warningContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = extendedColors.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.studioLegacyOmittedWarning(selectedPack.omittedLegacyKeys.joinToString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Compact visual diff chips
                    if (selectedPack.diffsFromDefault.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(
                            text = strings.studioVerifiedDiffsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            selectedPack.diffsFromDefault.forEach { diff ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = diff.localizedLabel(lang),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${diff.currentValue} → ${diff.proposedValue}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (selectedPack.debloatItems.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(
                            text = strings.studioDeclaredPackagesTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedPack.debloatItems.forEach { rec ->
                                val isProtected = ProtectedPackageRegistry.isProtected(rec.packageName)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rec.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = rec.localizedReason(lang),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isProtected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            ProtectedBadgePill()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = strings.studioAuthorAndCommit(
                            selectedPack.author,
                            selectedPack.pinnedCommitSha.take(8)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.preferences.showAdvancedAdbDetails) {
                        Text(
                            text = strings.studioManifestSha(selectedPack.sha256Manifest),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mandatory Explicit Unchecked-by-default Consent for Unverified GitHub Links
                    if (selectedPack.trustTier.requiresExtraUnverifiedConsent) {
                        Surface(
                            color = extendedColors.warningContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, extendedColors.warning.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleUnverifiedConsent(!uiState.unverifiedPackConsentChecked) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Checkbox(
                                    checked = uiState.unverifiedPackConsentChecked,
                                    onCheckedChange = onToggleUnverifiedConsent,
                                    modifier = Modifier.testTag("studio_unverified_consent_checkbox")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.studioUnverifiedConsentLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    val applyButtonLabel = when (selectedPack.target) {
                        PackTarget.MOBILE_UI -> strings.studioApplyMobileButton
                        PackTarget.TV_PROFILE -> if (uiState.isAdbConnected) {
                            strings.studioApplyTvProfileButton
                        } else {
                            strings.studioConnectTvFirstButton
                        }
                        PackTarget.TV_LAUNCHER, PackTarget.TV_ADDON -> if (uiState.isAdbConnected) {
                            strings.studioApplyTvThemeButton
                        } else {
                            strings.studioConnectTvFirstButton
                        }
                    }

                    Button(
                        onClick = { onApplyPack(selectedPack) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("studio_apply_selected_pack_button")
                    ) {
                        if (selectedPack.target != PackTarget.MOBILE_UI) {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = applyButtonLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Visual At-a-Glance Decorative & Pack Catalog Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.studioCatalogCountTitle(uiState.filteredPacks.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.studioCatalogSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.filteredPacks.isEmpty()) {
                item {
                    CalmSectionCard {
                        Text(
                            text = strings.studioEmptyFilterMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.filteredPacks, key = { it.id }) { pack ->
                    val isSelected = pack.id == selectedPack.id
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                onSelectPack(pack.id)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index = 2)
                                }
                            }
                            .testTag("pack_card_${pack.id}")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            // Immediate visual preview thumbnail inside every catalog card!
                            PackVisualCatalogThumbnail(
                                pack = pack,
                                language = lang,
                                isSelected = isSelected,
                                modifier = Modifier.testTag("pack_thumbnail_${pack.id}")
                            )

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = pack.localizedName(lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TrustTierBadge(tier = pack.trustTier, language = lang)
                            }

                            Text(
                                text = pack.localizedDescription(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TargetScopeBadge(target = pack.target, language = lang)
                                Text(
                                    text = "${pack.author} · ${pack.license}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val importState = uiState.gitHubImportState
    if (importState is GitHubImportCheckState.Editing) {
        AlertDialog(
            onDismissRequest = onCloseGitHubImport,
            title = { Text(strings.githubDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.githubDialogDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importState.rawUrl,
                        onValueChange = onUpdateGitHubUrl,
                        enabled = !importState.isImporting,
                        label = { Text(strings.githubUrlInputLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_url_input")
                    )
                    if (importState.isImporting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = strings.githubImportingProgress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    importState.localizedError(lang)?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_url_error")
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    importState.parsedSummary?.let { parsed ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_url_valid")
                        ) {
                            Text(
                                text = strings.githubUrlValidSummary(
                                    parsed.owner,
                                    parsed.repo,
                                    parsed.refOrDefault,
                                    parsed.relativePath
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onVerifyGitHubUrl,
                        enabled = !importState.isImporting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("github_verify_url_button")
                    ) {
                        Text(strings.githubVerifyButton)
                    }
                    Button(
                        onClick = onImportFromGitHubLive,
                        enabled = !importState.isImporting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("github_import_live_button")
                    ) {
                        Text(strings.githubImportLiveButton)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseGitHubImport) {
                    Text(strings.dialogClose)
                }
            }
        )
    }
}
