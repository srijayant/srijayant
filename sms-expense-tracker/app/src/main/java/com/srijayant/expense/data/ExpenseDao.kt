package com.srijayant.expense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE timestamp >= :startMs AND timestamp < :endMs
        ORDER BY timestamp DESC
        """
    )
    fun observeBetween(startMs: Long, endMs: Long): Flow<List<Expense>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE timestamp >= :startMs AND timestamp < :endMs
        ORDER BY timestamp DESC
        """
    )
    suspend fun getBetween(startMs: Long, endMs: Long): List<Expense>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(expenses: List<Expense>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Query("SELECT smsId FROM expenses WHERE smsId IS NOT NULL")
    suspend fun getKnownSmsIds(): List<String>
}
