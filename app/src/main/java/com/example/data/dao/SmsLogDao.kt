package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogs(): Flow<List<SmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLog): Long

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteSmsLogById(id: Long)
}
