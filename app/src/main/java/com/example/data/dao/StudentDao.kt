package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY createdAt DESC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    suspend fun getAllStudentsDirect(): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentById(id: Long): Flow<Student?>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentByIdDirect(id: Long): Student?

    @Query("""
        SELECT * FROM students 
        WHERE fullName LIKE '%' || :query || '%' 
           OR registrationNumber LIKE '%' || :query || '%'
           OR parentGuardianName LIKE '%' || :query || '%'
        ORDER BY fullName ASC
    """)
    fun searchStudents(query: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE className = :className ORDER BY fullName ASC")
    fun getStudentsByClass(className: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE gender = :gender ORDER BY fullName ASC")
    fun getStudentsByGender(gender: String): Flow<List<Student>>

    @Query("SELECT COUNT(*) FROM students")
    fun getStudentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE gender = 'Male'")
    fun getMaleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE gender = 'Female'")
    fun getFemaleCount(): Flow<Int>

    @Query("SELECT registrationNumber FROM students ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRegistrationNumber(): String?

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getTotalCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>): List<Long>

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: Long)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}
