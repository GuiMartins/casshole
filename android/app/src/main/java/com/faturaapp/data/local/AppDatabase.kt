package com.faturaapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.faturaapp.data.local.dao.FaturaDao
import com.faturaapp.data.local.dao.SenhaPadraoDao
import com.faturaapp.data.local.dao.TransacaoDao
import com.faturaapp.data.local.entity.FaturaEntity
import com.faturaapp.data.local.entity.SenhaPadraoEntity
import com.faturaapp.data.local.entity.TransacaoEntity

@Database(
    entities = [FaturaEntity::class, TransacaoEntity::class, SenhaPadraoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun faturaDao(): FaturaDao
    abstract fun transacaoDao(): TransacaoDao
    abstract fun senhaPadraoDao(): SenhaPadraoDao
}
