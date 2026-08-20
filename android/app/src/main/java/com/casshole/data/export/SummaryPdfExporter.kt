package com.casshole.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** One line of the exported summary: a label on the left, its amount on the right. */
data class ExportRow(val label: String, val value: String)

/**
 * Writes the summary to a PDF in the cache directory and returns a shareable
 * URI for it.
 *
 * Exists because a screenshot isn't always available (a work profile or a
 * Samsung Secure Folder blocks capture system-wide, above anything the app
 * can control), and because a file survives being sent to someone else in a
 * way a screenshot of a scrolling list doesn't.
 *
 * Uses the platform's own [PdfDocument] - no PDF-writing dependency added
 * just for this; PdfBox is in the project to *read* invoices.
 */
object SummaryPdfExporter {

    // A4 at 72dpi, the unit PdfDocument works in.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 24f

    fun export(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        rows: List<ExportRow>,
        totalLabel: String,
        totalValue: String,
        footer: String,
    ): Uri {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
        val subtitlePaint = Paint().apply { textSize = 12f; color = 0xFF666666.toInt(); isAntiAlias = true }
        val labelPaint = Paint().apply { textSize = 13f; isAntiAlias = true }
        val valuePaint = Paint().apply { textSize = 13f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
        val totalPaint = Paint().apply { textSize = 15f; isFakeBoldText = true; isAntiAlias = true }
        val totalValuePaint = Paint().apply {
            textSize = 15f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT; isAntiAlias = true
        }
        val rulePaint = Paint().apply { color = 0xFFDDDDDD.toInt() }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 24f

        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 20f
        canvas.drawText(subtitle, MARGIN, y, subtitlePaint)
        y += 24f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
        y += LINE_HEIGHT

        for (row in rows) {
            // Long lists of categories are unlikely, but a page break is
            // cheaper than silently cutting rows off the bottom.
            if (y > PAGE_HEIGHT - MARGIN - LINE_HEIGHT * 3) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN + LINE_HEIGHT
            }
            canvas.drawText(row.label, MARGIN, y, labelPaint)
            canvas.drawText(row.value, PAGE_WIDTH - MARGIN, y, valuePaint)
            y += LINE_HEIGHT
        }

        y += 4f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rulePaint)
        y += LINE_HEIGHT
        canvas.drawText(totalLabel, MARGIN, y, totalPaint)
        canvas.drawText(totalValue, PAGE_WIDTH - MARGIN, y, totalValuePaint)

        canvas.drawText(footer, MARGIN, PAGE_HEIGHT - MARGIN, subtitlePaint)
        document.finishPage(page)

        val directory = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(directory, "$fileName.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
