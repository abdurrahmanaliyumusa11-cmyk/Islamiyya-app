package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE className = :className AND date = :date ORDER BY studentName ASC")
    fun getAttendanceForClassAndDate(className: String, date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE className = :className AND date = :date ORDER BY studentName ASC")
    suspend fun getAttendanceForClassAndDateDirect(className: String, date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY className ASC, studentName ASC")
    fun getAllAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY className ASC, studentName ASC")
    suspend fun getAllAttendanceForDateDirect(date: String): List<AttendanceRecord>

    @Query("SELECT * FROM attendance WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    fun getAllAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecord>>

    @Query("SELECT DISTINCT date FROM attendance WHERE date LIKE :monthPrefix || '%'")
    fun getRecordedDatesForMonth(monthPrefix: String): Flow<List<String>>

    @Query("SELECT DISTINCT date FROM attendance WHERE className = :className ORDER BY date DESC")
    fun getRecordedDatesForClass(className: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBatch(records: List<AttendanceRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Query("DELETE FROM attendance WHERE className = :className AND date = :date")
    suspend fun deleteAttendanceForDate(className: String, date: String)
}
