package com.faturaapp.parsing

import android.graphics.RectF
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.PDFTextStripperByArea

fun parseValorBr(valorStr: String): Double =
    valorStr.replace(".", "").replace(",", ".").toDouble()

/** Extracao de texto simples (sem separar colunas), usada pra deteccao de banco e mes/ano. */
fun extrairTextoPdfSimples(pdfBytes: ByteArray, senha: String): String {
    PDDocument.load(pdfBytes, senha).use { document ->
        return PDFTextStripper().getText(document)
    }
}

/**
 * Algumas faturas (ex: Itau) usam layout de duas colunas lado a lado. A
 * extracao de texto padrao intercala as colunas linha a linha e embaralha
 * a ordem (ex: junta uma transacao da coluna esquerda com uma linha de
 * resumo da coluna direita). Por isso cortamos cada pagina no ponto de
 * corte informado e extraimos cada coluna separadamente, concatenando
 * esquerda+direita por pagina (ordem validada contra o parser Python
 * ja calibrado: uma transacao que imprime na coluna direita da pagina
 * ainda precisa cair no mesmo trecho da coluna esquerda da mesma pagina).
 */
fun extrairTextoPdfPorColunas(pdfBytes: ByteArray, senha: String, fracaoCorte: Float = 0.5f): String {
    PDDocument.load(pdfBytes, senha).use { document ->
        val partes = document.pages.map { page ->
            val box = page.mediaBox
            val largura = box.width
            val altura = box.height
            val corte = largura * fracaoCorte

            val stripper = PDFTextStripperByArea()
            stripper.sortByPosition = true
            stripper.addRegion("esquerda", RectF(0f, 0f, corte, altura))
            stripper.addRegion("direita", RectF(corte, 0f, largura, altura))
            stripper.extractRegions(page)

            (stripper.getTextForRegion("esquerda") ?: "") + "\n" + (stripper.getTextForRegion("direita") ?: "")
        }
        return partes.joinToString("\n")
    }
}
