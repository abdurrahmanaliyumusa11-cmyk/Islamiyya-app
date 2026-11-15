package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val authManager = AdminAuthManager(context)
        if (!authManager.isAttendanceReminderEnabledDirect()) return

        // Handle boot completed: reschedule active daily reminders
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.scheduleDailyAttendanceReminder(
                context,
                authManager.getAttendanceReminderHourDirect(),
                authManager.getAttendanceReminderMinuteDirect()
            )
            if (authManager.isAnniversaryAlertsEnabledDirect()) {
                NotificationHelper.scheduleDailyAnniversaryCheck(context)
            }
            if (authManager.isFeeReminderEnabledDirect()) {
                NotificationHelper.scheduleDailyFeeReminderCheck(context)
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val classes = db.classDao().getAllClassesDirect()
                val recordedToday = db.attendanceDao().getAllAttendanceForDateDirect(today)
                    .map { it.className }
                    .toSet()

                val unmarked = classes.filter { it.name !in recordedToday }
                if (unmarked.isNotEmpty()) {
                    val classListText = unmarked.joinToString(", ") { it.name }
                    NotificationHelper.showAttendanceReminderNotification(
                        context = context,
                        title = "Daily Attendance Reminder (${unmarked.size} classes pending)",
                        message = "Teachers reminder: Please mark today's attendance register for $classListText."
                    )
                }

                // Reschedule for tomorrow
                NotificationHelper.scheduleDailyAttendanceReminder(
                    context,
                    authManager.getAttendanceReminderHourDirect(),
                    authManager.getAttendanceReminderMinuteDirect()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
