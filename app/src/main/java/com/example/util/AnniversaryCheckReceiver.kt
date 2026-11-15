package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnniversaryCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val authManager = AdminAuthManager(context)
        if (!authManager.isAnniversaryAlertsEnabledDirect()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val students = db.studentDao().getAllStudentsDirect()
                val upcoming = NotificationHelper.findUpcomingAnniversaries(students, windowDays = 7)
                if (upcoming.isNotEmpty()) {
                    NotificationHelper.showAnniversaryNotification(context, upcoming)
                }
                // Reschedule for tomorrow
                NotificationHelper.scheduleDailyAnniversaryCheck(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
