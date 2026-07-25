package com.faturaapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.faturaapp.data.local.dao.CategoryOverrideDao
import com.faturaapp.data.local.dao.DefaultPasswordDao
import com.faturaapp.data.local.dao.InvoiceDao
import com.faturaapp.data.local.dao.TransactionDao
import com.faturaapp.data.local.entity.CategoryOverrideEntity
import com.faturaapp.data.local.entity.DefaultPasswordEntity
import com.faturaapp.data.local.entity.InvoiceEntity
import com.faturaapp.data.local.entity.TransactionEntity

@Database(
    entities = [
        InvoiceEntity::class,
        TransactionEntity::class,
        DefaultPasswordEntity::class,
        CategoryOverrideEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun defaultPasswordDao(): DefaultPasswordDao
    abstract fun categoryOverrideDao(): CategoryOverrideDao
}
