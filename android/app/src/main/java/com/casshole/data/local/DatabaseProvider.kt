package com.casshole.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categoria_overrides` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `descricao` TEXT NOT NULL,
                `categoria` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categoria_overrides_descricao` " +
                "ON `categoria_overrides` (`descricao`)"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apelidos_cartao` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `banco` TEXT NOT NULL,
                `cartao` TEXT NOT NULL,
                `apelido` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_apelidos_cartao_banco_cartao` " +
                "ON `apelidos_cartao` (`banco`, `cartao`)"
        )
    }
}

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "casshole.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
}
