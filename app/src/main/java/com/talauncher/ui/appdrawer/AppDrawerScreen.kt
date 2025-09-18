package com.talauncher.ui.appdrawer

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
// import androidx.compose.ui.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talauncher.R
import com.talauncher.data.model.AppInfo
import com.talauncher.ui.components.GoogleSearchItem
import com.talauncher.ui.components.MathChallengeDialog
import com.talauncher.ui.components.TimeLimitDialog
import com.talauncher.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import kotlin.math.max

@Composable
fun AppDrawerScreen(
    viewModel: AppDrawerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    var showHiddenApps by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current

    val locale = remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }

    val filteredApps = remember(uiState.allApps, searchQuery) {
        uiState.allApps.filter { app ->
            !app.isHidden &&
                app.appName.contains(searchQuery, ignoreCase = true)
        }
    }

    val sortedFilteredApps = remember(filteredApps, collator) {
        filteredApps.sortedWith { left, right ->
            val labelComparison = collator.compare(left.appName, right.appName)
            if (labelComparison != 0) {
                labelComparison
            } else {
                left.packageName.compareTo(right.packageName)
            }
        }
    }

    val sections = remember(sortedFilteredApps, uiState.recentApps, searchQuery, locale) {
        buildList {
            if (uiState.recentApps.isNotEmpty() && searchQuery.isEmpty()) {
                add(
                    AppDrawerSection(
                        key = RECENT_SECTION_KEY,
                        label = "Recently Used",
                        apps = uiState.recentApps,
                        isIndexable = false
                    )
                )
            }

            if (sortedFilteredApps.isNotEmpty()) {
                val grouped = linkedMapOf<String, MutableList<AppInfo>>()
                sortedFilteredApps.forEach { app ->
                    val sectionKey = sectionKeyForApp(app.appName, locale)
                    val sectionApps = grouped.getOrPut(sectionKey) { mutableListOf() }
                    sectionApps += app
                }
                grouped.forEach { (key, apps) ->
                    add(
                        AppDrawerSection(
                            key = key,
                            label = key,
                            apps = apps,
                            isIndexable = true
                        )
                    )
                }
            }
        }
    }

    val sectionPositions = remember(sections) {
        var currentIndex = 0
        buildMap<String, SectionPosition> {
            sections.forEach { section ->
                if (section.isIndexable && section.apps.isNotEmpty()) {
                    put(
                        section.key,
                        SectionPosition(
                            index = currentIndex,
                            previewAppName = section.apps.first().appName
                        )
                    )
                }
                currentIndex += 1 + section.apps.size
            }
        }
    }

    val alphabetEntries = remember(sections, sectionPositions) {
        if (sectionPositions.isEmpty()) {
            emptyList()
        } else {
            val baseAlphabet = ('A'..'Z').map { it.toString() }
            val sectionKeys = sections.filter { it.isIndexable }.map { it.key }
            val extraKeys = sectionKeys.filter { it !in baseAlphabet && it != "#" }
            val orderedKeys = (baseAlphabet + extraKeys + listOf("#")).distinct()
            orderedKeys.map { key ->
                val position = sectionPositions[key]
                AlphabetIndexEntry(
                    key = key,
                    displayLabel = key,
                    targetIndex = position?.index,
                    hasApps = position != null,
                    previewAppName = position?.previewAppName
                )
            }
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isScrubbing by remember { mutableStateOf(false) }
    var previewEntry by remember { mutableStateOf<AlphabetIndexEntry?>(null) }
    var previewFraction by remember { mutableStateOf(0f) }
    var lastScrubbedKey by remember { mutableStateOf<String?>(null) }

    val showIndex = searchQuery.isEmpty() && alphabetEntries.isNotEmpty()

    BackHandler(
        enabled = uiState.showFrictionDialog ||
            uiState.showTimeLimitDialog ||
            uiState.showMathChallengeDialog ||
            uiState.selectedAppForAction != null ||
            uiState.appBeingRenamed != null
    ) {
        when {
            uiState.appBeingRenamed != null -> {
                viewModel.dismissRenameDialog()
            }
            uiState.showMathChallengeDialog -> {
                // Math challenge cannot be dismissed with back button - force completion
            }
            uiState.showTimeLimitDialog -> {
                // Time limit dialog cannot be dismissed with back button - force choice
            }
            uiState.showFrictionDialog -> {
                // Friction dialog can be dismissed to respect user choice
                viewModel.dismissFrictionDialog()
            }
            uiState.selectedAppForAction != null -> {
                // App action dialog can be dismissed
                viewModel.dismissAppActionDialog()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PrimerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PrimerSpacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(2.dp, PrimerBlue.copy(alpha = 0.3f))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Search apps...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimerGray600,
                            modifier = Modifier.padding(start = PrimerSpacing.xs)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    keyboardController?.hide()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = PrimerGray600
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = PrimerSpacing.xs,
                                    vertical = 0.dp
                                )
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimerBlue,
                        unfocusedBorderColor = PrimerGray300,
                        cursorColor = PrimerBlue
                    ),
                    shape = PrimerShapes.small
                )
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f)
            ) {
                val density = LocalDensity.current
                val bubbleHeight = 72.dp
                val previewOffsetY = remember(
                    isScrubbing,
                    previewFraction,
                    maxHeight,
                    previewEntry,
                    density
                ) {
                    if (!isScrubbing || previewEntry == null || maxHeight == Dp.Unspecified) {
                        0.dp
                    } else {
                        with(density) {
                            val containerPx = maxHeight.toPx().coerceAtLeast(0f)
                            val bubblePx = bubbleHeight.toPx()
                            val center = previewFraction * containerPx
                            val top = (center - bubblePx / 2f).coerceIn(0f, max(containerPx - bubblePx, 0f))
                            top.toDp()
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = PrimerSpacing.md,
                        end = PrimerSpacing.md + 48.dp,
                        top = PrimerSpacing.sm,
                        bottom = PrimerSpacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs)
                ) {
                    // Show Google search as first option when searching
                    if (searchQuery.isNotBlank()) {
                        item {
                            GoogleSearchItem(
                                query = searchQuery,
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.performGoogleSearch(searchQuery)
                                }
                            )
                        }
                    }

                    sections.forEachIndexed { index, section ->
                        item {
                            SectionHeader(
                                label = section.label,
                                modifier = Modifier
                                    .padding(
                                        top = if (index == 0) PrimerSpacing.sm else PrimerSpacing.xs,
                                        bottom = PrimerSpacing.xs
                                    )
                            )
                        }

                        if (section.key == RECENT_SECTION_KEY) {
                            items(section.apps, key = { "recent_${it.packageName}" }) { app ->
                                RecentAppItem(
                                    appInfo = app,
                                    onClick = {
                                        viewModel.launchApp(app.packageName)
                                        keyboardController?.hide()
                                    },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.showAppActionDialog(app)
                                    }
                                )
                            }
                        } else {
                            items(section.apps, key = { it.packageName }) { app ->
                                AppDrawerItem(
                                    appInfo = app,
                                    onClick = {
                                        viewModel.launchApp(app.packageName)
                                        keyboardController?.hide()
                                    },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.showAppActionDialog(app)
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.hiddenApps.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(PrimerSpacing.lg))

                            PrimerCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = PrimerSpacing.sm),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, PrimerGray300)
                            ) {
                                TextButton(
                                    onClick = {
                                        showHiddenApps = !showHiddenApps
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(PrimerSpacing.sm)
                                ) {
                                    Text(
                                        text = if (showHiddenApps) {
                                            "Hide hidden apps (${uiState.hiddenApps.size})"
                                        } else {
                                            "Show hidden apps (${uiState.hiddenApps.size})"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PrimerBlue
                                    )
                                }
                            }
                        }

                        if (showHiddenApps) {
                            item {
                                SectionHeader(
                                    label = "Hidden Apps",
                                    modifier = Modifier.padding(bottom = PrimerSpacing.xs)
                                )
                            }
                            items(uiState.hiddenApps, key = { "hidden_${it.packageName}" }) { app ->
                                HiddenAppItem(
                                    appInfo = app,
                                    onClick = {
                                        viewModel.launchApp(app.packageName)
                                        keyboardController?.hide()
                                    },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.unhideApp(app.packageName)
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(PrimerSpacing.xl))
                    }
                }

                if (showIndex) {
                    AlphabetIndex(
                        entries = alphabetEntries,
                        activeKey = previewEntry?.key,
                        isEnabled = showIndex,
                        modifier = Modifier
                            .align(
                                if (layoutDirection == LayoutDirection.Rtl) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                }
                            )
                            .padding(horizontal = PrimerSpacing.sm),
                        onEntryFocused = { entry, fraction ->
                            previewEntry = entry
                            previewFraction = fraction
                            if (entry.hasApps && entry.key != lastScrubbedKey) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            if (entry.hasApps && entry.targetIndex != null) {
                                coroutineScope.launch {
                                    listState.scrollToItem(entry.targetIndex)
                                }
                            }
                            lastScrubbedKey = entry.key
                        },
                        onScrubbingChanged = { active ->
                            isScrubbing = active
                            if (active) {
                                keyboardController?.hide()
                            } else {
                                previewEntry = null
                                previewFraction = 0f
                                lastScrubbedKey = null
                            }
                        }
                    )
                }

                if (isScrubbing && previewEntry != null && maxHeight != Dp.Unspecified) {
                    ScrubPreviewBubble(
                        letter = previewEntry!!.displayLabel,
                        appName = previewEntry!!.previewAppName,
                        modifier = Modifier
                            .align(
                                if (layoutDirection == LayoutDirection.Rtl) {
                                    Alignment.TopStart
                                } else {
                                    Alignment.TopEnd
                                }
                            )
                            .padding(
                                start = if (layoutDirection == LayoutDirection.Rtl) 72.dp else 0.dp,
                                end = if (layoutDirection == LayoutDirection.Rtl) 0.dp else 72.dp,
                                top = previewOffsetY
                            )
                    )
                }
            }
        }

        AppActionDialog(
            app = uiState.selectedAppForAction,
            onDismiss = viewModel::dismissAppActionDialog,
            onPin = viewModel::pinApp,
            onUnpin = viewModel::unpinApp,
            onRename = viewModel::startRenamingApp,
            onHide = viewModel::hideApp,
            onAppInfo = { packageName ->
                viewModel.openAppInfo(context, packageName)
            },
            onUninstall = { packageName ->
                viewModel.uninstallApp(context, packageName)
            }
        )

        RenameAppDialog(
            app = uiState.appBeingRenamed,
            newName = uiState.renameInput,
            onNameChange = viewModel::updateRenameInput,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::dismissRenameDialog
        )

        if (uiState.showFrictionDialog) {
            run {
                val selectedPackage = uiState.selectedAppForFriction ?: return@run
                com.talauncher.ui.home.FrictionDialog(
                    appPackageName = selectedPackage,
                    onDismiss = { viewModel.dismissFrictionDialog() },
                    onProceed = { reason ->
                        viewModel.launchAppWithReason(selectedPackage, reason)
                    }
                )
            }
        }

        if (uiState.showTimeLimitDialog) {
            run {
                val selectedPackage = uiState.selectedAppForTimeLimit ?: return@run
                var appName by remember(selectedPackage) { mutableStateOf(selectedPackage) }

                LaunchedEffect(selectedPackage) {
                    appName = withContext(Dispatchers.IO) {
                        try {
                            val packageManager = context.packageManager
                            val appInfo = packageManager.getApplicationInfo(selectedPackage, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            selectedPackage
                        }
                    }
                }

                TimeLimitDialog(
                    appName = appName,
                    onConfirm = { durationMinutes ->
                        viewModel.launchAppWithTimeLimit(selectedPackage, durationMinutes)
                    },
                    onDismiss = { viewModel.dismissTimeLimitDialog() }
                )
            }
        }

        if (uiState.showMathChallengeDialog) {
            run {
                val selectedPackage = uiState.selectedAppForMathChallenge ?: return@run
                MathChallengeDialog(
                    difficulty = uiState.mathChallengeDifficulty,
                    onCorrect = {
                        viewModel.onMathChallengeCompleted(selectedPackage)
                    },
                    onDismiss = { viewModel.dismissMathChallengeDialog() },
                    isTimeExpired = false
                )
            }
        }
    }
}

private const val RECENT_SECTION_KEY = "recent"

private data class AppDrawerSection(
    val key: String,
    val label: String,
    val apps: List<AppInfo>,
    val isIndexable: Boolean
)

private data class SectionPosition(
    val index: Int,
    val previewAppName: String?
)

private data class AlphabetIndexEntry(
    val key: String,
    val displayLabel: String,
    val targetIndex: Int?,
    val hasApps: Boolean,
    val previewAppName: String?
)

@Composable
private fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(horizontal = PrimerSpacing.xs, vertical = PrimerSpacing.xs),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlphabetIndex(
    entries: List<AlphabetIndexEntry>,
    activeKey: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onEntryFocused: (AlphabetIndexEntry, Float) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit
) {
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    fun resolveEntry(positionY: Float): Pair<AlphabetIndexEntry, Float>? {
        if (entries.isEmpty() || componentSize.height == 0) {
            return null
        }
        val totalHeight = componentSize.height.toFloat()
        val clampedY = positionY.coerceIn(0f, totalHeight)
        val entryHeight = totalHeight / entries.size
        val index = (clampedY / entryHeight).toInt().coerceIn(0, entries.lastIndex)
        val center = (index + 0.5f) * entryHeight
        val fraction = center / totalHeight
        return entries[index] to fraction
    }

    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .onSizeChanged { componentSize = it }
            .pointerInput(entries, isEnabled) {
                if (!isEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrubbingChanged(true)
                    resolveEntry(down.position.y)?.let { (entry, fraction) ->
                        onEntryFocused(entry, fraction)
                    }
                    try {
                        drag(down.id) { change ->
                            resolveEntry(change.position.y)?.let { (entry, fraction) ->
                                onEntryFocused(entry, fraction)
                            }
                            change.consume()
                        }
                    } finally {
                        onScrubbingChanged(false)
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = PrimerSpacing.md),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            entries.forEach { entry ->
                val isActive = isEnabled && entry.hasApps && entry.key == activeKey
                val color = when {
                    !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    entry.hasApps && isActive -> MaterialTheme.colorScheme.onSurface
                    entry.hasApps -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
                Text(
                    text = entry.displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun ScrubPreviewBubble(
    letter: String,
    appName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = PrimerShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PrimerSpacing.md, vertical = PrimerSpacing.sm)
                .widthIn(min = 64.dp, max = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PrimerSpacing.xs)
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = appName ?: "No apps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private fun sectionKeyForApp(label: String, locale: Locale): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) {
        return "#"
    }
    val firstChar = trimmed.firstOrNull { it.isLetterOrDigit() } ?: return "#"
    if (!firstChar.isLetter()) {
        return "#"
    }
    val upper = firstChar.toString().uppercase(locale)
    return upper.take(1)
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, PrimerGray200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (appInfo.isPinned) {
                Surface(
                    color = PrimerBlue.copy(alpha = 0.1f),
                    shape = PrimerShapes.small,
                    border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Pinned",
                        modifier = Modifier.padding(
                            horizontal = PrimerSpacing.xs,
                            vertical = 2.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimerBlue
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, PrimerGray300.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = PrimerGray500.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, PrimerGray500.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Hidden",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimerGray600
                )
            }
        }
    }
}

@Composable
fun AppActionDialog(
    app: AppInfo?,
    onDismiss: () -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onRename: (AppInfo) -> Unit,
    onHide: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    if (app != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose an action:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
                    ) {
                        ActionTextButton(
                            label = if (app.isPinned) "Unpin from essentials" else "Pin to essentials",
                            description = if (app.isPinned) {
                                "Remove this app from your quick access list."
                            } else {
                                "Add this app to your essentials list for quick access."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (app.isPinned) {
                                    onUnpin(app.packageName)
                                } else {
                                    onPin(app.packageName)
                                }
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = stringResource(R.string.rename_app_action_label),
                            description = stringResource(R.string.rename_app_action_description),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onRename(app)
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = "Hide app",
                            description = "Move this app to the hidden list.",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onHide(app.packageName)
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = "View app info",
                            description = "Open the system settings page for this app.",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onAppInfo(app.packageName)
                                onDismiss()
                            }
                        )

                        ActionTextButton(
                            label = "Uninstall app",
                            description = "Remove this app from your device.",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onUninstall(app.packageName)
                                onDismiss()
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = PrimerShapes.medium
        )
    }
}

@Composable
fun RenameAppDialog(
    app: AppInfo?,
    newName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (app != null) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val trimmedName = newName.trim()
        val isConfirmEnabled = trimmedName.isNotEmpty() && trimmedName != app.appName

        LaunchedEffect(app.packageName) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.rename_app_dialog_title, app.appName),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.rename_app_dialog_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = onNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        label = {
                            Text(stringResource(R.string.rename_app_dialog_field_label))
                        },
                        placeholder = {
                            Text(stringResource(R.string.rename_app_dialog_placeholder))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isConfirmEnabled) {
                                    onConfirm()
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimerBlue,
                            unfocusedBorderColor = PrimerGray300,
                            cursorColor = PrimerBlue
                        ),
                        shape = PrimerShapes.small
                    )

                    Text(
                        text = stringResource(R.string.rename_app_dialog_supporting_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isConfirmEnabled) {
                            onConfirm()
                            keyboardController?.hide()
                        }
                    },
                    enabled = isConfirmEnabled
                ) {
                    Text(stringResource(R.string.rename_app_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        onDismiss()
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = PrimerShapes.medium
        )
    }
}

@Composable
fun ActionTextButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimerCard(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PrimerSpacing.md,
                    vertical = PrimerSpacing.sm
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PrimerSpacing.xs))

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = PrimerBlue.copy(alpha = 0.05f) // Slight blue tint for recent apps
        ),
        border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md)
                .heightIn(min = PrimerListItemDefaults.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appInfo.appName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Show a "recent" indicator
            Surface(
                color = PrimerBlue.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, PrimerBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Recent",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimerBlue
                )
            }
        }
    }
}
