package com.rpeters.jellyfin.ui.tv

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvKeyboardHandlerConfigTest {

    @Test
    fun controllerManager_respectsDpadToggle() {
        val manager = TvControllerManager()
        manager.updateConfig(
            TvControllerConfig(
                enableDpadNavigation = false,
                enableMediaKeys = true,
                enableQuickAccess = true,
            ),
        )

        assertFalse(manager.isKeyEnabled(Key.DirectionUp))
        assertFalse(manager.isKeyEnabled(Key.DirectionRight))
        assertTrue(manager.isKeyEnabled(Key.MediaPlay))
    }

    @Test
    fun controllerManager_respectsMediaAndQuickAccessToggles() {
        val manager = TvControllerManager()
        manager.updateConfig(
            TvControllerConfig(
                enableDpadNavigation = true,
                enableMediaKeys = false,
                enableQuickAccess = false,
            ),
        )

        assertFalse(manager.isKeyEnabled(Key.MediaPlay))
        assertFalse(manager.isKeyEnabled(Key.MediaPause))
        assertFalse(manager.isKeyEnabled(Key.One))
        assertFalse(manager.isKeyEnabled(Key.Five))
        assertTrue(manager.isKeyEnabled(Key.Search))
    }

    @Test
    fun keyBindings_returnsContextualHelpForEveryScreenType() {
        assertTrue(TvKeyBindings.getContextualHelp(TvScreenType.HOME).contains("D-pad"))
        assertTrue(TvKeyBindings.getContextualHelp(TvScreenType.PLAYER).contains("playback"))
        assertTrue(TvKeyBindings.getContextualHelp(TvScreenType.SEARCH).contains("search"))
    }

    @Test
    fun controllerManager_updateConfigPersistsNewConfig() {
        val manager = TvControllerManager()
        val updated = TvControllerConfig(
            enableDpadNavigation = false,
            enableMediaKeys = false,
            enableQuickAccess = false,
            enableColorKeys = true,
            enableVoiceSearch = false,
        )

        manager.updateConfig(updated)

        assertEquals(updated, manager.getConfig())
    }
}
