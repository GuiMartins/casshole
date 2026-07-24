package com.faturaapp.categorizer

// Ordem importa: categorias mais especificas (ex: "amazon prime", "mercado
// livre") ficam antes das mais genericas que colidiriam por substring (ex:
// "amazon" sozinho, ou "mercado" dentro de "mercadolivre").
private val REGRAS_CATEGORIA: List<Pair<String, Regex>> = listOf(
    "Streaming/Assinaturas" to Regex(
        "netflix|spotify|amazon prime|disney|hbo|youtube|icloud|universal music|apple\\.com",
        RegexOption.IGNORE_CASE,
    ),
    "Compras" to Regex(
        "amazon|magalu|mercado\\s?livre|shopee|shein|americanas|aliexpress|kabum!?",
        RegexOption.IGNORE_CASE,
    ),
    "Alimentação" to Regex(
        "ifd\\*|ifood|rappi|restaurante|pizzaria|padaria|lanchonete|mercado|supermercado|" +
            "redeconomia|abastecedora|hortifruti|acougue|empada|sushi|burger|fast food",
        RegexOption.IGNORE_CASE,
    ),
    "Transporte" to Regex("uber|99app|99pop|taxi|posto|combustivel|estacionamento|propark", RegexOption.IGNORE_CASE),
    "Saúde" to Regex("farmacia|drogaria|clinica|hospital|laboratorio", RegexOption.IGNORE_CASE),
    "Educação" to Regex("udemy|alura|coursera|faculdade|escola", RegexOption.IGNORE_CASE),
    "Contas/Serviços" to Regex("energia|luz|agua|telefone|internet|claro|vivo|tim|oi\\b", RegexOption.IGNORE_CASE),
)

const val CATEGORIA_PADRAO = "Outros"

val CATEGORIAS_DISPONIVEIS: List<String> = REGRAS_CATEGORIA.map { it.first } + CATEGORIA_PADRAO

fun categorizar(descricao: String): String {
    for ((categoria, padrao) in REGRAS_CATEGORIA) {
        if (padrao.containsMatchIn(descricao)) return categoria
    }
    return CATEGORIA_PADRAO
}
