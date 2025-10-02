package com.talauncher.ui.home.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.talauncher.R
import com.talauncher.domain.quotes.QuotesProvider
import com.talauncher.ui.components.BaseDialog
import com.talauncher.ui.home.MotivationalQuotesProvider
import com.talauncher.ui.theme.PrimerButton
import com.talauncher.ui.theme.PrimerSecondaryButton
import com.talauncher.ui.theme.PrimerShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppNameProvider {
    suspend fun getAppDisplayName(packageName: String): String
}

class DefaultAppNameProvider(private val context: Context) : AppNameProvider {
    override suspend fun getAppDisplayName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}

@Composable
fun FrictionDialog(
    appPackageName: String,
    onDismiss: () -> Unit,
    onProceed: (String) -> Unit,
    quotesProvider: QuotesProvider? = null,
    appNameProvider: AppNameProvider? = null
) {
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current

    val effectiveQuotesProvider = remember(context, quotesProvider) {
        quotesProvider ?: MotivationalQuotesProvider(context)
    }

    val effectiveAppNameProvider = remember(context, appNameProvider) {
        appNameProvider ?: DefaultAppNameProvider(context)
    }

    val motivationalQuote = remember(appPackageName, effectiveQuotesProvider) {
        effectiveQuotesProvider.getRandomQuote()
    }

    var appName by remember(appPackageName) { mutableStateOf(appPackageName) }

    LaunchedEffect(appPackageName) {
        appName = withContext(Dispatchers.IO) {
            effectiveAppNameProvider.getAppDisplayName(appPackageName)
        }
    }

    BaseDialog(
        title = stringResource(R.string.friction_dialog_title),
        onDismissRequest = onDismiss,
        testTag = "friction_dialog",
        confirmButton = {
            PrimerButton(
                onClick = {
                    if (reason.trim().isNotEmpty()) {
                        onProceed(reason.trim())
                    }
                },
                enabled = reason.trim().isNotEmpty(),
                modifier = Modifier.testTag("friction_continue_button")
            ) {
                Text(stringResource(R.string.friction_dialog_continue))
            }
        },
        dismissButton = {
            PrimerSecondaryButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("friction_cancel_button")
            ) {
                Text(stringResource(R.string.friction_dialog_cancel))
            }
        }
    ) {
        Text(
            text = stringResource(R.string.friction_dialog_marked_distracting, appName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.friction_dialog_why),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("friction_reason_input"),
            placeholder = { Text(stringResource(R.string.friction_dialog_reason_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = PrimerShapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (motivationalQuote.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = motivationalQuote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
