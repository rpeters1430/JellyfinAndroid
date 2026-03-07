package com.rpeters.jellyfin.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rpeters.jellyfin.data.preferences.LibraryActionsPreferences
import com.rpeters.jellyfin.ui.theme.JellyfinAndroidTheme
import com.rpeters.jellyfin.ui.viewmodel.LibraryActionsPreferencesViewModel
import com.rpeters.jellyfin.ui.viewmodel.RemoteConfigViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule(
        effectContext = StandardTestDispatcher(),
    )

    private lateinit var libraryPreferencesFlow: MutableStateFlow<LibraryActionsPreferences>
    private lateinit var libraryViewModel: LibraryActionsPreferencesViewModel
    private lateinit var remoteConfigViewModel: RemoteConfigViewModel
    private var managePinsClicked = false

    @Before
    fun setup() {
        libraryPreferencesFlow = MutableStateFlow(LibraryActionsPreferences.DEFAULT)
        libraryViewModel = mockk(relaxed = true)
        remoteConfigViewModel = mockk(relaxed = true)

        every { libraryViewModel.preferences } returns libraryPreferencesFlow
        every { remoteConfigViewModel.getBoolean(any()) } returns true

        coEvery { libraryViewModel.setManagementActionsEnabled(any()) } returns Unit

        managePinsClicked = false
    }

    @Test
    fun `pinning management card is shown and actionable`() {
        composeTestRule.setContent {
            JellyfinAndroidTheme {
                SettingsScreen(
                    onBackClick = {},
                    onManagePinsClick = { managePinsClicked = true },
                    libraryActionsPreferencesViewModel = libraryViewModel,
                    remoteConfigViewModel = remoteConfigViewModel,
                )
            }
        }

        composeTestRule.onNodeWithText("Certificate pins").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage pins").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage pins").performClick()

        assertTrue(managePinsClicked)
    }
}
