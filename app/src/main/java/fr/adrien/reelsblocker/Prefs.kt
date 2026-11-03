package fr.adrien.reelsblocker

import android.content.Context

object Prefs {
    private const val FILE = "reels_blocker"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun blockDmSwipe(c: Context) = sp(c).getBoolean("block_dm_swipe", true)
    fun setBlockDmSwipe(c: Context, v: Boolean) = sp(c).edit().putBoolean("block_dm_swipe", v).apply()

    fun debug(c: Context) = sp(c).getBoolean("debug", false)
    fun setDebug(c: Context, v: Boolean) = sp(c).edit().putBoolean("debug", v).apply()

    fun debugDump(c: Context, screen: String) = sp(c).getString("dump_$screen", "") ?: ""
    fun saveDebug(c: Context, screen: String, v: String) = sp(c).edit().putString("dump_$screen", v).apply()

    /** Log of the last 15 decisions, newest first. */
    fun log(c: Context) = sp(c).getString("log", "") ?: ""
    fun appendLog(c: Context, line: String) {
        val lines = (listOf(line) + log(c).lines().filter { it.isNotBlank() }).take(15)
        sp(c).edit().putString("log", lines.joinToString("\n")).apply()
    }

    fun diag(c: Context, key: String) = sp(c).getString("diag_$key", "") ?: ""
    fun setDiag(c: Context, key: String, v: String) = sp(c).edit().putString("diag_$key", v).apply()

    fun lastSeenCount(c: Context) = sp(c).getInt("last_seen", 0)
    fun setLastSeenCount(c: Context, v: Int) = sp(c).edit().putInt("last_seen", v).apply()

    fun blockedCount(c: Context) = sp(c).getInt("blocked", 0)
    fun incrementBlocked(c: Context) = sp(c).edit().putInt("blocked", blockedCount(c) + 1).apply()
}
