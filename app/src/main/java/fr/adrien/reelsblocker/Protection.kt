package fr.adrien.reelsblocker

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** The protection's overall state, shared by the main screen, the Quick Settings tile and the watchdog. */
object Protection {

    enum class State { SERVICE_OFF, SWITCHED_OFF, PAUSED, ACTIVE }

    const val PAUSE_MINUTES = 5

    fun state(c: Context): State = when {
        !isServiceEnabled(c) -> State.SERVICE_OFF
        !Prefs.enabled(c) -> State.SWITCHED_OFF
        Prefs.isPaused(c) -> State.PAUSED
        else -> State.ACTIVE
    }

    fun pause(c: Context) {
        Prefs.setPausedUntil(c, System.currentTimeMillis() + PAUSE_MINUTES * 60_000L)
        Prefs.logEvent(c, R.string.log_paused, PAUSE_MINUTES)
        PauseTile.refresh(c)
    }

    fun resume(c: Context) {
        if (!Prefs.isPaused(c)) return
        Prefs.setPausedUntil(c, 0L)
        Prefs.logEvent(c, R.string.log_resumed)
        PauseTile.refresh(c)
    }

    /** "14:37" in the device's time format. */
    fun pausedUntilLabel(c: Context): String =
        android.text.format.DateFormat.getTimeFormat(c).format(java.util.Date(Prefs.pausedUntil(c)))

    fun isServiceEnabled(c: Context): Boolean {
        val me = ComponentName(c, ReelsBlockerService::class.java)
        val enabled = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        // Android stores either the long or short form ("pkg/.Class") depending on the version: compare components.
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
