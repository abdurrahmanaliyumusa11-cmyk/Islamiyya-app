package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClassFeeSchedule
import com.example.data.model.FeePayment
import com.example.data.model.StudentFeeInstallment
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeDao {

    // Class Fee Schedules
    @Query("SELECT * FROM class_fee_schedules ORDER BY className ASC")
    fun getAllClassFeeSchedules(): Flow<List<ClassFeeSchedule>>

    @Query("SELECT * FROM class_fee_schedules ORDER BY className ASC")
    suspend fun getAllClassFeeSchedulesDirect(): List<ClassFeeSchedule>

    @Query("SELECT * FROM class_fee_schedules WHERE className = :className AND termLabel = :termLabel LIMIT 1")
    suspend fun getScheduleForClassAndTerm(className: String, termLabel: String): ClassFeeSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassFeeSchedule(schedule: ClassFeeSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassFeeSchedules(schedules: List<ClassFeeSchedule>)

    @Update
    suspend fun updateClassFeeSchedule(schedule: ClassFeeSchedule)

    // Student Fee Installments
    @Query("SELECT * FROM student_fee_installments ORDER BY dueDate ASC, id ASC")
    fun getAllInstallments(): Flow<List<StudentFeeInstallment>>

    @Query("SELECT * FROM student_fee_installments ORDER BY dueDate ASC, id ASC")
    suspend fun getAllInstallmentsDirect(): List<StudentFeeInstallment>

    @Query("SELECT * FROM student_fee_installments WHERE studentId = :studentId ORDER BY dueDate ASC, id ASC")
    fun getInstallmentsForStudent(studentId: Long): Flow<List<StudentFeeInstallment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: StudentFeeInstallment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<StudentFeeInstallment>)

    @Update
    suspend fun updateInstallment(installment: StudentFeeInstallment)

    @Delete
    suspend fun deleteInstallment(installment: StudentFeeInstallment)

    @Query("DELETE FROM student_fee_installments WHERE studentId = :studentId AND termLabel = :termLabel")
    suspend fun deleteInstallmentsForStudentTerm(studentId: Long, termLabel: String)

    // Fee Payments
    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC, id DESC")
    fun getAllPayments(): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC, id DESC")
    suspend fun getAllPaymentsDirect(): List<FeePayment>

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId ORDER BY paymentDate DESC, id DESC")
    fun getPaymentsForStudent(studentId: Long): Flow<List<FeePayment>>

    @Query("SELECT COUNT(*) FROM fee_payments")
    suspend fun getTotalPaymentsCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: FeePayment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<FeePayment>)

    @Delete
    suspend fun deletePayment(payment: FeePayment)
}
