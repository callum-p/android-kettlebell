package com.kettlebell.app.ui.whatsnew

import com.kettlebell.app.BuildConfig

/** One version's release notes. */
data class VersionNotes(val version: String, val entries: List<String>)

/**
 * The full release-note history, parsed from the whole CHANGELOG.md baked into
 * [BuildConfig.CHANGELOG_FULL] at build time. Used by the Settings "Release notes" screen.
 */
object ReleaseNotes {

    val versions: List<VersionNotes> = parse(BuildConfig.CHANGELOG_FULL)

    /** Splits raw changelog text into per-version sections (headers look like "## 1.3"). */
    fun parse(raw: String): List<VersionNotes> {
        val result = mutableListOf<VersionNotes>()
        var version: String? = null
        val buffer = StringBuilder()

        fun flush() {
            val v = version ?: return
            result += VersionNotes(v, WhatsNew.parseEntries(buffer.toString()))
            buffer.clear()
        }

        for (line in raw.split("\n")) {
            if (line.startsWith("## ")) {
                flush()
                version = line.removePrefix("## ").trim()
            } else {
                buffer.append(line).append('\n')
            }
        }
        flush()
        return result
    }
}
