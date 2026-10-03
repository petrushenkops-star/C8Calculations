package com.pavel.c8calculations.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** A null bitmap means the caller must ask which page to import. */
data class PdfTeamPage(
    val pageCount: Int,
    val pageNumber: Int,
    val bitmap: Bitmap?,
    val textResult: TeamRecognitionResult?,
)

object PdfTeamImporter {
    suspend fun read(context: Context, uri: Uri, pageNumber: Int? = null): PdfTeamPage =
        withContext(Dispatchers.IO) {
            val file = File.createTempFile("team-", ".pdf", context.cacheDir)
            var bitmap: Bitmap? = null
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Не удалось открыть PDF")
                input.use { source ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val size = source.read(buffer)
                            if (size < 0) break
                            total += size
                            require(total <= 30L * 1024 * 1024) { "PDF больше 30 МБ. Выберите файл меньшего размера." }
                            output.write(buffer, 0, size)
                        }
                    }
                }
                var pageCount = 0
                val selectedPage = pageNumber ?: 1
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        pageCount = renderer.pageCount
                        require(pageCount > 0) { "В PDF нет страниц" }
                        if (pageCount > 1 && pageNumber == null) {
                            return@withContext PdfTeamPage(pageCount, 0, null, null)
                        }
                        require(selectedPage in 1..pageCount) { "Укажите страницу от 1 до $pageCount" }
                        renderer.openPage(selectedPage - 1).use { page ->
                            val scale = 1800f / max(page.width, page.height)
                            val preview = Bitmap.createBitmap(
                                (page.width * scale).roundToInt().coerceAtLeast(1),
                                (page.height * scale).roundToInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            )
                            bitmap = preview
                            preview.eraseColor(Color.WHITE)
                            page.render(preview, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
                ensureActive()
                // Text decoding may be unsupported for a font; the page remains available for OCR.
                val textResult = try {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                        val labels = mutableListOf<PdfLevelLabel>()
                        val stripper = object : PDFTextStripper() {
                            val characters = StringBuilder()
                            val positions = mutableListOf<TextPosition?>()

                            fun flushLine() {
                                PdfTeamParser.levelPattern.findAll(characters).forEach { match ->
                                    val position = positions[match.range.first] ?: return@forEach
                                    labels += PdfLevelLabel(
                                        match.groupValues[1].toInt(), position.xDirAdj,
                                        position.yDirAdj, position.heightDir,
                                    )
                                }
                                characters.setLength(0)
                                positions.clear()
                            }

                            override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                                if (characters.isNotEmpty()) {
                                    characters.append(' ')
                                    positions.add(null)
                                }
                                textPositions.forEach { position ->
                                    val unicode = position.unicode ?: ""
                                    characters.append(unicode)
                                    repeat(unicode.length) { positions += position }
                                }
                            }

                            override fun writeLineSeparator() { flushLine() }
                        }
                        stripper.sortByPosition = true
                        stripper.startPage = selectedPage
                        stripper.endPage = selectedPage
                        stripper.getText(document)
                        stripper.flushLine()
                        PdfTeamParser.parse(labels)
                    }
                } catch (_: java.io.IOException) {
                    null
                }
                ensureActive()
                PdfTeamPage(pageCount, selectedPage, bitmap, textResult)
            } catch (error: Throwable) {
                bitmap?.recycle()
                throw error
            } finally {
                file.delete()
            }
        }
}
