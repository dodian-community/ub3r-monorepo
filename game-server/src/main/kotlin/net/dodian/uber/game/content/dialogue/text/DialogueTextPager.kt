package net.dodian.uber.game.content.dialogue.text

/**
 * Word-wrap + pagination for dialogue/chatbox text.
 *
 * Rules:
 * - 56 visible characters per line (formatting tags don't count).
 * - 4 text lines per page (client only supports 1–4 text lines per dialogue).
 * - Hard breaks via '\n' are preserved.
 * - Formatting tags ignored for length:
 *   - <...>
 *   - @xxx@ / @xxxx@ (and other short @...@ codes)
 */
object DialogueTextPager {
    const val MAX_INPUT_CHARS: Int = 10_000
    const val MAX_VISIBLE_CHARS_PER_LINE: Int = 56
    const val MAX_TEXT_LINES_PER_PAGE: Int = 4

    fun paginate(text: String): List<List<String>> {
        val lines = wrap(text)
        if (lines.isEmpty()) return listOf(listOf(""))
        return lines.chunked(MAX_TEXT_LINES_PER_PAGE)
    }

    fun wrap(text: String): List<String> {
        val normalized = normalizeInput(text)
        if (normalized.isEmpty()) return listOf("")

        val hardLines = normalized.split('\n')
        val wrapped = ArrayList<String>()
        for (hardLine in hardLines) {
            val line = normalizeSpaces(hardLine)
            if (line.isEmpty()) {
                wrapped.add("")
                continue
            }
            wrapped.addAll(wrapHardLine(line))
        }
        return wrapped
    }

    private fun normalizeInput(text: String): String {
        if (text.isEmpty()) return ""
        val capped = if (text.length > MAX_INPUT_CHARS) text.substring(0, MAX_INPUT_CHARS) else text
        return capped
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\t', ' ')
    }

    private fun normalizeSpaces(text: String): String {
        if (text.isEmpty()) return ""
        val out = StringBuilder(text.length)
        var prevWasSpace = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == ' ') {
                if (!prevWasSpace) {
                    out.append(' ')
                    prevWasSpace = true
                }
                i++
                continue
            }
            prevWasSpace = false
            out.append(c)
            i++
        }
        return out.toString().trim()
    }

    private fun wrapHardLine(line: String): List<String> {
        val out = ArrayList<String>()
        var remaining = line
        while (remaining.isNotEmpty()) {
            val cut = findWrapCut(remaining)
            if (cut == null) {
                out.add(remaining.trimEnd())
                break
            }

            val rawSegment = remaining.substring(0, cut.cutIndexExclusive)
            val segment = rawSegment.trimEnd()
            out.add(segment)

            var next = remaining.substring(cut.cutIndexExclusive)
            next = next.trimStart()
            if (next.isNotEmpty() && cut.carryPrefix.isNotEmpty()) {
                next = cut.carryPrefix + next
            }
            remaining = next
        }
        return out
    }

    private data class WrapCut(
        val cutIndexExclusive: Int,
        val carryPrefix: String,
    )

    private fun findWrapCut(text: String): WrapCut? {
        var visible = 0
        var i = 0
        var lastWhitespaceIndex: Int? = null
        var lastWhitespaceCarryPrefix: String = ""
        var lastVisibleCharIndexInclusive: Int? = null

        while (i < text.length) {
            val tag = parseTagAt(text, i)
            if (tag != null) {
                i = tag.endExclusive
                continue
            }

            val c = text[i]
            visible += 1
            if (c == ' ') {
                lastWhitespaceIndex = i
                lastWhitespaceCarryPrefix = activeCarryPrefix(text.substring(0, i))
            }
            lastVisibleCharIndexInclusive = i

            if (visible >= MAX_VISIBLE_CHARS_PER_LINE) {
                break
            }
            i++
        }

        if (visible < MAX_VISIBLE_CHARS_PER_LINE) {
            return null
        }

        val whitespaceCut = lastWhitespaceIndex
        if (whitespaceCut != null && whitespaceCut > 0) {
            return WrapCut(
                cutIndexExclusive = whitespaceCut,
                carryPrefix = lastWhitespaceCarryPrefix,
            )
        }

        val hardCutInclusive = lastVisibleCharIndexInclusive ?: return null
        val hardCutExclusive = hardCutInclusive + 1
        val carry = activeCarryPrefix(text.substring(0, hardCutExclusive))
        return WrapCut(
            cutIndexExclusive = hardCutExclusive,
            carryPrefix = carry,
        )
    }

    private data class TagSpan(
        val endExclusive: Int,
    )

    private fun parseTagAt(text: String, index: Int): TagSpan? {
        val c = text[index]
        if (c == '<') {
            val close = text.indexOf('>', startIndex = index + 1)
            if (close == -1) return null
            return TagSpan(endExclusive = close + 1)
        }
        if (c == '@') {
            val close = text.indexOf('@', startIndex = index + 1)
            if (close == -1) return null
            val len = close - index + 1
            if (len < 3 || len > 12) return null
            val inner = text.substring(index + 1, close)
            if (inner.any { !it.isLetterOrDigit() }) return null
            return TagSpan(endExclusive = close + 1)
        }
        return null
    }

    private fun activeCarryPrefix(segment: String): String {
        if (segment.isEmpty()) return ""

        var lastAtCode: String? = null
        val openStyleTags = ArrayDeque<String>()
        val openStyleTagNames = ArrayDeque<String>()

        var i = 0
        while (i < segment.length) {
            val c = segment[i]
            if (c == '<') {
                val close = segment.indexOf('>', startIndex = i + 1)
                if (close == -1) break
                val full = segment.substring(i, close + 1)
                val inner = segment.substring(i + 1, close).trim()
                if (inner.startsWith("/")) {
                    val name = inner.drop(1).takeWhile { it.isLetterOrDigit() }.lowercase()
                    if (openStyleTagNames.isNotEmpty() && openStyleTagNames.last() == name) {
                        openStyleTagNames.removeLast()
                        openStyleTags.removeLast()
                    }
                } else {
                    val name = inner.takeWhile { it.isLetterOrDigit() }.lowercase()
                    if (name in STYLE_TAGS && !inner.endsWith("/")) {
                        openStyleTagNames.addLast(name)
                        openStyleTags.addLast(full)
                    }
                }
                i = close + 1
                continue
            }
            if (c == '@') {
                val close = segment.indexOf('@', startIndex = i + 1)
                if (close != -1) {
                    val len = close - i + 1
                    if (len in 3..12) {
                        val inner = segment.substring(i + 1, close)
                        if (inner.isNotEmpty() && inner.all { it.isLetterOrDigit() }) {
                            lastAtCode = segment.substring(i, close + 1)
                            i = close + 1
                            continue
                        }
                    }
                }
            }
            i++
        }

        val sb = StringBuilder()
        if (lastAtCode != null) sb.append(lastAtCode)
        for (tag in openStyleTags) sb.append(tag)
        return sb.toString()
    }

    private val STYLE_TAGS: Set<String> = setOf("col", "shad", "trans", "u", "str")
}
