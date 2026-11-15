package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val age: Int,
    val dateOfBirth: String,
    val dateOfAdmission: String,
    val gender: String, // "Male" or "Female"
    val className: String, // e.g. "Primary 1", "Primary 2"
    val registrationNumber: String, // e.g. "UIPS/2026/001"
    val parentGuardianName: String = "",
    val parentGuardianPhone: String = "",
    val homeAddress: String = "",
    val previousSchool: String = "",
    val admissionStatus: String = "Active", // "Active", "Completed", "Transferred", "Suspended"
    val photoUri: String? = null,
    val additionalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
