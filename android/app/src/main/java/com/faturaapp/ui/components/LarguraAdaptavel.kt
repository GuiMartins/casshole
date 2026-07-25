package com.faturaapp.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Limita a largura do conteúdo pra manter uma leitura confortável em telas
 * grandes (tablets, tela interna aberta de dobráveis como o Fold) — em
 * celulares normais essa largura máxima nunca chega a ser atingida, então
 * o layout não muda em nada ali.
 */
fun Modifier.larguraDeLeituraConfortavel(): Modifier = this.widthIn(max = 640.dp)
