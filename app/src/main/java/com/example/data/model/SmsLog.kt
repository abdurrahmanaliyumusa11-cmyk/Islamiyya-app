package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String,         // "Attendance Alert", "School Announcement", "Report Card", "Custom"
    val recipientSummary: String,  // e.g. "Primary 2 Parents (2)" or "Alhaji Muhammad Bello"
    val recipientPhones: String,   // comma-separated phone numbers
    val recipientCount: Int,
    val messageBody: String,
    val sentBy: String,
    val timestamp: Long = System.currentTimeMillis()
)
