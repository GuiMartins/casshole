package com.faturaapp.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instancia: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase =
        instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fatura_app.db",
            ).build().also { instancia = it }
        }
}
