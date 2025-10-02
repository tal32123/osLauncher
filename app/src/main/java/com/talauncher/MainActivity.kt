package com.talauncher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.talauncher.data.repository.SearchInteractionRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
// AppDrawer imports removed - functionality moved to HomeScreen
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
import com.talauncher.utils.IdlingResourceHelper
import com.talauncher.ui.components.ErrorDialog
import com.talauncher.receivers.PackageChangeReceiver
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private var shouldNavigateToHome by mutableStateOf(false)
    private lateinit var sessionRepository: SessionRepository
    private lateinit var appRepository: AppRepository
    private lateinit var searchInteractionRepository: SearchInteractionRepository
    private lateinit var errorHandler: MainErrorHandler
    private lateinit var permissionsHelper: PermissionsHelper
    private lateinit var usageStatsHelper: UsageStatsHelper
    private lateinit var contactHelper: ContactHelper
    private var packageChangeReceiver: PackageChangeReceiver? = null

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
                applicationContext,
                settingsRepository,
                this.sessionRepository,
                this.errorHandler
            )
            this.searchInteractionRepository = SearchInteractionRepository(database.searchInteractionDao())

            packageChangeReceiver = PackageChangeReceiver.register(
                context = applicationContext,
                onPackageChanged = {
                    lifecycleScope.launch {
                        appRepository.syncInstalledApps()
                    }
                }
            )

            // Run asynchronous initialization tasks
            // Add delay to allow UI to fully initialize first for Espresso tests
            lifecycleScope.launch {
                try {
                    // Signal to IdlingResource that async work is starting
                    IdlingResourceHelper.increment()

                    // Brief delay to allow activity to fully start for testing
                    kotlinx.coroutines.delay(100)

                    // Read and store commit info (defer further for tests)
                    launch {
                        IdlingResourceHelper.increment()
                        try {
                            kotlinx.coroutines.delay(500)
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
                        } finally {
                            IdlingResourceHelper.decrement()
                        }
                    }

                    // Signal to IdlingResource that main initialization is done
                    IdlingResourceHelper.decrement()
                } catch (e: Exception) {
                    IdlingResourceHelper.decrement()
                    Log.e(TAG, "Error during initialization", e)
                    errorHandler.showError(
                        "Initialization Error",
                        "Failed to initialize app components: ${e.localizedMessage ?: e.message}",
                        e
                    )
                }
            }

            setContent {
                val mainViewModel: MainViewModel = viewModel {
                    MainViewModel(settingsRepository, appRepository)
                }
                val mainUiState by mainViewModel.uiState.collectAsState()

                // Log UI state changes
                LaunchedEffect(mainUiState.isOnboardingCompleted, mainUiState.isLoading) {
                    android.util.Log.d("MainActivity", "===== UI State Changed =====")
                    android.util.Log.d("MainActivity", "  isLoading: ${mainUiState.isLoading}")
                    android.util.Log.d("MainActivity", "  isOnboardingCompleted: ${mainUiState.isOnboardingCompleted}")
                    android.util.Log.d("MainActivity", "===== END UI State =====")
                }

                TALauncherTheme(
                    themeMode = mainUiState.themeMode,
                    colorPalette = mainUiState.colorPalette,
                    customColorOption = mainUiState.customColorOption,
                    customPrimaryColor = mainUiState.uiSettings.customPrimaryColor,
                    customSecondaryColor = mainUiState.uiSettings.customSecondaryColor
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
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
                                searchInteractionRepository = searchInteractionRepository,
                                settingsRepository = settingsRepository,
                                permissionsHelper = permissionsHelper,
                                usageStatsHelper = usageStatsHelper,
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
        shouldNavigateToHome = true
        if(::permissionsHelper.isInitialized) {
            permissionsHelper.checkAllPermissions()
        }
        if(::appRepository.isInitialized) {
            lifecycleScope.launch {
                appRepository.syncInstalledApps()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PackageChangeReceiver.unregister(applicationContext, packageChangeReceiver)
        packageChangeReceiver = null
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
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

}

@Composable
fun TALauncherApp(
    appRepository: AppRepository,
    searchInteractionRepository: SearchInteractionRepository,
    settingsRepository: SettingsRepository,
    permissionsHelper: PermissionsHelper,
    usageStatsHelper: UsageStatsHelper,
    contactHelper: ContactHelper,
    errorHandler: ErrorHandler? = null,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {}
) {
    LauncherNavigationPager(
        appRepository = appRepository,
        searchInteractionRepository = searchInteractionRepository,
        settingsRepository = settingsRepository,
        permissionsHelper = permissionsHelper,
        usageStatsHelper = usageStatsHelper,
        contactHelper = contactHelper,
        errorHandler = errorHandler,
        shouldNavigateToHome = shouldNavigateToHome,
        onNavigatedToHome = onNavigatedToHome
    )
}

@Composable
fun LauncherNavigationPager(
    appRepository: AppRepository,
    searchInteractionRepository: SearchInteractionRepository,
    settingsRepository: SettingsRepository,
    permissionsHelper: PermissionsHelper,
    usageStatsHelper: UsageStatsHelper,
    contactHelper: ContactHelper,
    errorHandler: ErrorHandler? = null,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {},
    pagerStateListener: ((PagerState) -> Unit)? = null,
    onPageAnimation: ((Int) -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var homeViewModelState by remember { mutableStateOf<HomeViewModel?>(null) }

    LaunchedEffect(Unit) {
        pagerStateListener?.invoke(pagerState)
    }

    suspend fun animateToPage(page: Int) {
        onPageAnimation?.invoke(page)
        val targetPage = page.coerceIn(0, pagerState.pageCount - 1)
        pagerState.animateScrollToPage(targetPage)
    }

    // Handle home button navigation
    LaunchedEffect(shouldNavigateToHome) {
        if (shouldNavigateToHome) {
            homeViewModelState?.clearSearchOnNavigation()
            if (pagerState.currentPage != 1) {
                // Only animate if we're not already on the home page
                animateToPage(1) // Navigate to essential apps screen
            }
            // Always reset the flag, even if we didn't need to navigate
            onNavigatedToHome()
        }
    }

    // Handle back button - navigate to home screen or stay there
    BackHandler {
        homeViewModelState?.clearSearchOnNavigation()
        coroutineScope.launch {
            if (pagerState.currentPage != 1) {
                // If not on home screen, go to home screen
                animateToPage(1)
            }
            // If already on home screen, do nothing (stay in launcher)
        }
    }

    // Function to launch app and navigate to rightmost screen
    val onLaunchApp: (String, Int?) -> Unit = { _, _ ->
        coroutineScope.launch {
            // Navigate to rightmost screen after an app launch completes elsewhere
            animateToPage(2)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("launcher_navigation_pager")
    ) { page ->
        when (page) {
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("launcher_settings_page")
                ) {
                    // Settings/Insights Screen
                    val settingsViewModel: SettingsViewModel = viewModel {
                        SettingsViewModel(appRepository, settingsRepository, permissionsHelper, usageStatsHelper)
                    }
                    SettingsScreen(
                        onNavigateBack = {},
                        viewModel = settingsViewModel
                    )
                }
            }
            1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("launcher_home_page")
                ) {
                    // Main/Home Screen with pinned apps
                    val context = LocalContext.current
                    val applicationContext = context.applicationContext
                    val homeViewModel: HomeViewModel = viewModel {
                        HomeViewModel(
                            appRepository = appRepository,
                            searchInteractionRepository = searchInteractionRepository,
                            settingsRepository = settingsRepository,
                            onLaunchApp = onLaunchApp,
                            appContext = applicationContext,
                            initialContactHelper = contactHelper,
                            permissionsHelper = permissionsHelper,
                            usageStatsHelper = usageStatsHelper,
                            errorHandler = errorHandler
                        )
                    }
                    LaunchedEffect(homeViewModel) {
                        homeViewModelState = homeViewModel
                    }
                    // Clear search when navigating to home screen
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage == 1) {
                            homeViewModel.clearSearchOnNavigation()
                        }
                    }

                    HomeScreen(
                        viewModel = homeViewModel,
                        permissionsHelper = permissionsHelper,
                        onNavigateToSettings = {
                            coroutineScope.launch {
                                animateToPage(0)
                            }
                        }
                    )
                }
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

