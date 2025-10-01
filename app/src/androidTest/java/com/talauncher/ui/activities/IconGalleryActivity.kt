package com.talauncher.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talauncher.ui.components.AppIcon
import com.talauncher.data.model.AppIconStyleOption

class IconGalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apps = packageManager.getInstalledApplications(0)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp)) {
                        items(apps) {
                            AppIcon(
                                packageName = it.packageName,
                                appName = it.loadLabel(packageManager).toString(),
                                iconStyle = AppIconStyleOption.THEMED
                            )
                        }
                    }
                }
            }
        }
    }
}
