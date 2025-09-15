package com.talauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.talauncher.data.database.LauncherDatabase
import com.talauncher.data.repository.AppRepository
import com.talauncher.data.repository.SettingsRepository
import com.talauncher.ui.appdrawer.AppDrawerScreen
import com.talauncher.ui.appdrawer.AppDrawerViewModel
import com.talauncher.ui.home.HomeScreen
import com.talauncher.ui.home.HomeViewModel
import com.talauncher.ui.insights.InsightsScreen
import com.talauncher.ui.insights.InsightsViewModel
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
        val appRepository = AppRepository(database.appDao(), this)
        val settingsRepository = SettingsRepository(database.settingsDao())
        val usageStatsHelper = UsageStatsHelper(this)

        setContent {
            TALauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mainViewModel: MainViewModel = viewModel {
                        MainViewModel(settingsRepository)
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
    usageStatsHelper: UsageStatsHelper,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val viewModel: HomeViewModel = viewModel {
                HomeViewModel(appRepository, settingsRepository)
            }
            HomeScreen(
                onNavigateToAppDrawer = { navController.navigate("app_drawer") },
                onNavigateToInsights = { navController.navigate("insights") },
                onNavigateToSettings = { navController.navigate("settings") },
                viewModel = viewModel
            )
        }

        composable("app_drawer") {
            val viewModel: AppDrawerViewModel = viewModel {
                AppDrawerViewModel(appRepository, settingsRepository)
            }
            AppDrawerScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable("insights") {
            val viewModel: InsightsViewModel = viewModel {
                InsightsViewModel(usageStatsHelper)
            }
            InsightsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = viewModel {
                SettingsViewModel(appRepository, settingsRepository)
            }
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
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
            text = "ZenLauncher",
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