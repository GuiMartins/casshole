package com.faturaapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Classe de largura da janela (Material [WindowWidthSizeClass]), calculada
 * uma vez em MainActivity via `calculateWindowSizeClass` e disponibilizada
 * pra qualquer tela via [LocalWindowWidthSizeClass.current]. Default
 * Compact (celular) só entra em uso se alguém ler o valor fora da árvore
 * de composição que o MainActivity provê.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val LocalWindowWidthSizeClass: ProvidableCompositionLocal<WindowWidthSizeClass> =
    compositionLocalOf { WindowWidthSizeClass.Compact }

/**
 * Largura máxima de conteúdo fora da classe Compact — tablets, telas
 * internas abertas de dobráveis (Fold) caem em Medium ou Expanded e, sem
 * esse limite, listas/cards phone-first esticam de ponta a ponta com vãos
 * vazios enormes. Em Compact (a grande maioria dos celulares) isso não
 * tem efeito nenhum: a tela nunca chega a essa largura.
 */
private val LARGURA_MAXIMA_CONTEUDO = 600.dp

/**
 * Envolve o conteúdo principal de uma tela, centralizando-o e limitando a
 * largura fora da classe Compact. Substitui o `Modifier.fillMaxWidth()`
 * de nível mais alto de cada tela — o conteúdo interno (LazyColumn/Column)
 * continua usando `fillMaxWidth()` normalmente, só que agora preenche até
 * o teto imposto aqui, não a tela inteira.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun TelaAdaptavel(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter,
    content: @Composable () -> Unit,
) {
    val larguraJanela = LocalWindowWidthSizeClass.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = alignment) {
        Box(
            modifier = if (larguraJanela == WindowWidthSizeClass.Compact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .widthIn(max = LARGURA_MAXIMA_CONTEUDO)
                    .fillMaxWidth()
            },
        ) {
            content()
        }
    }
}
