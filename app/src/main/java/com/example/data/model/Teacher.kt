package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val staffId: String, // e.g. "UIPS/T/001"
    val phoneNumber: String = "",
    val gender: String = "Male", // "Male" or "Female"
    val qualification: String = "NCE Arabic & Islamic Studies",
    val assignedClass: String = "", // e.g. "Primary 1"
    val subjectSpecialization: String = "Islamic Studies & Arabic",
    val role: String = "Class Teacher", // "Class Teacher", "Subject Teacher", or "Admin"
    val status: String = "Active", // "Active", "On Leave", "Former"
    val joinedDate: String = "2024-09-01"
)
