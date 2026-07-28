package com.casshole.parsing

import android.graphics.RectF
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.PDFTextStripperByArea

fun parseBrazilianAmount(amountStr: String): Double =
    amountStr.replace(".", "").replace(",", ".").toDouble()

/** Simple text extraction (columns not separated), used for bank/month/year detection. */
fun extractSimplePdfText(pdfBytes: ByteArray, password: String): String {
    PDDocument.load(pdfBytes, password).use { document ->
        return PDFTextStripper().getText(document)
    }
}

/**
 * Some invoices (e.g. Itau) use a side-by-side two-column layout. Standard
 * text extraction interleaves the columns line by line and shuffles the
 * order (e.g. it merges a transaction from the left column with a summary
 * line from the right column). Because of that we cut each page at the
 * given cut point and extract each column separately, concatenating
 * left+right per page (order validated against the already-calibrated
 * Python parser: a transaction that prints in the right column of a page
 * still needs to land in the same "left column" chunk of the same page).
 */
fun extractPdfTextByColumns(pdfBytes: ByteArray, password: String, cutFraction: Float = 0.5f): String {
    PDDocument.load(pdfBytes, password).use { document ->
        val parts = document.pages.map { page ->
            val box = page.mediaBox
            val width = box.width
            val height = box.height
            val cut = width * cutFraction

            val stripper = PDFTextStripperByArea()
            stripper.sortByPosition = true
            stripper.addRegion("left", RectF(0f, 0f, cut, height))
            stripper.addRegion("right", RectF(cut, 0f, width, height))
            stripper.extractRegions(page)

            (stripper.getTextForRegion("left") ?: "") + "\n" + (stripper.getTextForRegion("right") ?: "")
        }
        return parts.joinToString("\n")
    }
}
