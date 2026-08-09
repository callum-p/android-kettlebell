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

    /**
     * The changelog for this build as display bullets. Lines starting with "- " begin a new
     * bullet; any following non-bullet lines are folded into it (so a wrapped line never shows
     * up as its own stray bullet).
     */
    val entries: List<String> = buildList {
        for (raw in BuildConfig.CHANGELOG.split("\n")) {
            val line = raw.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("- ") -> add(line.removePrefix("- ").trim())
                isNotEmpty() -> set(lastIndex, "${this[lastIndex]} $line")
                else -> add(line)
            }
        }
    }

    /**
     * True when there are notes to show and the user hasn't seen this version's notes yet. Also
     * shows on a first install (last seen defaults to 0), which doubles as a welcome — we can't
     * reliably tell a fresh install apart from an update off a version that never tracked this.
     */
    fun shouldShow(context: Context): Boolean {
        if (entries.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = prefs.getInt(KEY_LAST_SEEN_VERSION, 0)
        return lastSeen < BuildConfig.VERSION_CODE
    }

    /** Records that the user has seen the notes for the current version. */
    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_LAST_SEEN_VERSION, BuildConfig.VERSION_CODE)
            .apply()
    }
}
