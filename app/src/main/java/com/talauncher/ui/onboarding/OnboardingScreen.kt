package com.talauncher.ui.onboarding

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talauncher.R
import com.talauncher.ui.components.PermissionManager
import com.talauncher.utils.PermissionType
import com.talauncher.utils.PermissionsHelper
import com.talauncher.utils.UsageStatsHelper

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
    permissionsHelper: PermissionsHelper,
    usageStatsHelper: UsageStatsHelper
) {
    val permissionState by permissionsHelper.permissionState.collectAsState()
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    var isDefaultLauncher by remember { mutableStateOf(usageStatsHelper.isDefaultLauncher()) }

    PermissionManager(permissionsHelper)

    LaunchedEffect(permissionState.allOnboardingPermissionsGranted, isDefaultLauncher) {
        if (permissionState.allOnboardingPermissionsGranted && isDefaultLauncher) {
            viewModel.completeOnboarding()
            onOnboardingComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to $appName",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A minimalist launcher designed to reduce digital distraction",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Setup Required",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Set as Default Launcher
        OnboardingStepCard(
            icon = Icons.Default.Home,
            title = "Set as Default Launcher",
            description = "Make $appName your default home screen",
            isCompleted = isDefaultLauncher,
            buttonText = if (isDefaultLauncher) "Completed" else "Set as Default",
            onButtonClick = {
                permissionsHelper.requestPermission(context as Activity, PermissionType.DEFAULT_LAUNCHER)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Usage Stats Permission
        OnboardingStepCard(
            icon = Icons.Default.Info,
            title = "Usage Statistics Permission",
            description = "Required to show your app usage insights and track time spent in distracting apps",
            isCompleted = permissionState.hasUsageStats,
            buttonText = if (permissionState.hasUsageStats) "Completed" else "Grant Permission",
            onButtonClick = {
                permissionsHelper.requestPermission(context as Activity, PermissionType.USAGE_STATS)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnboardingStepCard(
                icon = Icons.Default.Notifications,
                title = "Allow Notifications",
                description = "Required to show gentle reminders when time limits expire",
                isCompleted = permissionState.hasNotifications,
                buttonText = if (permissionState.hasNotifications) "Completed" else "Allow Notifications",
                onButtonClick = {
                    permissionsHelper.requestPermission(context as Activity, PermissionType.NOTIFICATIONS)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // System Alert Window Permission
        OnboardingStepCard(
            icon = Icons.Default.Notifications,
            title = "Overlay Permission",
            description = "Allows timer notifications to appear over other apps when time limits expire",
            isCompleted = permissionState.hasSystemAlertWindow,
            buttonText = if (permissionState.hasSystemAlertWindow) "Completed" else "Grant Permission",
            onButtonClick = {
                permissionsHelper.requestPermission(context as Activity, PermissionType.SYSTEM_ALERT_WINDOW)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (permissionState.allOnboardingPermissionsGranted && isDefaultLauncher) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Setup Complete!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$appName is ready to help you focus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Complete all steps above to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStepCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isCompleted: Boolean,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onButtonClick,
                enabled = !isCompleted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
