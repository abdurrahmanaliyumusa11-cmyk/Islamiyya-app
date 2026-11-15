package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g. "Primary 1"
    val section: String = "Main",
    val classTeacher: String = "",
    val capacity: Int = 40,
    val description: String = ""
)
