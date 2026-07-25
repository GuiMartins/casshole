package com.faturaapp.data.local

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

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fatura_app.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
}
