package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_grades")
data class StudentGrade(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val termLabel: String,      // e.g. "2024 T2", "2024 T3", "2025 T1", "2025 T2", "2026 T1"
    val subject: String,        // e.g. "Quran & Tajweed", "Arabic", "Mathematics", "English", "Islamic Studies"
    val score: Int,             // 0..100
    val maxScore: Int = 100,
    val sortOrder: Int = 0,     // chronological ordering across terms
    val remarks: String = "",
    val recordedBy: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)
