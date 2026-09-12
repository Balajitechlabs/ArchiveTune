/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.lyrics

import java.io.File
import java.util.regex.Pattern

data class ParsedLrcLine(
    val timestampMs: Long,
    val text: String,
)

object LocalLyricsParser {

    private val LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    fun parse(lrcContent: String): List<ParsedLrcLine> {
        val lines = mutableListOf<ParsedLrcLine>()
        lrcContent.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val matcher = LRC_PATTERN.matcher(trimmed)
            if (matcher.matches()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val fractionStr = matcher.group(3) ?: "0"
                val fractionMs = if (fractionStr.length == 2) {
                    (fractionStr.toLongOrNull() ?: 0L) * 10L
                } else {
                    fractionStr.toLongOrNull() ?: 0L
                }

                val totalMs = (minutes * 60L + seconds) * 1000L + fractionMs
                val text = matcher.group(4)?.trim().orEmpty()
                lines.add(ParsedLrcLine(totalMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    fun findLocalLrcForAudio(audioFile: File): File? {
        val parent = audioFile.parentFile ?: return null
        val baseName = audioFile.nameWithoutExtension
        val potentialLrc = File(parent, "$baseName.lrc")
        return if (potentialLrc.exists() && potentialLrc.isFile) potentialLrc else null
    }
}
