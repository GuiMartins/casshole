package com.faturaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Correcao manual de categoria "lembrada" pelo app: quando o usuario edita a
 * categoria de uma transacao, guardamos descricao -> categoria aqui, pra
 * faturas futuras com a mesma descricao (nome de estabelecimento fixo, ex:
 * "Hospital das Bonecas") ja carem na categoria certa automaticamente.
 */
@Entity(tableName = "categoria_overrides", indices = [Index(value = ["descricao"], unique = true)])
data class CategoriaOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val categoria: String,
)
