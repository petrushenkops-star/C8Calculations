package com.pavel.c8calculations.recognition

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class TeamRecognitionResult(
    val counts: IntArray,
    val detectedCards: Int,
    val leaderExcluded: Boolean,
)

object TeamLevelRecognizer {
    private val levelPattern = Pattern.compile("(?i)[CСCcСс]\\s*([1-6])")
    private val digitPattern = Pattern.compile("[1-6]")

    fun recognize(source: Bitmap, onResult: (TeamRecognitionResult?) -> Unit) {
        val matches = mutableListOf<LevelMatch>()
        val scaledWidth = min(source.width * 2, 3000)
        val scale = scaledWidth.toFloat() / source.width
        val enlarged = Bitmap.createScaledBitmap(source, scaledWidth, (source.height * scale).roundToInt(), true)
        val passes = listOf(Pass(source, 1f), Pass(enlarged, scale))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val remaining = AtomicInteger(passes.size)
        val lock = Any()

        passes.forEach { pass ->
            recognizer.process(InputImage.fromBitmap(pass.bitmap, 0))
                .addOnSuccessListener { text ->
                    synchronized(lock) { collect(text, pass.scale, matches) }
                }
                .addOnCompleteListener {
                    if (remaining.decrementAndGet() == 0) {
                        recognizer.close()
                        val result = synchronized(lock) { buildResult(matches) }
                        onResult(result)
                    }
                }
        }
    }

    private fun collect(text: Text, scale: Float, matches: MutableList<LevelMatch>) {
        text.textBlocks.forEach { block -> block.lines.forEach { line ->
            val elements = line.elements
            elements.forEachIndexed { index, element ->
                val box = element.boundingBox ?: return@forEachIndexed
                val raw = element.text.trim()
                val matcher = levelPattern.matcher(raw)
                while (matcher.find()) addMatch(matcher.group(1)!!.toInt(), box, scale, matches)

                if (raw.matches(Regex("(?i)[CСCcСс]"))) {
                    for (k in index + 1 until min(elements.size, index + 3)) {
                        val next = elements[k]
                        val nextBox = next.boundingBox ?: continue
                        val digit = next.text.trim()
                        if (digitPattern.matcher(digit).matches() &&
                            abs(nextBox.centerY() - box.centerY()) < max(35f * scale, box.height().toFloat())) {
                            val pair = Rect(box).apply { union(nextBox) }
                            addMatch(digit.toInt(), pair, scale, matches)
                            break
                        }
                    }
                }
            }
        }}
    }

    private fun addMatch(level: Int, box: Rect, scale: Float, matches: MutableList<LevelMatch>) {
        val x = (box.centerX() / scale).roundToInt()
        val y = (box.centerY() / scale).roundToInt()
        if (matches.any { it.level == level && abs(it.x - x) <= 28 && abs(it.y - y) <= 18 }) return
        matches += LevelMatch(level, x, y)
    }

    private fun buildResult(raw: List<LevelMatch>): TeamRecognitionResult? {
        if (raw.isEmpty()) return null
        val matches = raw.sortedWith(compareBy<LevelMatch> { it.x }.thenBy { it.y })
        val minX = matches.minOf { it.x }
        val leftColumn = matches.filter { abs(it.x - minX) <= 35 }
        // A leader exists only when the leftmost column contains exactly one card.
        val leader = if (leftColumn.size == 1) leftColumn.first() else null
        val counts = IntArray(7)
        matches.forEach { if (it !== leader) counts[it.level]++ }
        return TeamRecognitionResult(counts, matches.size, leader != null)
    }

    private data class Pass(val bitmap: Bitmap, val scale: Float)
    private data class LevelMatch(val level: Int, val x: Int, val y: Int)
}
