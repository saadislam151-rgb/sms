package com.mfslogger.app

import android.content.Context

/**
 * Thin wrapper around SharedPreferences for the two things this app
 * needs to remember: the Apps Script Web App URL + shared secret, and
 * the list of SMS sender IDs/numbers to watch (e.g. "bKash", "Nagad",
 * "16216", or a specific phone number).
 */
object Prefs {
    private const val FILE = "mfs_logger_prefs"
    private const val KEY_SCRIPT_URL = "script_url"
    private const val KEY_SECRET = "shared_secret"
    private const val KEY_SENDERS = "watched_senders"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getScriptUrl(ctx: Context): String = prefs(ctx).getString(KEY_SCRIPT_URL, "") ?: ""
    fun setScriptUrl(ctx: Context, url: String) = prefs(ctx).edit().putString(KEY_SCRIPT_URL, url).apply()

    fun getSecret(ctx: Context): String = prefs(ctx).getString(KEY_SECRET, "") ?: ""
    fun setSecret(ctx: Context, secret: String) = prefs(ctx).edit().putString(KEY_SECRET, secret).apply()

    fun getWatchedSenders(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_SENDERS, emptySet()) ?: emptySet()

    fun setWatchedSenders(ctx: Context, senders: Set<String>) =
        prefs(ctx).edit().putStringSet(KEY_SENDERS, senders).apply()

    fun addWatchedSender(ctx: Context, sender: String) {
        val current = getWatchedSenders(ctx).toMutableSet()
        current.add(sender.trim())
        setWatchedSenders(ctx, current)
    }

    fun removeWatchedSender(ctx: Context, sender: String) {
        val current = getWatchedSenders(ctx).toMutableSet()
        current.remove(sender)
        setWatchedSenders(ctx, current)
    }

    /** True if `senderId` (the SMS "from" field) matches one of the watched entries. */
    fun isWatched(ctx: Context, senderId: String): Boolean {
        val watched = getWatchedSenders(ctx)
        if (watched.isEmpty()) return false
        val normalizedIncoming = senderId.trim().lowercase()
        return watched.any { w ->
            val nw = w.trim().lowercase()
            normalizedIncoming == nw || normalizedIncoming.contains(nw) || nw.contains(normalizedIncoming)
        }
    }
}
