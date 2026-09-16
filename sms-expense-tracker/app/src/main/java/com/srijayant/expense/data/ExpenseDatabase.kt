package com.srijayant.expense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): ExpenseCategory =
        runCatching { ExpenseCategory.valueOf(value) }.getOrDefault(ExpenseCategory.OTHER)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: ExpenseDatabase? = null

        fun get(context: Context): ExpenseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_pulse.db"
                ).build().also { instance = it }
            }
    }
}
