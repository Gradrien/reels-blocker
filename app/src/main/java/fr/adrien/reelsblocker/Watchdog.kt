package fr.adrien.reelsblocker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Periodic check (every few hours, framework JobScheduler, no network) that the accessibility
 * service is still enabled and bound. Some phones (Xiaomi in particular) silently kill it or turn
 * it off after an update; in that case a single notification offers to turn it back on.
 */
class Watchdog : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        check(this)
        return false
    }

    override fun onStopJob(params: JobParameters?) = false

    companion object {
        private const val JOB_ID = 4201
        private const val CHANNEL = "protection"
        private const val NOTIFICATION_ID = 1
        private const val PERIOD_MS = 6 * 60 * 60 * 1000L
        private const val MIN_ALERT_GAP_MS = 3 * 24 * 60 * 60 * 1000L

        /** Idempotent: schedules the periodic check once setup has been done. */
        fun schedule(c: Context) {
            val js = c.getSystemService(JobScheduler::class.java) ?: return
            if (js.getPendingJob(JOB_ID) != null) return
            js.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(c, Watchdog::class.java))
                    .setPeriodic(PERIOD_MS)
                    .setPersisted(true)
                    .build()
            )
        }

        fun check(c: Context) {
            if (!Prefs.onboarded(c)) return
            val stopped = !Protection.isServiceEnabled(c) || !Prefs.serviceRunning(c)
            if (!stopped) return
            val now = System.currentTimeMillis()
            if (now - Prefs.lastAlertAt(c) < MIN_ALERT_GAP_MS) return
            if (notify(c)) Prefs.setLastAlertAt(c, now)
        }

        fun cancelAlert(c: Context) {
            c.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }

        private fun notify(c: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val nm = c.getSystemService(NotificationManager::class.java) ?: return false
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, c.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE
            )
            nm.notify(
                NOTIFICATION_ID,
                Notification.Builder(c, CHANNEL)
                    .setSmallIcon(R.drawable.ic_tile)
                    .setContentTitle(c.getString(R.string.notif_stopped_title))
                    .setContentText(c.getString(R.string.notif_stopped_text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
            )
            return true
        }
    }
}
