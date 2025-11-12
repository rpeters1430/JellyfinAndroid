package com.rpeters.jellyfin.ui.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.rpeters.jellyfin.MainActivity
import com.rpeters.jellyfin.R
import com.rpeters.jellyfin.ui.navigation.Screen
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Helper responsible for publishing dynamic launcher shortcuts that mirror the user's
 * "Continue Watching" queue. The shortcuts deep link into the correct detail screen so
 * playback can resume with a single tap from the launcher.
 */
object DynamicShortcutManager {

    private const val CONTINUE_PREFIX = "continue_watching_"

    fun updateContinueWatchingShortcuts(context: Context, items: List<BaseItemDto>) {
        val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        if (maxShortcuts <= 0) return

        val shortcuts = items
            .filter { it.id != null && !it.name.isNullOrBlank() }
            .take(maxShortcuts.coerceAtMost(4))
            .mapNotNull { item ->
                buildShortcut(context, item)
            }

        val existingDynamic = ShortcutManagerCompat.getDynamicShortcuts(context)
            .map { it.id }
            .filter { it.startsWith(CONTINUE_PREFIX) }

        if (shortcuts.isEmpty()) {
            if (existingDynamic.isNotEmpty()) {
                ShortcutManagerCompat.removeDynamicShortcuts(context, existingDynamic)
            }
            return
        }

        val newIds = shortcuts.map { it.id }.toSet()
        val toRemove = existingDynamic.filterNot { it in newIds }
        if (toRemove.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, toRemove)
        }

        ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
    }

    private fun buildShortcut(context: Context, item: BaseItemDto): ShortcutInfoCompat? {
        val id = item.id?.toString() ?: return null
        val route = when (item.type) {
            BaseItemKind.MOVIE -> Screen.MovieDetail.createRoute(id)
            BaseItemKind.EPISODE -> Screen.TVEpisodeDetail.createRoute(id)
            else -> Screen.ItemDetail.createRoute(id)
        }

        val shortLabel = item.name?.take(24) ?: return null
        val longLabel = when (item.type) {
            BaseItemKind.EPISODE ->
                item.seriesName ?: context.getString(R.string.shortcut_continue_watching_long)

            else -> context.getString(R.string.shortcut_continue_watching_long)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("destination", route)
        }

        return ShortcutInfoCompat.Builder(context, "$CONTINUE_PREFIX$id")
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_continue))
            .setIntent(intent)
            .build()
    }
}
