package com.casshole.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.casshole.data.local.dao.CardNicknameDao
import com.casshole.data.local.dao.CategoryOverrideDao
import com.casshole.data.local.dao.DefaultPasswordDao
import com.casshole.data.local.dao.InvoiceDao
import com.casshole.data.local.dao.TransactionDao
import com.casshole.data.local.entity.CardNicknameEntity
import com.casshole.data.local.entity.CategoryOverrideEntity
import com.casshole.data.local.entity.DefaultPasswordEntity
import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.TransactionEntity

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
