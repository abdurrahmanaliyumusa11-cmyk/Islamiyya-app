package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val authManager = AdminAuthManager(context)
        if (!authManager.isFeeReminderEnabledDirect()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val students = db.studentDao().getAllStudentsDirect()
                val installments = db.feeDao().getAllInstallmentsDirect()
                val payments = db.feeDao().getAllPaymentsDirect()
                val leadDays = authManager.getFeeReminderLeadDaysDirect()
                val today = FeeHelper.todayDateString()

                val summaries = students.map { student ->
                    FeeHelper.computeStudentFeeSummary(
                        student = student,
                        installments = installments,
                        payments = payments,
                        referenceDateStr = today,
                        dueSoonWindowDays = leadDays
                    )
                }
                val reminders = FeeHelper.findDueAndOverdueReminders(summaries)
                if (reminders.isNotEmpty()) {
                    NotificationHelper.showFeeDueReminderNotification(context, reminders)
                }

                // Reschedule for tomorrow
                NotificationHelper.scheduleDailyFeeReminderCheck(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
