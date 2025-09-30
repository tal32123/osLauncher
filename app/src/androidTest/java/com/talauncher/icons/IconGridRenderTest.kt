package com.talauncher.icons

import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconGridRenderTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun themedIconsRenderInGrid() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0)
            .takeIf { it.isNotEmpty() }
            ?.take(12)
            ?.map { appInfo ->
                ThemedAppGridItem(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString()
                )
            }
            ?: run {
                val fallbackLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(context.packageName, 0)).toString()
                }.getOrDefault(context.packageName)
                listOf(ThemedAppGridItem(context.packageName, fallbackLabel))
            }

        scenarioRule.scenario.onActivity { activity ->
            val recyclerView = RecyclerView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
            }
            recyclerView.useThemedGrid(columns = 4)
            val themeColor = Color.parseColor("#FF3B30")
            val adapter = ThemedAppAdapter(activity, themeColor, (64 * activity.resources.displayMetrics.density).toInt().coerceAtLeast(96))
            recyclerView.adapter = adapter
            adapter.submitList(installed)
            activity.setContentView(recyclerView)
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenarioRule.scenario.onActivity { activity ->
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            val recycler = root.getChildAt(0) as RecyclerView
            assertTrue("RecyclerView should have themed content", recycler.childCount > 0)
            val firstHolder = recycler.findViewHolderForAdapterPosition(0) as? ThemedAppAdapter.ViewHolder
            assertTrue("First holder should contain an icon", firstHolder?.iconView?.drawable != null)
        }
    }
}

