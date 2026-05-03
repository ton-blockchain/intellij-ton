package org.ton.intellij.acton.ide.formatter

import com.intellij.openapi.util.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActonFmtFormattingServiceTest {
    @Test
    fun `test range argument uses zero based utf8 byte columns`() {
        val text = "fun main() {\n  я()\n}\n"
        val start = text.indexOf("я")
        val end = start + "я".length

        val range = ActonFmtFormattingService.createActonFmtRangeArgument(text, listOf(TextRange(start, end)))

        assertEquals("1:2-1:4", range)
    }

    @Test
    fun `test range argument merges multiple formatting ranges`() {
        val text = "a\nbb\nccc\n"

        val range = ActonFmtFormattingService.createActonFmtRangeArgument(
            text,
            listOf(
                TextRange(2, 4),
                TextRange(5, 7),
            ),
        )

        assertEquals("1:0-2:2", range)
    }

    @Test
    fun `test range argument is omitted for whole document formatting`() {
        val text = "fun main() {}\n"

        val range = ActonFmtFormattingService.createActonFmtRangeArgument(text, listOf(TextRange(0, text.length)))

        assertNull(range)
    }

    @Test
    fun `test range argument is omitted without non empty ranges`() {
        val text = "fun main() {}\n"

        val range = ActonFmtFormattingService.createActonFmtRangeArgument(text, listOf(TextRange(0, 0)))

        assertNull(range)
    }
}
