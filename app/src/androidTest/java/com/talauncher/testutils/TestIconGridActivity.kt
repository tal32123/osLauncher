package com.talauncher.testutils

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talauncher.R
import com.talauncher.ui.appdrawer.AppGridAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class TestIconGridActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_icon_grid)
        recyclerView = findViewById(R.id.test_icon_grid)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val density = resources.displayMetrics.density
        val iconSizePx = (64 * density).roundToInt()
        val themeColor = Color.parseColor("#D32F2F")

        adapter = AppGridAdapter(
            context = this,
            coroutineScope = lifecycleScope,
            themeColor = themeColor,
            iconSizePx = iconSizePx,
            entries = emptyList()
        )
        recyclerView.adapter = adapter

        lifecycleScope.launch(Dispatchers.Default) {
            val pm = packageManager
            val installedApps = pm.getInstalledApplications(0)
                .sortedBy { it.loadLabel(pm).toString() }
            val sample = if (installedApps.size >= 9) installedApps.take(9) else installedApps
            val entries = sample.map {
                AppGridAdapter.AppGridEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.packageName
                )
            }
            withContext(Dispatchers.Main) {
                adapter.submitList(entries)
            }
        }
    }
}

