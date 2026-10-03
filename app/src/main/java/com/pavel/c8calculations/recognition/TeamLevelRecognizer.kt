package com.pavel.c8calculations.recognition

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Color
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

data class TeamDetectedCard(
    val level: Int,
    val x: Int,
    val y: Int,
    val excludedAsLeader: Boolean,
)

data class TeamRecognitionResult(
    val counts: IntArray,
    val detectedCards: Int,
    val leaderExcluded: Boolean,
    val cards: List<TeamDetectedCard>,
)

object TeamLevelRecognizer {
    private val levelPattern = Pattern.compile("(?i)[CСCcСс]\\s*([0-6])")
    private val c4AsLetterPattern = Pattern.compile("(?i)^\\s*[CСCcСс]\\s*[AАaа](?=\\s|$)")
    private val digitPattern = Pattern.compile("[0-6]")
    private const val COLUMN_TOLERANCE_PX = 45

    fun recognize(source: Bitmap, onResult: (TeamRecognitionResult?) -> Unit) {
        val matches = mutableListOf<LevelMatch>()
        val scaledWidth = min(source.width * 2, 3000)
        val scale = scaledWidth.toFloat() / source.width
        val enlarged = Bitmap.createScaledBitmap(source, scaledWidth, (source.height * scale).roundToInt(), true)
        val highContrast = toHighContrast(enlarged)
        val passes = listOf(Pass(source, 1f), Pass(enlarged, scale), Pass(highContrast, scale))
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
            var levelFoundInElements = false
            elements.forEachIndexed { index, element ->
                val box = element.boundingBox ?: return@forEachIndexed
                val raw = element.text.trim()
                val matcher = levelPattern.matcher(raw)
                while (matcher.find()) {
                    addMatch(matcher.group(1)!!.toInt(), box, scale, matches)
                    levelFoundInElements = true
                }
                if (!levelFoundInElements && c4AsLetterPattern.matcher(raw).find()) {
                    addMatch(4, box, scale, matches)
                    levelFoundInElements = true
                }

                if (raw.matches(Regex("(?i)[CСCcСс]"))) {
                    for (k in index + 1 until min(elements.size, index + 3)) {
                        val next = elements[k]
                        val nextBox = next.boundingBox ?: continue
                        val digit = next.text.trim()
                        if (digitPattern.matcher(digit).matches() &&
                            abs(nextBox.centerY() - box.centerY()) < max(35f * scale, box.height().toFloat())) {
                            val pair = Rect(box).apply { union(nextBox) }
                            addMatch(digit.toInt(), pair, scale, matches)
                            levelFoundInElements = true
                            break
                        }
                    }
                }
            }

            // ML Kit can merge a level with the rest of a dense card into one OCR line.
            // Fall back to the complete line so C0-C6 is not lost when element segmentation varies.
            if (!levelFoundInElements) {
                val lineText = line.text.trim()
                val lineMatcher = levelPattern.matcher(lineText)
                if (lineMatcher.find()) {
                    line.boundingBox?.let { addMatch(lineMatcher.group(1)!!.toInt(), it, scale, matches) }
                } else if (c4AsLetterPattern.matcher(lineText).find()) {
                    line.boundingBox?.let { addMatch(4, it, scale, matches) }
                }
            }
        }}
    }

    private fun addMatch(level: Int, box: Rect, scale: Float, matches: MutableList<LevelMatch>) {
        val x = (box.left / scale).roundToInt()
        val y = (box.centerY() / scale).roundToInt()
        // The level label is at the left edge of every participant card. Using left X
        // is more stable than the centre because OCR may return either "C4" alone or
        // the whole participant line, whose centre differs substantially.
        if (matches.any { abs(it.x - x) <= 45 && abs(it.y - y) <= 20 }) return
        matches += LevelMatch(level, x, y)
    }

    private fun toHighContrast(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val gray = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
            val adjusted = if (gray < 205) 0 else 255
            pixels[i] = Color.rgb(adjusted, adjusted, adjusted)
        }
        result.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        return result
    }

    private fun buildResult(raw: List<LevelMatch>): TeamRecognitionResult? {
        if (raw.isEmpty()) return null
        val matches = raw.sortedWith(compareBy<LevelMatch> { it.x }.thenBy { it.y })
        val columns = groupIntoColumns(matches)
        val leaderColumn = columns.firstOrNull().orEmpty()
        // A leader exists only when the leftmost column contains exactly one card.
        val leader = leaderColumn.singleOrNull()

        // L1 is the first column to the right of the leader, then L2 and L3.
        // C0/C1 remain in the geometry so they cannot make a column disappear and
        // accidentally shift L4 into L3. Only C2-C6 belong to the team total.
        val firstThreeLevels = if (leader != null) columns.drop(1).take(3).flatten() else emptyList()
        val counts = IntArray(7)
        firstThreeLevels.forEach { match ->
            if (match.level in 1..6) counts[match.level]++
        }

        val cards = matches.map {
            TeamDetectedCard(
                level = it.level,
                x = it.x,
                y = it.y,
                excludedAsLeader = it === leader,
            )
        }
        return TeamRecognitionResult(counts, matches.size, leader != null, cards)
    }

    private fun groupIntoColumns(matches: List<LevelMatch>): List<List<LevelMatch>> {
        val columns = mutableListOf<MutableList<LevelMatch>>()
        matches.forEach { match ->
            val current = columns.lastOrNull()
            val currentX = current?.map { it.x }?.average()
            if (current == null || currentX == null || abs(match.x - currentX) > COLUMN_TOLERANCE_PX) {
                columns += mutableListOf(match)
            } else {
                current += match
            }
        }
        return columns
    }

    private data class Pass(val bitmap: Bitmap, val scale: Float)
    private data class LevelMatch(val level: Int, val x: Int, val y: Int)
}
