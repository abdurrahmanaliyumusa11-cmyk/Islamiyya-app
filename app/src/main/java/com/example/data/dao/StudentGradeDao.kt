package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudentGrade
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentGradeDao {
    @Query("SELECT * FROM student_grades WHERE studentId = :studentId ORDER BY sortOrder ASC, termLabel ASC, subject ASC")
    fun getGradesForStudent(studentId: Long): Flow<List<StudentGrade>>

    @Query("SELECT * FROM student_grades ORDER BY sortOrder ASC")
    fun getAllGrades(): Flow<List<StudentGrade>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: StudentGrade): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<StudentGrade>)

    @Delete
    suspend fun deleteGrade(grade: StudentGrade)
}
