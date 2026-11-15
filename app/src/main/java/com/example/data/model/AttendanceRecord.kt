package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["date", "className", "studentId"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // e.g. "2026-10-04"
    val className: String,
    val studentId: Long,
    val studentName: String,
    val studentRegNo: String,
    val status: String = "Present", // "Present", "Absent", "Late", "Excused"
    val remarks: String = "",
    val markedBy: String = "Class Teacher",
    val timestamp: Long = System.currentTimeMillis()
)
