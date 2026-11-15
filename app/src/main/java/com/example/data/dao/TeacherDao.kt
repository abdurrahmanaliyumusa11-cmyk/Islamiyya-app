package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY fullName ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    fun getTeacherById(id: Long): Flow<Teacher?>

    @Query("SELECT * FROM teachers WHERE assignedClass = :className LIMIT 1")
    suspend fun getTeacherByClass(className: String): Teacher?

    @Query("SELECT COUNT(*) FROM teachers")
    fun getTeacherCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM teachers")
    suspend fun getTotalTeachersDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacherById(id: Long)
}
