package com.kettlebell.app

import com.kettlebell.app.ui.whatsnew.ReleaseNotes
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {

    @Test
    fun parsesMultipleVersionsInOrder() {
        val raw = """
            ## 1.3
            - Fixed a bug
            - Added a thing

            ## 1.2
            - Older change
        """.trimIndent()

        val versions = ReleaseNotes.parse(raw)
        assertEquals(2, versions.size)
        assertEquals("1.3", versions[0].version)
        assertEquals(listOf("Fixed a bug", "Added a thing"), versions[0].entries)
        assertEquals("1.2", versions[1].version)
        assertEquals(listOf("Older change"), versions[1].entries)
    }

    @Test
    fun keepsNonBulletIntroLineForAVersion() {
        val raw = """
            ## 0.0.1
            First release.
            - A feature
        """.trimIndent()

        val versions = ReleaseNotes.parse(raw)
        assertEquals(1, versions.size)
        assertEquals(listOf("First release.", "A feature"), versions[0].entries)
    }

    @Test
    fun emptyInput_yieldsNoVersions() {
        assertEquals(emptyList<Any>(), ReleaseNotes.parse(""))
    }
}
