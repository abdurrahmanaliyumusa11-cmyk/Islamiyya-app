package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Student
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class UpcomingAnniversary(
    val student: Student,
    val yearsCompleted: Int,
    val daysUntil: Int,
    val anniversaryDateFormatted: String
)

object NotificationHelper {

    const val CHANNEL_ATTENDANCE = "daily_attendance_reminder"
    const val CHANNEL_ANNIVERSARIES = "admission_anniversaries"
    const val CHANNEL_FEES = "tuition_fee_reminders"

    private const val NOTIFICATION_ID_ATTENDANCE = 1001
    private const val NOTIFICATION_ID_ANNIVERSARY = 1002
    private const val NOTIFICATION_ID_FEES = 1003

    private const val REQUEST_CODE_ATTENDANCE_ALARM = 2001
    private const val REQUEST_CODE_ANNIVERSARY_ALARM = 2002
    private const val REQUEST_CODE_FEE_ALARM = 2003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Daily Attendance Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds teachers to mark daily class attendance registers"
                enableVibration(true)
            }

            val anniversaryChannel = NotificationChannel(
                CHANNEL_ANNIVERSARIES,
                "Student Admission Anniversaries",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies administrators of upcoming student admission anniversaries"
            }

            val feeChannel = NotificationChannel(
                CHANNEL_FEES,
                "Tuition Fee Due-Date Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts administrators when student fee installments are due soon or overdue"
                enableVibration(true)
            }

            manager.createNotificationChannel(attendanceChannel)
            manager.createNotificationChannel(anniversaryChannel)
            manager.createNotificationChannel(feeChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showAttendanceReminderNotification(
        context: Context,
        title: String = "Daily Attendance Reminder",
        message: String = "Please mark today's class attendance register for Umuriddin Islamiyya Primary School."
    ): Boolean {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_ATTENDANCE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ATTENDANCE, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showAnniversaryNotification(
        context: Context,
        anniversaries: List<UpcomingAnniversary>
    ): Boolean {
        if (anniversaries.isEmpty()) return false
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val title = if (anniversaries.size == 1) {
            "Student Admission Anniversary"
        } else {
            "${anniversaries.size} Upcoming Admission Anniversaries"
        }

        val summaryLines = anniversaries.take(5).map { item ->
            val whenText = when (item.daysUntil) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> "In ${item.daysUntil} days"
            }
            "• ${item.student.fullName} (${item.student.className}) — ${item.yearsCompleted} yr(s) ($whenText)"
        }
        val body = summaryLines.joinToString("\n")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_ANNIVERSARY,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ANNIVERSARIES)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(summaryLines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ANNIVERSARY, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showFeeDueReminderNotification(
        context: Context,
        dueOrOverdueSummaries: List<StudentFeeSummary>
    ): Boolean {
        if (dueOrOverdueSummaries.isEmpty()) return false
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val overdueCount = dueOrOverdueSummaries.count { it.status == StudentFeeStatus.OVERDUE }
        val dueSoonCount = dueOrOverdueSummaries.count { it.status == StudentFeeStatus.DUE_SOON }
        val title = when {
            overdueCount > 0 && dueSoonCount > 0 ->
                "Tuition Alert: $overdueCount Overdue, $dueSoonCount Due Soon"
            overdueCount > 0 ->
                "Tuition Alert: $overdueCount Overdue Student Account(s)"
            else ->
                "Tuition Reminder: $dueSoonCount Installment(s) Due Soon"
        }

        val lines = dueOrOverdueSummaries.take(5).map { summary ->
            val tag = if (summary.status == StudentFeeStatus.OVERDUE) "OVERDUE" else "Due ${summary.nextDueDate}"
            "• ${summary.student.fullName} (${summary.student.className}) — ${FeeHelper.formatNaira(summary.balance)} [$tag]"
        }
        val body = lines.joinToString("\n")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_FEES,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FEES)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FEES, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun findUpcomingAnniversaries(
        students: List<Student>,
        referenceDate: Calendar = Calendar.getInstance(),
        windowDays: Int = 7
    ): List<UpcomingAnniversary> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFmt = SimpleDateFormat("dd MMM", Locale.US)

        val todayCal = (referenceDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentYear = todayCal.get(Calendar.YEAR)

        val results = mutableListOf<UpcomingAnniversary>()
        students.forEach { student ->
            val admStr = student.dateOfAdmission.trim()
            val admDate = try {
                sdf.parse(admStr)
            } catch (_: Exception) {
                null
            } ?: return@forEach

            val admCal = Calendar.getInstance().apply { time = admDate }
            val admYear = admCal.get(Calendar.YEAR)
            val admMonth = admCal.get(Calendar.MONTH)
            val admDay = admCal.get(Calendar.DAY_OF_MONTH)

            // Only count anniversaries of at least 1 year
            if (admYear >= currentYear && admCal.after(todayCal)) return@forEach

            // Check candidate anniversary in currentYear and nextYear (for Dec -> Jan rollover)
            for (targetYear in listOf(currentYear, currentYear + 1)) {
                val yearsCompleted = targetYear - admYear
                if (yearsCompleted <= 0) continue

                val annivCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, admMonth)
                    set(Calendar.DAY_OF_MONTH, admDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val diffMillis = annivCal.timeInMillis - todayCal.timeInMillis
                val diffDays = (diffMillis / (24L * 60L * 60L * 1000L)).toInt()
                if (diffDays in 0..windowDays) {
                    results.add(
                        UpcomingAnniversary(
                            student = student,
                            yearsCompleted = yearsCompleted,
                            daysUntil = diffDays,
                            anniversaryDateFormatted = displayFmt.format(annivCal.time)
                        )
                    )
                    break
                }
            }
        }

        return results.sortedWith(compareBy({ it.daysUntil }, { it.student.fullName }))
    }

    fun scheduleDailyAttendanceReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AttendanceReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ATTENDANCE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleNextOccurrence(alarmManager, pendingIntent, hour, minute)
    }

    fun cancelAttendanceReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AttendanceReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ATTENDANCE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleDailyAnniversaryCheck(context: Context, hour: Int = 7, minute: Int = 45) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AnniversaryCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ANNIVERSARY_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleNextOccurrence(alarmManager, pendingIntent, hour, minute)
    }

    fun cancelAnniversaryCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AnniversaryCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ANNIVERSARY_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleDailyFeeReminderCheck(context: Context, hour: Int = 8, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FeeReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FEE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleNextOccurrence(alarmManager, pendingIntent, hour, minute)
    }

    fun cancelFeeReminderCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FeeReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FEE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleNextOccurrence(
        alarmManager: AlarmManager,
        pendingIntent: PendingIntent,
        hour: Int,
        minute: Int
    ) {
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerCal.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerCal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
        }
    }
}
