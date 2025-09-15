package com.talauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SessionRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.appdrawer.AppDrawerViewModel
import com.talauncher.ui.appdrawer.NewAppDrawerScreen
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.ui.home.NewHomeScreen
import com.talauncher.ui.main.MainViewModel
import com.talauncher.ui.onboarding.OnboardingScreen
import com.talauncher.ui.onboarding.OnboardingViewModel
import com.talauncher.ui.settings.SettingsScreen
import com.talauncher.ui.settings.SettingsViewModel
import com.talauncher.ui.theme.TALauncherTheme
import com.talauncher.utils.UsageStatsHelper

class MainActivity : ComponentActivity() {
    private var shouldNavigateToHome by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = LauncherDatabase.getDatabase(this)
        val settingsRepository = SettingsRepository(database.settingsDao())
        val sessionRepository = SessionRepository(database.appSessionDao())
        val appRepository = AppRepository(database.appDao(), this, settingsRepository, sessionRepository)
        val usageStatsHelper = UsageStatsHelper(this)

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

                    if (mainUiState.isLoading) {
                        LoadingScreen()
                    } else if (!mainUiState.isOnboardingCompleted) {
                        val onboardingViewModel: OnboardingViewModel = viewModel {
                            OnboardingViewModel(this@MainActivity, settingsRepository)
                        }
                        OnboardingScreen(
                            onOnboardingComplete = mainViewModel::onOnboardingCompleted,
                            viewModel = onboardingViewModel
                        )
                    } else {
                        TALauncherApp(
                            appRepository = appRepository,
                            settingsRepository = settingsRepository,
                            usageStatsHelper = usageStatsHelper,
                            sessionRepository = sessionRepository,
                            shouldNavigateToHome = shouldNavigateToHome,
                            onNavigatedToHome = { shouldNavigateToHome = false }
                        )
                    }
                }
            }
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
    }

    private fun checkExpiredSessions() {
        val database = LauncherDatabase.getDatabase(this)
        val sessionRepository = SessionRepository(database.appSessionDao())

        // This will be handled by the ViewModels to show math challenges
        // The actual checking is done in the UI layer to have access to the dialogs
    }
}

@Composable
fun TALauncherApp(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    usageStatsHelper: UsageStatsHelper,
    sessionRepository: SessionRepository,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {}
) {
    NiagaraLauncherPager(
        appRepository = appRepository,
        settingsRepository = settingsRepository,
        usageStatsHelper = usageStatsHelper,
        sessionRepository = sessionRepository,
        shouldNavigateToHome = shouldNavigateToHome,
        onNavigatedToHome = onNavigatedToHome
    )
}

@Composable
fun NiagaraLauncherPager(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    usageStatsHelper: UsageStatsHelper,
    sessionRepository: SessionRepository,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val settingsState by settingsRepository.getSettings().collectAsState(initial = null)
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
    val onLaunchApp: (String, Int?) -> Unit = { packageName, plannedDuration ->
        coroutineScope.launch {
            appRepository.launchApp(packageName, plannedDuration = plannedDuration)
            // Navigate to rightmost screen after launching app
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
                    SettingsViewModel(appRepository, settingsRepository, usageStatsHelper)
                }
                SettingsScreen(
                    onNavigateBack = {},
                    viewModel = settingsViewModel
                )
            }
            1 -> {
                // Main/Home Screen with pinned apps
                val homeViewModel: HomeViewModel = viewModel {
                    HomeViewModel(appRepository, settingsRepository, onLaunchApp, sessionRepository)
                }
                NewHomeScreen(
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
                val appDrawerViewModel: AppDrawerViewModel = viewModel {
                    AppDrawerViewModel(appRepository, settingsRepository, usageStatsHelper, onLaunchApp)
                }
                NewAppDrawerScreen(
                    viewModel = appDrawerViewModel
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TALauncher",
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