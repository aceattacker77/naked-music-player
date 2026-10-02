package io.github.aceattacker77.nakedmusicplayer.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

object WidgetUpdater {
    /**
     * Whether [new] needs to reach the widget. Playback position ticks constantly but the widget only
     * shows a coarse bar, so progress-only changes never trigger a redraw: updates happen on events.
     */
    fun shouldUpdate(old: WidgetState?, new: WidgetState): Boolean =
        old == null || old.copy(progress = 0f) != new.copy(progress = 0f)

    /** Saves [state] into every placed widget and redraws them. */
    suspend fun push(context: Context, state: WidgetState) {
        val widget = PlayerWidget()
        GlanceAppWidgetManager(context).getGlanceIds(PlayerWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { prefs -> state.writeTo(prefs) }
            widget.update(context, id)
        }
    }
}
