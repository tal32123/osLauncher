package com.talauncher

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talauncher.ui.activities.IconGalleryActivity
import com.facebook.testing.screenshot.Screenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconGalleryTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<IconGalleryActivity>()

    @Test
    fun generateIconGallery() {
        val view = composeTestRule.activity.window.decorView
        Screenshot.snap(view).setName("IconGallery").record()
    }
}
