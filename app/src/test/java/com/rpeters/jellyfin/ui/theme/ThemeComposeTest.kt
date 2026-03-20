package com.rpeters.jellyfin.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rpeters.jellyfin.TestCinefinApplication
import com.rpeters.jellyfin.data.preferences.ThemePreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestCinefinApplication::class)
class ThemeComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testThemeRendering() {
        composeTestRule.setContent {
            JellyfinAndroidTheme(themePreferences = ThemePreferences.DEFAULT) {
                ExpressiveThemeProbe()
            }
        }

        // If it reaches here without NoSuchMethodError, it's likely fixed or not present in this config
    }

    @Composable
    private fun ExpressiveThemeProbe() {
        val sectionColor = JellyfinExpressiveTheme.colors.sectionContainer
        Text(text = "Hello Compose", color = sectionColor)
    }
}
