package com.talauncher

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talauncher.R
import com.talauncher.testutils.TestIconGridActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconGridInstrumentedTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(TestIconGridActivity::class.java)

    @Test
    fun themedIconsRenderInRecyclerView() {
        onView(withId(R.id.test_icon_grid)).check(matches(isDisplayed()))

        scenarioRule.scenario.onActivity { activity ->
            val recycler = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.test_icon_grid)
            requireNotNull(recycler.adapter) { "Adapter should be attached" }
            assertTrue("Adapter should report item count", recycler.adapter!!.itemCount > 0)
        }
    }
}

