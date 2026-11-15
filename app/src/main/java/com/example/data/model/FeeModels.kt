package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-class term tuition fee configuration in Nigerian Naira (NGN).
 */
@Entity(tableName = "class_fee_schedules")
data class ClassFeeSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val className: String,
    val termLabel: String = DEFAULT_TERM_LABEL,
    val tuitionAmount: Long,
    val defaultInstallmentCount: Int = 2,
    val firstDueDate: String = "2026-09-25",
    val secondDueDate: String = "2026-10-25",
    val thirdDueDate: String = "2026-12-05",
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_TERM_LABEL = "2026 Term 1"
    }
}

/**
 * Individual student installment obligation with explicit due date for automated reminders.
 */
@Entity(tableName = "student_fee_installments")
data class StudentFeeInstallment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val termLabel: String = ClassFeeSchedule.DEFAULT_TERM_LABEL,
    val installmentTitle: String,
    val amountDue: Long,
    val dueDate: String, // yyyy-MM-dd
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Recorded tuition payment transaction with auto-generated receipt number.
 */
@Entity(tableName = "fee_payments")
data class FeePayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val installmentId: Long? = null,
    val termLabel: String = ClassFeeSchedule.DEFAULT_TERM_LABEL,
    val amountPaid: Long,
    val paymentDate: String, // yyyy-MM-dd
    val paymentMethod: String = "Cash", // Cash, Bank Transfer, POS
    val receiptNumber: String,
    val recordedBy: String = "Administrator",
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
