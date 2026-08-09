package com.kettlebell.app

import com.kettlebell.app.ui.whatsnew.WhatsNew
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewParserTest {

    @Test
    fun parsesSimpleBullets() {
        assertEquals(listOf("First", "Second"), WhatsNew.parseEntries("- First\n- Second"))
    }

    @Test
    fun ignoresBlankLines() {
        assertEquals(listOf("Only"), WhatsNew.parseEntries("\n- Only\n\n"))
    }

    @Test
    fun foldsWrappedContinuationLines() {
        val raw = "- A long line\n  that wraps\n- Second"
        assertEquals(listOf("A long line that wraps", "Second"), WhatsNew.parseEntries(raw))
    }

    @Test
    fun keepsLeadingNonBulletLine() {
        assertEquals(listOf("Intro text", "Bullet"), WhatsNew.parseEntries("Intro text\n- Bullet"))
    }

    @Test
    fun emptyInput_yieldsNoEntries() {
        assertEquals(emptyList<String>(), WhatsNew.parseEntries(""))
    }
}
