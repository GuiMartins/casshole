package com.moneyhole.categorizer

// Order matters: more specific categories (ex: "amazon prime", "mercado
// livre") come before more generic ones that would collide by substring
// (ex: "amazon" alone, or "mercado" inside "mercadolivre").
private val CATEGORY_RULES: List<Pair<String, Regex>> = listOf(
    "Streaming/Assinaturas" to Regex(
        "netflix|spotify|amazon prime|disney|hbo|youtube|icloud|universal music|apple\\.com|" +
            "wellhub|gympass",
        RegexOption.IGNORE_CASE,
    ),
    "Pets" to Regex(
        "pet\\s?shop|petsupermark|petz|cobasi",
        RegexOption.IGNORE_CASE,
    ),
    "Compras" to Regex(
        "amazon|magalu|mercado\\s?livre|shopee|shein|americanas|aliexpress|kabum!?|" +
            "papelaria|livraria|leitura|calcados|confeccoes|vestuario|outlet|biju|tintas|" +
            "eletronicos|electronics|samsung|moveis|decoracao|cartucho",
        RegexOption.IGNORE_CASE,
    ),
    "Alimentação" to Regex(
        "ifd\\*|ifood|rappi|restaurante|pizzaria|padaria|lanchonete|mercado|supermercado|" +
            "mercearia|redeconomia|abastecedora|hortifruti|acougue|empada|sushi|burger|" +
            "fast food|gelados?|sorvete|acai|torta|doceria|confeitaria|salgad|boteco|" +
            "churrascaria",
        RegexOption.IGNORE_CASE,
    ),
    "Transporte" to Regex(
        "uber|99app|99pop|taxi|posto|combustivel|estacionamento|propark|pneu",
        RegexOption.IGNORE_CASE,
    ),
    "Saúde" to Regex(
        // Lookahead avoids a false positive like "Hospital das Bonecas" (a
        // doll-repair shop, not an actual hospital).
        "farmacia|drogaria|drogasil|pague\\s?menos|ultrafarma|raia\\d|" +
            "clinica|hospital(?!\\s*das\\s*bonecas)|laboratorio",
        RegexOption.IGNORE_CASE,
    ),
    "Educação" to Regex("udemy|alura|coursera|faculdade|escola", RegexOption.IGNORE_CASE),
    "Contas/Serviços" to Regex("energia|luz|agua|telefone|internet|claro|vivo|tim|oi\\b", RegexOption.IGNORE_CASE),
)

const val DEFAULT_CATEGORY = "Outros"

val AVAILABLE_CATEGORIES: List<String> = CATEGORY_RULES.map { it.first } + DEFAULT_CATEGORY

fun categorize(description: String): String {
    for ((category, pattern) in CATEGORY_RULES) {
        if (pattern.containsMatchIn(description)) return category
    }
    return DEFAULT_CATEGORY
}
