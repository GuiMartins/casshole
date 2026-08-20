package com.casshole.categorizer

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
    // Pedido de comida pronta e mercado são hábitos de gasto diferentes, então
    // não dividem categoria. Só marcas de delivery entram aqui - um "delivery"
    // solto pegaria também entrega de supermercado, que é compra de mercado.
    "Delivery" to Regex(
        "ifd\\*|ifood|rappi|uber\\s?eats|99\\s?food|aiqfome|aiq\\s?fome|" +
            "james\\s?delivery|ze\\s?delivery|zé\\s?delivery|goomer|delivery\\s?center",
        RegexOption.IGNORE_CASE,
    ),
    "Compras" to Regex(
        "amazon|magalu|mercado\\s?livre|shopee|shein|americanas|aliexpress|kabum!?|" +
            "papelaria|livraria|leitura|calcados|confeccoes|vestuario|outlet|biju|tintas|" +
            "eletronicos|electronics|samsung|moveis|decoracao|cartucho",
        RegexOption.IGNORE_CASE,
    ),
    "Alimentação" to Regex(
        "restaurante|pizzaria|padaria|lanchonete|mercado|supermercado|" +
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

/**
 * Bumped whenever the rules above change in a way that would recategorize
 * already-imported transactions (e.g. splitting food delivery out of
 * "Alimentação"). Invoices already in the database are re-run through
 * [categorize] once per revision - see CategoryRefresher.
 */
const val CATEGORIZER_REVISION = 2

val AVAILABLE_CATEGORIES: List<String> = CATEGORY_RULES.map { it.first } + DEFAULT_CATEGORY

fun categorize(description: String): String {
    for ((category, pattern) in CATEGORY_RULES) {
        if (pattern.containsMatchIn(description)) return category
    }
    return DEFAULT_CATEGORY
}
