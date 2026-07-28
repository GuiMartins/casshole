package com.moneyhole.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moneyhole.data.local.dao.CardNicknameDao
import com.moneyhole.data.local.dao.CategoryOverrideDao
import com.moneyhole.data.local.dao.DefaultPasswordDao
import com.moneyhole.data.local.dao.InvoiceDao
import com.moneyhole.data.local.dao.TransactionDao
import com.moneyhole.data.local.entity.CardNicknameEntity
import com.moneyhole.data.local.entity.CategoryOverrideEntity
import com.moneyhole.data.local.entity.DefaultPasswordEntity
import com.moneyhole.data.local.entity.InvoiceEntity
import com.moneyhole.data.local.entity.TransactionEntity

@Database(
    entities = [
        InvoiceEntity::class,
        TransactionEntity::class,
        DefaultPasswordEntity::class,
        CategoryOverrideEntity::class,
        CardNicknameEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun defaultPasswordDao(): DefaultPasswordDao
    abstract fun categoryOverrideDao(): CategoryOverrideDao
    abstract fun cardNicknameDao(): CardNicknameDao
}
