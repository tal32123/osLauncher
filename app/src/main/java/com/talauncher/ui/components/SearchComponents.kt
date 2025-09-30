package com.talauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
// import androidx.compose.foundation.lazy.LazyListDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.talauncher.R
import com.talauncher.ui.home.SearchItem
import com.talauncher.ui.theme.PrimerSpacing
import com.talauncher.ui.theme.getUiDensity
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.theme.UiSettings
import com.talauncher.utils.SearchScoring

@Composable
fun UnifiedSearchResults(
    searchQuery: String,
    searchResults: List<SearchItem>,
    onAppClick: (String) -> Unit,
    onAppLongClick: (SearchItem.App) -> Unit,
    onContactCall: (SearchItem.Contact) -> Unit,
    onContactMessage: (SearchItem.Contact) -> Unit,
    onContactWhatsApp: (SearchItem.Contact) -> Unit,
    onContactOpen: (SearchItem.Contact) -> Unit,
    showPhoneAction: Boolean,
    showMessageAction: Boolean,
    showWhatsAppAction: Boolean,
    onGoogleSearch: (String) -> Unit,
    showContactsPermissionMissing: Boolean,
    onGrantContactsPermission: () -> Unit,
    uiSettings: UiSettings = UiSettings(),
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs),
        contentPadding = PaddingValues(bottom = 80.dp), // Extra bottom padding for accessibility
    ) {
        // Always show Google search as first option
        item {
            GoogleSearchItem(
                query = searchQuery,
                onClick = {
                    keyboardController?.hide()
                    onGoogleSearch(searchQuery)
                }
            )
        }

        // Show unified search results (apps and contacts ordered by relevance)
        if (searchResults.isNotEmpty()) {
            items(searchResults, key = {
                when (it) {
                    is SearchItem.App -> "app_${it.appInfo.packageName}"
                    is SearchItem.Contact -> "contact_${it.contactInfo.id}"
                }
            }) { searchItem ->
                when (searchItem) {
                    is SearchItem.App -> {
                        ModernAppItem(
                            appName = searchItem.appInfo.appName,
                            isHidden = searchItem.appInfo.isHidden,
                            onClick = {
                                keyboardController?.hide()
                                onAppClick(searchItem.appInfo.packageName)
                            },
                            onLongClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppLongClick(searchItem)
                            },
                            enableGlassmorphism = uiSettings.enableGlassmorphism,
                            uiDensity = uiSettings.getUiDensity()
                        )
                    }
                    is SearchItem.Contact -> {
                        ContactItem(
                            contact = searchItem.contactInfo,
                            onCall = {
                                keyboardController?.hide()
                                onContactCall(searchItem)
                            },
                            onMessage = {
                                keyboardController?.hide()
                                onContactMessage(searchItem)
                            },
                            onWhatsApp = {
                                keyboardController?.hide()
                                onContactWhatsApp(searchItem)
                            },
                            onOpenContact = {
                                keyboardController?.hide()
                                onContactOpen(searchItem)
                            },
                            showPhoneAction = showPhoneAction,
                            showMessageAction = showMessageAction,
                            showWhatsAppAction = showWhatsAppAction
                        )
                    }
                }
            }
        }

        if (showContactsPermissionMissing && searchQuery.isNotBlank()) {
            item {
                ContactPermissionCallout(
                    onGrantAccess = {
                        keyboardController?.hide()
                        onGrantContactsPermission()
                    },
                    uiSettings = uiSettings
                )
            }
        }

        // Show no results message only if unified results are empty
        if (searchResults.isEmpty() && searchQuery.isNotBlank() && !showContactsPermissionMissing) {
            item {
                Text(
                    text = stringResource(R.string.home_search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(PrimerSpacing.md)
                )
            }
        }
    }
}

@Composable
fun ContactPermissionCallout(
    onGrantAccess: () -> Unit,
    uiSettings: UiSettings = UiSettings()
) {
    ModernGlassCard(
        modifier = Modifier.fillMaxWidth().testTag("contact_permission_callout"),
        enableGlassmorphism = uiSettings.enableGlassmorphism,
        cornerRadius = 12,
        elevation = 1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.contact_permission_missing_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = onGrantAccess,
                modifier = Modifier.testTag("grant_contacts_permission_button"),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Text(stringResource(R.string.contact_permission_required_confirm))
            }
        }
    }
}

@Composable
fun AppDrawerUnifiedSearchResults(
    searchQuery: String,
    allApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onGoogleSearch: (String) -> Unit,
    uiSettings: UiSettings = UiSettings(),
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current

    // Filter apps based on search query using fuzzy search
    val filteredApps = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        allApps.mapNotNull { app ->
                val score = SearchScoring.calculateRelevanceScore(searchQuery, app.appName)
                if (score > 0) app to score else null
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs),
        contentPadding = PaddingValues(bottom = 80.dp), // Extra bottom padding for accessibility
    ) {
        // Always show Google search as first option
        if (searchQuery.isNotBlank()) {
            item {
                GoogleSearchItem(
                    query = searchQuery,
                    onClick = {
                        keyboardController?.hide()
                        onGoogleSearch(searchQuery)
                    }
                )
            }
        }

        // Show filtered apps with long press support
        if (filteredApps.isNotEmpty()) {
            items(filteredApps, key = { it.packageName }) { app ->
                ModernAppItem(
                    appName = app.appName,
                    isHidden = app.isHidden,
                    onClick = {
                        keyboardController?.hide()
                        onAppClick(app.packageName)
                    },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAppLongClick(app)
                    },
                    enableGlassmorphism = uiSettings.enableGlassmorphism,
                    uiDensity = uiSettings.getUiDensity()
                )
            }
        }

        // Show no results message when no apps found
        if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Text(
                    text = stringResource(R.string.home_search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(PrimerSpacing.md)
                )
            }
        }
    }
}
