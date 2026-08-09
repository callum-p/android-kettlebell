package com.kettlebell.app

import com.kettlebell.app.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun versionCodeOf_matchesReleaseWorkflowEncoding() {
        assertEquals(10300, UpdateChecker.versionCodeOf("1.3"))
        assertEquals(10400, UpdateChecker.versionCodeOf("1.4"))
        assertEquals(10402, UpdateChecker.versionCodeOf("1.4.2"))
        assertEquals(20000, UpdateChecker.versionCodeOf("2.0"))
    }

    @Test
    fun versionCodeOf_isMonotonicAcrossVersions() {
        val ordered = listOf("1.3", "1.4", "1.4.1", "1.5", "2.0")
            .map { UpdateChecker.versionCodeOf(it) }
        assertEquals(ordered.sorted(), ordered)
    }
}
