package com.talauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.repository.AppRepository
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = LauncherDatabase.getDatabase(this)
        val settingsRepository = SettingsRepository(database.settingsDao())
        val appRepository = AppRepository(database.appDao(), this, settingsRepository)
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
                            usageStatsHelper = usageStatsHelper
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TALauncherApp(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    usageStatsHelper: UsageStatsHelper
) {
    NiagaraLauncherPager(
        appRepository = appRepository,
        settingsRepository = settingsRepository,
        usageStatsHelper = usageStatsHelper
    )
}

@Composable
fun NiagaraLauncherPager(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    usageStatsHelper: UsageStatsHelper
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val settingsState by settingsRepository.getSettings().collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    // Function to launch app and navigate to rightmost screen unless focus mode is enabled
    val onLaunchApp: (String) -> Unit = { packageName ->
        coroutineScope.launch {
            appRepository.launchApp(packageName)
            // Navigate to rightmost screen unless focus mode is enabled
            if (settingsState?.isFocusModeEnabled != true) {
                pagerState.animateScrollToPage(2)
            }
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
                    HomeViewModel(appRepository, settingsRepository, onLaunchApp)
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
                    AppDrawerViewModel(appRepository, settingsRepository, onLaunchApp)
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