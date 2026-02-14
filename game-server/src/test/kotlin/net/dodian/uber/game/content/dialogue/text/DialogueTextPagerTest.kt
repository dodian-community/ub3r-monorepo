package net.dodian.uber.game.content.dialogue.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueTextPagerTest {

    @Test
    fun `wrap respects max visible chars and avoids mid-word`() {
        val text = "This is a sentence that should wrap nicely without splitting words."
        val lines = DialogueTextPager.wrap(text)
        assertTrue(lines.all { visibleLength(it) <= DialogueTextPager.MAX_VISIBLE_CHARS_PER_LINE })
        assertTrue(lines.none { it.endsWith(" ") })
    }

    @Test
    fun `paginate uses max 4 lines per page`() {
        val text = (1..30).joinToString(" ") { "word$it" }
        val pages = DialogueTextPager.paginate(text)
        assertTrue(pages.all { it.size in 1..DialogueTextPager.MAX_TEXT_LINES_PER_PAGE })
    }

    @Test
    fun `explicit newlines are preserved as hard breaks`() {
        val text = "Line one.\nLine two.\nLine three."
        val lines = DialogueTextPager.wrap(text)
        assertTrue(lines.contains("Line one."))
        assertTrue(lines.contains("Line two."))
        assertTrue(lines.contains("Line three."))
    }

    @Test
    fun `formatting tags do not count toward visible length`() {
        val text = "<col=ff0000>@red@Hello world@bla@</col>"
        val lines = DialogueTextPager.wrap(text)
        assertEquals(1, lines.size)
        assertEquals(11, visibleLength(lines[0])) // "Hello world"
    }

    @Test
    fun `overlong tokens are split`() {
        val token = "a".repeat(DialogueTextPager.MAX_VISIBLE_CHARS_PER_LINE + 10)
        val lines = DialogueTextPager.wrap(token)
        assertTrue(lines.size >= 2)
        assertTrue(lines.all { visibleLength(it) <= DialogueTextPager.MAX_VISIBLE_CHARS_PER_LINE })
    }

    @Test
    fun `max input size is capped`() {
        val text = "x".repeat(DialogueTextPager.MAX_INPUT_CHARS + 500)
        val lines = DialogueTextPager.wrap(text)
        val totalVisible = lines.sumOf { visibleLength(it) }
        assertTrue(totalVisible <= DialogueTextPager.MAX_INPUT_CHARS)
    }

    private fun visibleLength(s: String): Int {
        var visible = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '<') {
                val close = s.indexOf('>', startIndex = i + 1)
                if (close == -1) break
                i = close + 1
                continue
            }
            if (c == '@') {
                val close = s.indexOf('@', startIndex = i + 1)
                if (close != -1) {
                    val inner = s.substring(i + 1, close)
                    val len = close - i + 1
                    if (len in 3..12 && inner.isNotEmpty() && inner.all { it.isLetterOrDigit() }) {
                        i = close + 1
                        continue
                    }
                }
            }
            visible++
            i++
        }
        return visible
    }
}

