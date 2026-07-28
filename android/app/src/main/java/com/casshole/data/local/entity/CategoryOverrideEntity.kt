package com.casshole.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Manual category correction "remembered" by the app: when the user edits a
 * transaction's category, we store description -> category here, so future
 * invoices with the same description (a fixed merchant name, e.g.
 * "Hospital das Bonecas") already fall into the right category automatically.
 */
@Entity(tableName = "categoria_overrides", indices = [Index(value = ["descricao"], unique = true)])
data class CategoryOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "descricao") val description: String,
    @ColumnInfo(name = "categoria") val category: String,
)
