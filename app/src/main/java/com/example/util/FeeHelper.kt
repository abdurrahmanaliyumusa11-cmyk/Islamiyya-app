package com.example.util

import com.example.data.model.ClassFeeSchedule
import com.example.data.model.FeePayment
import com.example.data.model.SchoolInfo
import com.example.data.model.Student
import com.example.data.model.StudentFeeInstallment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class StudentFeeStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    DUE_SOON("Due Soon"),
    OVERDUE("Overdue"),
    PENDING("Pending"),
    UNBILLED("Unbilled")
}

data class InstallmentAllocation(
    val installment: StudentFeeInstallment,
    val paidTowardInstallment: Long,
    val remaining: Long,
    val daysUntilDue: Long,
    val isCleared: Boolean,
    val isOverdue: Boolean,
    val isDueSoon: Boolean
)

data class StudentFeeSummary(
    val student: Student,
    val termLabel: String,
    val totalDue: Long,
    val totalPaid: Long,
    val balance: Long,
    val status: StudentFeeStatus,
    val nextDueDate: String?,
    val nextDueAmount: Long,
    val overdueAmount: Long,
    val overdueCount: Int,
    val dueSoonCount: Int,
    val installmentBreakdown: List<InstallmentAllocation>,
    val payments: List<FeePayment>
)

object FeeHelper {

    fun formatNaira(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return "₦${formatter.format(amount)}"
    }

    fun todayDateString(referenceCal: Calendar = Calendar.getInstance()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(referenceCal.time)
    }

    /**
     * Computes days between today and dueDate (positive = in future, negative = overdue).
     */
    fun daysBetweenTodayAndDate(dueDateStr: String, referenceDateStr: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val due = sdf.parse(dueDateStr.trim()) ?: return 0L
            val ref = sdf.parse(referenceDateStr.trim()) ?: Date()
            val diffMs = due.time - ref.time
            TimeUnit.MILLISECONDS.toDays(diffMs)
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Generates balanced Naira installments for a student given a per-class fee schedule.
     */
    fun buildInstallmentsForStudent(
        studentId: Long,
        schedule: ClassFeeSchedule
    ): List<StudentFeeInstallment> {
        val count = schedule.defaultInstallmentCount.coerceIn(1, 3)
        val total = schedule.tuitionAmount.coerceAtLeast(0L)
        if (total <= 0L) return emptyList()

        val dueDates = listOf(schedule.firstDueDate, schedule.secondDueDate, schedule.thirdDueDate)
        val titles = listOf("1st Installment", "2nd Installment", "3rd Installment")

        val baseShare = (total / count / 100L) * 100L // clean 100 Naira chunks
        val list = mutableListOf<StudentFeeInstallment>()
        var assigned = 0L

        for (i in 0 until count) {
            val amount = if (i == count - 1) {
                (total - assigned).coerceAtLeast(0L)
            } else {
                assigned += baseShare
                baseShare
            }
            list.add(
                StudentFeeInstallment(
                    studentId = studentId,
                    termLabel = schedule.termLabel,
                    installmentTitle = titles[i],
                    amountDue = amount,
                    dueDate = dueDates[i],
                    notes = "Auto-assigned from ${schedule.className} fee schedule"
                )
            )
        }
        return list
    }

    /**
     * Reconciles student installments and recorded payments for a term into a live ledger summary.
     */
    fun computeStudentFeeSummary(
        student: Student,
        installments: List<StudentFeeInstallment>,
        payments: List<FeePayment>,
        termLabel: String = ClassFeeSchedule.DEFAULT_TERM_LABEL,
        referenceDateStr: String = todayDateString(),
        dueSoonWindowDays: Int = 7
    ): StudentFeeSummary {
        val termInstallments = installments
            .filter { it.studentId == student.id && it.termLabel == termLabel }
            .sortedWith(compareBy({ it.dueDate }, { it.id }))

        val termPayments = payments
            .filter { it.studentId == student.id && it.termLabel == termLabel }
            .sortedWith(compareByDescending<FeePayment> { it.paymentDate }.thenByDescending { it.id })

        val totalDue = termInstallments.sumOf { it.amountDue }
        val totalPaid = termPayments.sumOf { it.amountPaid }
        val balance = (totalDue - totalPaid).coerceAtLeast(0L)

        // First allocate payments explicitly linked to an installmentId, then allocate remaining FIFO
        val allocatedMap = mutableMapOf<Long, Long>()
        var unlinkedPool = 0L

        termPayments.forEach { p ->
            val targetId = p.installmentId
            if (targetId != null && termInstallments.any { it.id == targetId }) {
                allocatedMap[targetId] = (allocatedMap[targetId] ?: 0L) + p.amountPaid
            } else {
                unlinkedPool += p.amountPaid
            }
        }

        val breakdown = mutableListOf<InstallmentAllocation>()
        for (inst in termInstallments) {
            val directPaid = allocatedMap[inst.id] ?: 0L
            val neededForInst = (inst.amountDue - directPaid).coerceAtLeast(0L)
            val fromPool = minOf(unlinkedPool, neededForInst)
            unlinkedPool -= fromPool
            val paidToward = (directPaid + fromPool).coerceAtMost(inst.amountDue)
            val remaining = (inst.amountDue - paidToward).coerceAtLeast(0L)
            val isCleared = remaining <= 0L
            val daysUntil = daysBetweenTodayAndDate(inst.dueDate, referenceDateStr)
            val isOverdue = !isCleared && daysUntil < 0L
            val isDueSoon = !isCleared && daysUntil in 0L..dueSoonWindowDays.toLong()

            breakdown.add(
                InstallmentAllocation(
                    installment = inst,
                    paidTowardInstallment = paidToward,
                    remaining = remaining,
                    daysUntilDue = daysUntil,
                    isCleared = isCleared,
                    isOverdue = isOverdue,
                    isDueSoon = isDueSoon
                )
            )
        }

        val overdueAllocations = breakdown.filter { it.isOverdue }
        val dueSoonAllocations = breakdown.filter { it.isDueSoon }
        val nextOpen = breakdown.firstOrNull { !it.isCleared }

        val overdueAmount = overdueAllocations.sumOf { it.remaining }

        val status = when {
            totalDue <= 0L -> StudentFeeStatus.UNBILLED
            balance <= 0L -> StudentFeeStatus.PAID
            overdueAllocations.isNotEmpty() -> StudentFeeStatus.OVERDUE
            dueSoonAllocations.isNotEmpty() -> StudentFeeStatus.DUE_SOON
            totalPaid > 0L -> StudentFeeStatus.PARTIAL
            else -> StudentFeeStatus.PENDING
        }

        return StudentFeeSummary(
            student = student,
            termLabel = termLabel,
            totalDue = totalDue,
            totalPaid = totalPaid,
            balance = balance,
            status = status,
            nextDueDate = nextOpen?.installment?.dueDate,
            nextDueAmount = nextOpen?.remaining ?: 0L,
            overdueAmount = overdueAmount,
            overdueCount = overdueAllocations.size,
            dueSoonCount = dueSoonAllocations.size,
            installmentBreakdown = breakdown,
            payments = termPayments
        )
    }

    /**
     * Returns all student fee summaries that are either Overdue or Due Soon, sorted by urgency.
     */
    fun findDueAndOverdueReminders(summaries: List<StudentFeeSummary>): List<StudentFeeSummary> {
        return summaries
            .filter { it.status == StudentFeeStatus.OVERDUE || it.status == StudentFeeStatus.DUE_SOON }
            .sortedWith(
                compareBy<StudentFeeSummary> { if (it.status == StudentFeeStatus.OVERDUE) 0 else 1 }
                    .thenByDescending { it.overdueAmount }
                    .thenByDescending { it.balance }
                    .thenBy { it.student.fullName }
            )
    }

    /**
     * Generates a pre-filled parent SMS payment reminder mentioning exact Naira balance and due date.
     */
    fun buildFeeReminderSmsMessage(summary: StudentFeeSummary, schoolInfo: SchoolInfo): String {
        val schoolName = schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" }
        val guardian = summary.student.parentGuardianName.ifBlank { "Parent/Guardian" }
        val amountFocus = if (summary.overdueAmount > 0L) summary.overdueAmount else summary.nextDueAmount.takeIf { it > 0L } ?: summary.balance
        val dueText = summary.nextDueDate?.let { " (due $it)" } ?: ""
        val urgency = if (summary.status == StudentFeeStatus.OVERDUE) "overdue tuition installment" else "upcoming tuition installment"

        return "Assalamu Alaikum $guardian. Reminder from $schoolName regarding ${summary.student.fullName} " +
            "(${summary.student.className}): $urgency of ${formatNaira(amountFocus)}$dueText for ${summary.termLabel}. " +
            "Total balance: ${formatNaira(summary.balance)}. Enquiries: ${schoolInfo.phoneNumber}."
    }

    fun buildBulkFeeReminderSmsMessage(schoolInfo: SchoolInfo, termLabel: String): String {
        val schoolName = schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" }
        return "Assalamu Alaikum Dear Parent/Guardian. Friendly tuition fee reminder from $schoolName " +
            "for $termLabel: Please clear any due or overdue school fee installments at the Admin Office. " +
            "Contact ${schoolInfo.phoneNumber} for your balance."
    }
}
