package com.kettlebell.app.ui.whatsnew

import android.content.Context
import com.kettlebell.app.BuildConfig

/**
 * Decides when to show the "What's new" modal. The changelog text itself is computed at build
 * time and injected via [BuildConfig.CHANGELOG]; here we only track whether the user has already
 * seen the notes for the currently-installed version.
 */
object WhatsNew {

    private const val PREFS = "whats_new"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version_code"

    /** The changelog for this build, split into display lines (leading "- " bullets stripped). */
    val entries: List<String> = BuildConfig.CHANGELOG
        .split("\n")
        .map { it.trim().removePrefix("- ").trim() }
        .filter { it.isNotBlank() }

    /**
     * True when the app has just been updated to a newer version than the user last saw, and there
     * are notes to show. Returns false on a fresh install so first-time users aren't interrupted.
     */
    fun shouldShow(context: Context): Boolean {
        if (entries.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = prefs.getInt(KEY_LAST_SEEN_VERSION, 0)
        return lastSeen in 1 until BuildConfig.VERSION_CODE
    }

    /** Records that the user has seen the notes for the current version. */
    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_LAST_SEEN_VERSION, BuildConfig.VERSION_CODE)
            .apply()
    }
}
