package fr.adrien.reelsblocker

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: shows whether protection runs, at a glance.
 * Tapping it resumes a pause right away, but never pauses: pausing keeps its hold-to-confirm
 * friction in the app, so the tile opens the app instead.
 */
class PauseTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (Protection.state(this) == Protection.State.PAUSED) {
            Protection.resume(this)
            render()
        } else {
            openApp()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val state = Protection.state(this)
        tile.label = getString(R.string.app_name)
        tile.state = if (state == Protection.State.ACTIVE) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val sub = when (state) {
            Protection.State.ACTIVE -> getString(R.string.tile_on)
            Protection.State.PAUSED -> getString(R.string.status_paused_until, Protection.pausedUntilLabel(this))
            Protection.State.SWITCHED_OFF -> getString(R.string.status_paused)
            Protection.State.SERVICE_OFF -> getString(R.string.status_off)
        }
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = sub
        tile.contentDescription = "${tile.label}, $sub"
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Asks the system to redraw the tile after a state change made elsewhere. */
        fun refresh(c: Context) {
            runCatching { TileService.requestListeningState(c, ComponentName(c, PauseTile::class.java)) }
        }
    }
}
