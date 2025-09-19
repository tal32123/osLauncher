package com.talauncher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.R
import kotlinx.coroutines.launch
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.appdrawer.AppDrawerViewModel
import com.talauncher.ui.appdrawer.AppDrawerScreen
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.ui.home.HomeScreen
import com.talauncher.ui.main.MainViewModel
import com.talauncher.ui.onboarding.OnboardingScreen
import com.talauncher.ui.onboarding.OnboardingViewModel
import com.talauncher.ui.settings.SettingsScreen
import com.talauncher.ui.settings.SettingsViewModel
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.utils.ContactHelper
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper
import com.talauncher.utils.ErrorHandler
import com.talauncher.utils.MainErrorHandler
import com.talauncher.utils.CommitInfoReader
import com.talauncher.ui.components.ErrorDialog
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private var shouldNavigateToHome by mutableStateOf(false)
    private lateinit var sessionRepository: SessionRepository
    private lateinit var appRepository: AppRepository
    private lateinit var errorHandler: MainErrorHandler
    private lateinit var permissionsHelper: PermissionsHelper
    private lateinit var usageStatsHelper: UsageStatsHelper
    private lateinit var contactHelper: ContactHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val database = LauncherDatabase.getDatabase(this)
            val settingsRepository = SettingsRepository(database.settingsDao())
            this.sessionRepository = SessionRepository(database.appSessionDao())
            this.errorHandler = MainErrorHandler(this)
            this.permissionsHelper = PermissionsHelper(applicationContext)
            this.usageStatsHelper = UsageStatsHelper(applicationContext)
            this.contactHelper = ContactHelper(applicationContext, this.permissionsHelper)
            this.appRepository = AppRepository(
                database.appDao(),
                this,
                settingsRepository,
                this.sessionRepository,
                this.errorHandler
            )

            lifecycleScope.launch {
                sessionRepository.initialize()
                sessionRepository.emitExpiredSessions()
            }

            // Read and store commit info
            lifecycleScope.launch {
                val commitInfo = CommitInfoReader.readCommitInfo(this@MainActivity)
                commitInfo?.let {
                    settingsRepository.updateBuildInfo(
                        commitHash = it.commit,
                        commitMessage = it.message,
                        commitDate = it.date,
                        branch = it.branch,
                        buildTime = it.buildTime
                    )
                }
            }

            lifecycleScope.launch {
                sessionRepository.observeSessionExpirations().collect {
                    shouldNavigateToHome = true
                }
            }

            setContent {
                TALauncherTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val mainViewModel: MainViewModel = viewModel {
                            MainViewModel(settingsRepository, appRepository)
                        }
                        val mainUiState by mainViewModel.uiState.collectAsState()
                        val errorState by errorHandler.errorState.collectAsState()

                        // Show error dialog if there's an error
                        if (errorState.isVisible) {
                            ErrorDialog(
                                title = errorState.title,
                                message = errorState.message,
                                stackTrace = errorState.stackTrace,
                                onDismiss = { errorHandler.dismissError() },
                                onRetry = errorState.onRetry
                            )
                        }

                        if (mainUiState.isLoading) {
                            LoadingScreen()
                        } else if (!mainUiState.isOnboardingCompleted) {
                            val onboardingViewModel: OnboardingViewModel = viewModel {
                                OnboardingViewModel(settingsRepository)
                            }
                            OnboardingScreen(
                                onOnboardingComplete = mainViewModel::onOnboardingCompleted,
                                viewModel = onboardingViewModel,
                                permissionsHelper = permissionsHelper,
                                usageStatsHelper = usageStatsHelper
                            )
                        } else {
                            TALauncherApp(
                                appRepository = appRepository,
                                settingsRepository = settingsRepository,
                                permissionsHelper = permissionsHelper,
                                usageStatsHelper = usageStatsHelper,
                                sessionRepository = sessionRepository,
                                contactHelper = contactHelper,
                                errorHandler = errorHandler,
                                shouldNavigateToHome = shouldNavigateToHome,
                                onNavigatedToHome = { shouldNavigateToHome = false }
                            )
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Error starting ${getString(R.string.app_name)}", error)
            reportStartupError(error)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // When home button is pressed, navigate to home screen (page 1)
        if (intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)) {
            // Only set flag if we're not already processing a home navigation
            if (!shouldNavigateToHome) {
                shouldNavigateToHome = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for expired sessions when returning to launcher
        checkExpiredSessions()
        // Always navigate back to the home page when the launcher becomes visible
        shouldNavigateToHome = true
        if(::permissionsHelper.isInitialized) {
            permissionsHelper.checkAllPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (::permissionsHelper.isInitialized) {
            permissionsHelper.handlePermissionResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkExpiredSessions() {
        if (!::sessionRepository.isInitialized) {
            return
        }

        lifecycleScope.launch {
            sessionRepository.emitExpiredSessions()
        }
    }
}

@Composable
fun TALauncherApp(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    permissionsHelper: PermissionsHelper,
    usageStatsHelper: UsageStatsHelper,
    sessionRepository: SessionRepository,
    contactHelper: ContactHelper,
    errorHandler: ErrorHandler? = null,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {}
) {
    LauncherNavigationPager(
        appRepository = appRepository,
        settingsRepository = settingsRepository,
        permissionsHelper = permissionsHelper,
        usageStatsHelper = usageStatsHelper,
        sessionRepository = sessionRepository,
        contactHelper = contactHelper,
        errorHandler = errorHandler,
        shouldNavigateToHome = shouldNavigateToHome,
        onNavigatedToHome = onNavigatedToHome
    )
}

@Composable
fun LauncherNavigationPager(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    permissionsHelper: PermissionsHelper,
    usageStatsHelper: UsageStatsHelper,
    sessionRepository: SessionRepository,
    contactHelper: ContactHelper,
    errorHandler: ErrorHandler? = null,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    // Handle home button navigation
    LaunchedEffect(shouldNavigateToHome) {
        if (shouldNavigateToHome) {
            if (pagerState.currentPage != 1) {
                // Only animate if we're not already on the home page
                pagerState.animateScrollToPage(1) // Navigate to essential apps screen
            }
            // Always reset the flag, even if we didn't need to navigate
            onNavigatedToHome()
        }
    }

    // Handle back button - navigate to home screen or stay there
    BackHandler {
        coroutineScope.launch {
            if (pagerState.currentPage != 1) {
                // If not on home screen, go to home screen
                pagerState.animateScrollToPage(1)
            }
            // If already on home screen, do nothing (stay in launcher)
        }
    }

    // Function to launch app and navigate to rightmost screen
    val onLaunchApp: (String, Int?) -> Unit = { _, _ ->
        coroutineScope.launch {
            // Navigate to rightmost screen after an app launch completes elsewhere
            pagerState.animateScrollToPage(2)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                // Settings/Insights Screen
                val settingsViewModel: SettingsViewModel = viewModel {
                    SettingsViewModel(appRepository, settingsRepository, permissionsHelper, usageStatsHelper)
                }
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = settingsViewModel
                )
            }
            1 -> {
                // Main/Home Screen with pinned apps
                val context = LocalContext.current
                val homeViewModel: HomeViewModel = viewModel {
                    HomeViewModel(
                        appRepository,
                        settingsRepository,
                        onLaunchApp,
                        sessionRepository,
                        context,
                        permissionsHelper,
                        usageStatsHelper,
                        errorHandler
                    )
                }
                // Clear search when navigating to home screen
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage == 1) {
                        homeViewModel.clearSearchOnNavigation()
                    }
                }

                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAppDrawer = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    onNavigateToSettings = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    }
                )
            }
            2 -> {
                // All Apps Screen
                val context = LocalContext.current
                val appDrawerViewModel: AppDrawerViewModel = viewModel {
                    AppDrawerViewModel(
                        appRepository,
                        settingsRepository,
                        usageStatsHelper,
                        permissionsHelper,
                        contactHelper,
                        context,
                        onLaunchApp
                    )
                }
                // Clear search when navigating away from app drawer
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != 2) {
                        appDrawerViewModel.clearSearchOnNavigation()
                    }
                }

                AppDrawerScreen(
                    viewModel = appDrawerViewModel
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    val appName = stringResource(R.string.app_name)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = appName,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun Throwable.asStackTraceString(): String {
    val stringWriter = StringWriter()
    PrintWriter(stringWriter).use { printWriter ->
        printStackTrace(printWriter)
        printWriter.flush()
    }
    return stringWriter.toString()
}

private fun ComponentActivity.reportStartupError(error: Throwable) {
    setContent {
        TALauncherTheme {
            StartupErrorScreen(error)
        }
    }
}

@Composable
private fun StartupErrorScreen(error: Throwable) {
    val stackTrace = remember(error) { error.asStackTraceString() }
    val scrollState = rememberScrollState()
    val message = remember(error) {
        error.localizedMessage?.takeIf { it.isNotBlank() } 
            ?: "${error::class.qualifiedName ?: error::class.simpleName}"
    }
    val appName = stringResource(R.string.app_name)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "$appName ran into a problem",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Copy the stack trace below and share it with the team:",
                style = MaterialTheme.typography.bodyMedium
            )
            SelectionContainer {
                Text(
                    text = stackTrace,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

private const val TAG = "MainActivity"