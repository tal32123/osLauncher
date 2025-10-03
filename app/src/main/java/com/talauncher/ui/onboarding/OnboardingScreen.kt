package com.talauncher.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.R
import com.talauncher.data.model.AppIconStyleOption
import com.talauncher.data.model.ThemeModeOption
import com.talauncher.ui.components.Collapsible
import com.talauncher.ui.components.ModernAppItem
import com.talauncher.ui.components.PermissionManager
import com.talauncher.utils.PermissionType
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper

private enum class OnboardingStepId {
    DEFAULT_LAUNCHER,
    USAGE_STATS,
    CONTACTS,
    LOCATION,
    APPEARANCE
}

private enum class AppearanceSection { THEME_MODE, ICON_STYLE, THEME_OPTIONS, WALLPAPER }



@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
    permissionsHelper: PermissionsHelper,
    usageStatsHelper: UsageStatsHelper
) {
    val permissionState by permissionsHelper.permissionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    var isDefaultLauncher by remember { mutableStateOf(usageStatsHelper.isDefaultLauncher()) }

    PermissionManager(permissionsHelper)

    LaunchedEffect(permissionState) {
        isDefaultLauncher = usageStatsHelper.isDefaultLauncher()
    }

    val steps = remember(permissionState) {
        buildList {
            add(OnboardingStepId.DEFAULT_LAUNCHER)
            add(OnboardingStepId.USAGE_STATS)
            add(OnboardingStepId.CONTACTS)
            add(OnboardingStepId.LOCATION)
            add(OnboardingStepId.APPEARANCE)
        }
    }

    val currentStep = steps.getOrNull(uiState.currentStepIndex) ?: OnboardingStepId.APPEARANCE

    val pickWallpaperLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                uiState.customWallpaperPath?.let { existing ->
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(existing),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setCustomWallpaper(it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_title_welcome, appName),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(28.dp))

        when (currentStep) {
            OnboardingStepId.DEFAULT_LAUNCHER -> PermissionStep(
                icon = Icons.Default.Home,
                title = stringResource(R.string.onboarding_step_default_title),
                description = stringResource(R.string.onboarding_step_default_desc, appName),
                isGranted = isDefaultLauncher,
                primaryButtonText = if (isDefaultLauncher) stringResource(R.string.completed) else stringResource(R.string.onboarding_action_set_default),
                onPrimary = { permissionsHelper.requestPermission(context as Activity, PermissionType.DEFAULT_LAUNCHER) },
                onNext = { viewModel.goToNextStep() },
                canProceed = isDefaultLauncher
            )

            OnboardingStepId.USAGE_STATS -> PermissionStep(
                icon = Icons.Default.Info,
                title = stringResource(R.string.onboarding_step_usage_title),
                description = stringResource(R.string.onboarding_step_usage_desc),
                isGranted = permissionState.hasUsageStats,
                primaryButtonText = if (permissionState.hasUsageStats) stringResource(R.string.completed) else stringResource(R.string.onboarding_action_grant_permission),
                onPrimary = { permissionsHelper.requestPermission(context as Activity, PermissionType.USAGE_STATS) },
                onNext = { viewModel.goToNextStep() },
                onBack = { viewModel.goToPreviousStep() },
                canProceed = permissionState.hasUsageStats
            )

            

            OnboardingStepId.CONTACTS -> PermissionStep(
                icon = Icons.Default.Person,
                title = stringResource(R.string.onboarding_step_contacts_title),
                description = stringResource(R.string.onboarding_step_contacts_desc),
                isGranted = permissionState.hasContacts,
                primaryButtonText = if (permissionState.hasContacts) stringResource(R.string.completed) else stringResource(R.string.onboarding_action_grant_permission),
                onPrimary = { permissionsHelper.requestPermission(context as Activity, PermissionType.CONTACTS) },
                onNext = { viewModel.goToNextStep() },
                onBack = { viewModel.goToPreviousStep() },
                canProceed = permissionState.hasContacts,
                allowSkip = true,
                onSkip = { viewModel.goToNextStep() }
            )

            OnboardingStepId.LOCATION -> PermissionStep(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.onboarding_step_location_title),
                description = stringResource(R.string.onboarding_step_location_desc),
                isGranted = permissionState.hasLocation,
                primaryButtonText = if (permissionState.hasLocation) stringResource(R.string.completed) else stringResource(R.string.onboarding_action_grant_permission),
                onPrimary = { permissionsHelper.requestPermission(context as Activity, PermissionType.LOCATION) },
                onNext = { viewModel.goToNextStep() },
                onBack = { viewModel.goToPreviousStep() },
                canProceed = permissionState.hasLocation,
                allowSkip = true,
                onSkip = { viewModel.goToNextStep() }
            )

            OnboardingStepId.APPEARANCE -> AppearanceStep(
                selectedTheme = uiState.selectedThemeMode,
                onSelectTheme = viewModel::setThemeMode,
                selectedIconStyle = uiState.selectedAppIconStyle,
                onSelectIconStyle = viewModel::setAppIconStyle,
                showWallpaper = uiState.showWallpaper,
                onToggleWallpaper = viewModel::setShowWallpaper,
                onPickWallpaper = { pickWallpaperLauncher.launch(arrayOf("image/*")) },
                wallpaperBlurAmount = uiState.wallpaperBlurAmount,
                backgroundOpacity = uiState.backgroundOpacity,
                backgroundColor = uiState.backgroundColor,
                customWallpaperPath = uiState.customWallpaperPath,
                onFinish = {
                    viewModel.completeOnboarding()
                    onOnboardingComplete()
                },
                onBack = { viewModel.goToPreviousStep() },
                onSkip = {
                    viewModel.completeOnboarding()
                    onOnboardingComplete()
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    primaryButtonText: String,
    onPrimary: () -> Unit,
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    canProceed: Boolean = false,
    allowSkip: Boolean = false,
    onSkip: (() -> Unit)? = null
) {
    LaunchedEffect(isGranted) {
        if (isGranted) onNext()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                if (isGranted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPrimary, enabled = !isGranted, modifier = Modifier.fillMaxWidth()) {
                Text(primaryButtonText)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (onBack != null) {
                    TextButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.back)) }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Row {
                    if (allowSkip && onSkip != null) {
                        TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_for_now)) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onNext, enabled = canProceed) { Text(stringResource(R.string.next)) }
                }
            }
        }
    }
}


@Composable
private fun AppearanceStep(
    selectedTheme: ThemeModeOption,
    onSelectTheme: (ThemeModeOption) -> Unit,
    selectedIconStyle: AppIconStyleOption,
    onSelectIconStyle: (AppIconStyleOption) -> Unit,
    showWallpaper: Boolean,
    onToggleWallpaper: (Boolean) -> Unit,
    onPickWallpaper: () -> Unit,
    wallpaperBlurAmount: Float,
    backgroundOpacity: Float,
    backgroundColor: String,
    customWallpaperPath: String?,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<AppearanceSection?>(AppearanceSection.THEME_MODE) }

    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_appearance_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Collapsible(
            title = stringResource(R.string.theme_mode_title),
            isExpanded = expandedSection == AppearanceSection.THEME_MODE,
            onToggle = {
                expandedSection = if (expandedSection == AppearanceSection.THEME_MODE) null else AppearanceSection.THEME_MODE
            }
        ) {
            FlowRowChipsTheme(selectedTheme, onSelectTheme)
        }

        Spacer(Modifier.height(12.dp))

        Collapsible(
            title = stringResource(R.string.settings_app_icons),
            isExpanded = expandedSection == AppearanceSection.ICON_STYLE,
            onToggle = {
                expandedSection = if (expandedSection == AppearanceSection.ICON_STYLE) null else AppearanceSection.ICON_STYLE
            }
        ) {
            Text(
                text = stringResource(R.string.settings_app_icons_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRowChipsIcon(selectedIconStyle, onSelectIconStyle)
        }

        Spacer(Modifier.height(12.dp))

        Collapsible(
            title = stringResource(R.string.color_palette_title),
            isExpanded = expandedSection == AppearanceSection.THEME_OPTIONS,
            onToggle = {
                expandedSection = if (expandedSection == AppearanceSection.THEME_OPTIONS) null else AppearanceSection.THEME_OPTIONS
            }
        ) {
            val obVm: OnboardingViewModel = viewModel()
            val obState by obVm.uiState.collectAsState()
            com.talauncher.ui.components.ColorPaletteSelector(
                selectedPalette = obState.selectedColorPalette,
                onPaletteSelected = { obVm.setColorPalette(it) },
                currentCustomColor = obState.customColorOption,
                onCustomColorSelected = { colorName -> obVm.setCustomPalette(colorName) }
            )
        }

        Spacer(Modifier.height(12.dp))

        Collapsible(
            title = stringResource(R.string.wallpaper_settings_title),
            isExpanded = expandedSection == AppearanceSection.WALLPAPER,
            onToggle = {
                expandedSection = if (expandedSection == AppearanceSection.WALLPAPER) null else AppearanceSection.WALLPAPER
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.settings_show_wallpaper_title), modifier = Modifier.weight(1f))
                Switch(checked = showWallpaper, onCheckedChange = onToggleWallpaper)
            }
            if (showWallpaper) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onPickWallpaper, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.custom_wallpaper_choose))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.color_palette_preview_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        com.talauncher.ui.components.ModernBackdrop(
            showWallpaper = showWallpaper,
            blurAmount = wallpaperBlurAmount,
            backgroundColor = backgroundColor,
            opacity = backgroundOpacity,
            customWallpaperPath = customWallpaperPath,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(9f / 19.5f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                ModernAppItem(appName = "Calendar", packageName = "com.android.calendar", onClick = {}, appIconStyle = selectedIconStyle, enableGlassmorphism = true)
                ModernAppItem(appName = "Messages", packageName = "com.android.messaging", onClick = {}, appIconStyle = selectedIconStyle, enableGlassmorphism = true)
                ModernAppItem(appName = "Notes", packageName = "com.example.notes", onClick = {}, appIconStyle = selectedIconStyle, enableGlassmorphism = true)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.back)) }
            Row {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_for_now)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onFinish) { Text(stringResource(R.string.finish)) }
            }
        }
    }
}
@Composable
private fun FlowRowChipsTheme(selected: ThemeModeOption, onSelect: (ThemeModeOption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeModeOption.entries.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option.label) })
        }
    }
}

@Composable
private fun FlowRowChipsIcon(selected: AppIconStyleOption, onSelect: (AppIconStyleOption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppIconStyleOption.entries.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option.label) })
        }
    }
}










