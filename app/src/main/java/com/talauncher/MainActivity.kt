package com.talauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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