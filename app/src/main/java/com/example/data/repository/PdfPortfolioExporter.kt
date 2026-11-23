package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.local.CreationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

enum class PdfLayoutMode(val label: String, val subtitle: String) {
    PORTFOLIO("Portfolio", "1 artwork per page with prompt & details"),
    CONTACT_SHEET("Grid Sheet", "Up to 4 artworks per page")
}

object PdfPageMath {
    data class FittedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    fun fitAspectRect(
        imageWidth: Int,
        imageHeight: Int,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float
    ): FittedRect {
        if (imageWidth <= 0 || imageHeight <= 0 || boxWidth <= 0f || boxHeight <= 0f) {
            return FittedRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)
        }
        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val boxAspect = boxWidth / boxHeight

        val drawWidth: Float
        val drawHeight: Float
        if (imageAspect > boxAspect) {
            drawWidth = boxWidth
            drawHeight = boxWidth / imageAspect
        } else {
            drawHeight = boxHeight
            drawWidth = boxHeight * imageAspect
        }

        val left = boxLeft + (boxWidth - drawWidth) / 2f
        val top = boxTop + (boxHeight - drawHeight) / 2f
        return FittedRect(
            left = left,
            top = top,
            right = left + drawWidth,
            bottom = top + drawHeight
        )
    }

    fun calculateSampleSize(rawWidth: Int, rawHeight: Int, maxDim: Int = 1600): Int {
        var sample = 1
        while ((rawWidth / sample) > maxDim || (rawHeight / sample) > maxDim) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    fun sanitizeFileName(title: String): String {
        val cleaned = title.trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
        return if (cleaned.isNotEmpty()) cleaned.take(40) else "Lumina_Portfolio"
    }
}

class PdfPortfolioExporter(private val context: Context) {

    private val sharedPdfsDir: File
        get() = File(context.cacheDir, "shared_pdfs").apply {
            if (!exists()) mkdirs()
        }

    suspend fun generatePortfolioPdf(
        creations: List<CreationEntity>,
        title: String,
        layoutMode: PdfLayoutMode
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(creations.isNotEmpty()) { "Select at least one image to export." }

            cleanupOldPdfs()
            val portfolioTitle = title.trim().ifBlank { "Lumina Studio Portfolio" }
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in PDF points
            val pageHeight = 842 // A4 height in PDF points

            try {
                when (layoutMode) {
                    PdfLayoutMode.PORTFOLIO -> {
                        val totalPages = creations.size
                        creations.forEachIndexed { index, creation ->
                            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                            val page = pdfDocument.startPage(pageInfo)
                            drawPortfolioPage(
                                canvas = page.canvas,
                                pageWidth = pageWidth.toFloat(),
                                pageHeight = pageHeight.toFloat(),
                                portfolioTitle = portfolioTitle,
                                creation = creation,
                                pageNumber = index + 1,
                                totalPages = totalPages
                            )
                            pdfDocument.finishPage(page)
                        }
                    }

                    PdfLayoutMode.CONTACT_SHEET -> {
                        val chunks = creations.chunked(4)
                        val totalPages = chunks.size
                        chunks.forEachIndexed { pageIndex, chunk ->
                            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                            val page = pdfDocument.startPage(pageInfo)
                            drawContactSheetPage(
                                canvas = page.canvas,
                                pageWidth = pageWidth.toFloat(),
                                pageHeight = pageHeight.toFloat(),
                                portfolioTitle = portfolioTitle,
                                items = chunk,
                                totalCount = creations.size,
                                pageNumber = pageIndex + 1,
                                totalPages = totalPages
                            )
                            pdfDocument.finishPage(page)
                        }
                    }
                }

                val safeName = PdfPageMath.sanitizeFileName(portfolioTitle)
                val outFile = File(sharedPdfsDir, "${safeName}_${System.currentTimeMillis()}.pdf")
                FileOutputStream(outFile).use { stream ->
                    pdfDocument.writeTo(stream)
                }
                outFile
            } finally {
                pdfDocument.close()
            }
        }
    }

    fun getPdfShareUri(pdfFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
    }

    suspend fun copyPdfToUri(sourcePath: String, destinationUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = File(sourcePath)
                require(source.exists()) { "Generated PDF file was not found." }
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    source.inputStream().use { input ->
                        input.copyTo(out)
                    }
                    out.flush()
                } ?: throw IllegalStateException("Could not open destination document.")
                Unit
            }
        }

    private fun cleanupOldPdfs() {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        sharedPdfsDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private fun decodeSampledBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = PdfPageMath.calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDim = 1600)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }

    private fun drawPortfolioPage(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        portfolioTitle: String,
        creation: CreationEntity,
        pageNumber: Int,
        totalPages: Int
    ) {
        val margin = 36f
        val contentWidth = pageWidth - margin * 2f

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, pageWidth, pageHeight, bgPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 24, 38)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 102, 122)
            textSize = 10.5f
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(220, 225, 235)
            strokeWidth = 1f
        }

        canvas.drawText(portfolioTitle, margin, margin + 16f, titlePaint)
        val dateText = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(creation.createdAt))
        val headerMeta = "Artwork $pageNumber of $totalPages • $dateText"
        canvas.drawText(headerMeta, margin, margin + 32f, subtitlePaint)
        canvas.drawLine(margin, margin + 42f, pageWidth - margin, margin + 42f, dividerPaint)

        // Artwork frame
        val imageTop = margin + 56f
        val imageBoxHeight = 450f
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 247, 251)
            style = Paint.Style.FILL
        }
        val frameStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 228, 236)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val outerRect = RectF(margin, imageTop, margin + contentWidth, imageTop + imageBoxHeight)
        canvas.drawRoundRect(outerRect, 12f, 12f, framePaint)
        canvas.drawRoundRect(outerRect, 12f, 12f, frameStroke)

        val bitmap = decodeSampledBitmap(creation.imagePath)
        if (bitmap != null) {
            try {
                val fitted = PdfPageMath.fitAspectRect(
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    boxLeft = margin + 10f,
                    boxTop = imageTop + 10f,
                    boxWidth = contentWidth - 20f,
                    boxHeight = imageBoxHeight - 20f
                )
                val destRect = RectF(fitted.left, fitted.top, fitted.right, fitted.bottom)
                val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
            } finally {
                bitmap.recycle()
            }
        }

        // Metadata summary pill bar
        var cursorY = imageTop + imageBoxHeight + 22f
        val metaLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(67, 88, 180)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val seedSegment = creation.seed?.let { "   •   SEED: $it" } ?: ""
        val metaLine = "STYLE: ${creation.styleName.ifBlank { "No Style" }}   •   " +
            "ASPECT RATIO: ${creation.aspectRatioLabel}   •   MODEL: ${creation.modelId}$seedSegment"
        canvas.drawText(metaLine, margin, cursorY, metaLabelPaint)
        cursorY += 18f

        // Positive prompt
        val sectionHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 80, 100)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 30, 44)
            textSize = 11.5f
        }

        canvas.drawText("PROMPT", margin, cursorY, sectionHeadingPaint)
        cursorY += 6f
        val promptLayout = buildStaticLayout(
            text = creation.prompt,
            paint = bodyPaint,
            width = contentWidth.toInt(),
            maxLines = 7
        )
        canvas.save()
        canvas.translate(margin, cursorY)
        promptLayout.draw(canvas)
        canvas.restore()
        cursorY += promptLayout.height + 14f

        if (creation.negativePrompt.isNotBlank() && cursorY < pageHeight - 72f) {
            canvas.drawText("NEGATIVE PROMPT (EXCLUDED)", margin, cursorY, sectionHeadingPaint)
            cursorY += 6f
            val negPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(90, 98, 118)
                textSize = 10.5f
            }
            val negLayout = buildStaticLayout(
                text = creation.negativePrompt,
                paint = negPaint,
                width = contentWidth.toInt(),
                maxLines = 4
            )
            canvas.save()
            canvas.translate(margin, cursorY)
            negLayout.draw(canvas)
            canvas.restore()
        }

        drawFooter(canvas, pageWidth, pageHeight, margin, pageNumber, totalPages)
    }

    private fun drawContactSheetPage(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        portfolioTitle: String,
        items: List<CreationEntity>,
        totalCount: Int,
        pageNumber: Int,
        totalPages: Int
    ) {
        val margin = 32f
        val gap = 14f
        val contentWidth = pageWidth - margin * 2f

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, pageWidth, pageHeight, bgPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 24, 38)
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 102, 122)
            textSize = 10f
        }
        canvas.drawText(portfolioTitle, margin, margin + 14f, titlePaint)
        canvas.drawText(
            "$totalCount selected artworks • Sheet $pageNumber of $totalPages",
            margin,
            margin + 29f,
            subtitlePaint
        )

        val gridTop = margin + 44f
        val gridHeight = pageHeight - gridTop - margin - 24f
        val cellWidth = (contentWidth - gap) / 2f
        val cellHeight = (gridHeight - gap) / 2f

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 249, 252)
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 225, 235)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(67, 88, 180)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 36, 50)
            textSize = 9.5f
        }

        items.forEachIndexed { idx, creation ->
            val col = idx % 2
            val row = idx / 2
            val cellLeft = margin + col * (cellWidth + gap)
            val cellTop = gridTop + row * (cellHeight + gap)
            val cellRect = RectF(cellLeft, cellTop, cellLeft + cellWidth, cellTop + cellHeight)
            canvas.drawRoundRect(cellRect, 10f, 10f, cardBgPaint)
            canvas.drawRoundRect(cellRect, 10f, 10f, cardStrokePaint)

            val imgPadding = 10f
            val imgBoxHeight = cellHeight - 72f
            val bitmap = decodeSampledBitmap(creation.imagePath)
            if (bitmap != null) {
                try {
                    val fitted = PdfPageMath.fitAspectRect(
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        boxLeft = cellLeft + imgPadding,
                        boxTop = cellTop + imgPadding,
                        boxWidth = cellWidth - imgPadding * 2f,
                        boxHeight = imgBoxHeight
                    )
                    val dest = RectF(fitted.left, fitted.top, fitted.right, fitted.bottom)
                    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(bitmap, null, dest, bitmapPaint)
                } finally {
                    bitmap.recycle()
                }
            }

            val textTop = cellTop + imgPadding + imgBoxHeight + 14f
            canvas.drawText(
                "${creation.styleName.ifBlank { "Artwork" }} • ${creation.aspectRatioLabel}",
                cellLeft + imgPadding,
                textTop,
                metaPaint
            )

            val captionLayout = buildStaticLayout(
                text = creation.prompt,
                paint = captionPaint,
                width = (cellWidth - imgPadding * 2f).toInt(),
                maxLines = 3
            )
            canvas.save()
            canvas.translate(cellLeft + imgPadding, textTop + 4f)
            captionLayout.draw(canvas)
            canvas.restore()
        }

        drawFooter(canvas, pageWidth, pageHeight, margin, pageNumber, totalPages)
    }

    private fun drawFooter(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        margin: Float,
        pageNumber: Int,
        totalPages: Int
    ) {
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 140, 160)
            textSize = 9f
        }
        canvas.drawText(
            "Generated with Lumina Studio",
            margin,
            pageHeight - 18f,
            footerPaint
        )
        val pageLabel = "Page $pageNumber of $totalPages"
        val labelWidth = footerPaint.measureText(pageLabel)
        canvas.drawText(
            pageLabel,
            pageWidth - margin - labelWidth,
            pageHeight - 18f,
            footerPaint
        )
    }

    private fun buildStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setMaxLines(maxLines)
            .build()
    }
}
