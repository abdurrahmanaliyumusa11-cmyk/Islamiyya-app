package com.example.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.SchoolInfo
import com.example.data.model.Student

enum class SmsEventTemplate(val title: String, val badge: String) {
    ATTENDANCE_ALERT("Attendance Alert", "Attendance"),
    FEE_REMINDER("Tuition Fee Reminder", "Fees"),
    SCHOOL_ANNOUNCEMENT("School Announcement", "Announcement"),
    REPORT_CARD("Report Card Availability", "Report Card"),
    CUSTOM("Custom Parent Message", "Custom")
}

object SmsHelper {

    fun cleanPhoneNumber(raw: String): String {
        return raw.trim().replace(" ", "").replace("-", "")
    }

    fun isValidPhoneNumber(raw: String): Boolean {
        val cleaned = cleanPhoneNumber(raw)
        val digits = cleaned.count { it.isDigit() }
        return digits >= 7
    }

    fun buildTemplateMessage(
        template: SmsEventTemplate,
        schoolInfo: SchoolInfo,
        selectedStudents: List<Student>,
        dateStr: String = ""
    ): String {
        val schoolName = schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" }
        val singleStudent = selectedStudents.singleOrNull()
        return when (template) {
            SmsEventTemplate.ATTENDANCE_ALERT -> {
                if (singleStudent != null) {
                    "Assalamu Alaikum ${singleStudent.parentGuardianName.ifBlank { "Parent/Guardian" }}. " +
                        "$schoolName notifies you regarding ${singleStudent.fullName} (${singleStudent.className}) " +
                        "attendance${if (dateStr.isNotBlank()) " on $dateStr" else " today"}. Please contact the school office if needed."
                } else {
                    "Assalamu Alaikum Dear Parent/Guardian. $schoolName attendance alert" +
                        "${if (dateStr.isNotBlank()) " ($dateStr)" else ""}: Please ensure your ward arrives punctually or inform the class teacher of any absence."
                }
            }
            SmsEventTemplate.SCHOOL_ANNOUNCEMENT -> {
                "Assalamu Alaikum Dear Parent/Guardian. Notice from $schoolName (${schoolInfo.location}): " +
                    "Please be informed of our upcoming school assembly/PTA briefing. Contact ${schoolInfo.phoneNumber} for enquiries."
            }
            SmsEventTemplate.REPORT_CARD -> {
                if (singleStudent != null) {
                    "Assalamu Alaikum ${singleStudent.parentGuardianName.ifBlank { "Parent/Guardian" }}. " +
                        "The termly report card for ${singleStudent.fullName} (${singleStudent.className}) is now available for collection at $schoolName."
                } else {
                    "Assalamu Alaikum Dear Parent/Guardian. Termly academic report cards are now available for collection at $schoolName. Thank you."
                }
            }
            SmsEventTemplate.FEE_REMINDER -> {
                if (singleStudent != null) {
                    "Assalamu Alaikum ${singleStudent.parentGuardianName.ifBlank { "Parent/Guardian" }}. " +
                        "Tuition fee reminder from $schoolName regarding ${singleStudent.fullName} (${singleStudent.className}): " +
                        "please clear due/overdue school fee installments at the Admin Office. Contact ${schoolInfo.phoneNumber}."
                } else {
                    "Assalamu Alaikum Dear Parent/Guardian. Friendly tuition fee reminder from $schoolName: " +
                        "Please settle any due or overdue school fee installments at the Admin Office. Enquiries: ${schoolInfo.phoneNumber}."
                }
            }
            SmsEventTemplate.CUSTOM -> {
                "Assalamu Alaikum from $schoolName: "
            }
        }
    }

    /**
     * Play-Policy compliant SMS composer launch using ACTION_SENDTO (`smsto:`) without requesting SEND_SMS.
     * Returns true if the SMS app opened, false if no SMS handler exists (and copies message to clipboard).
     */
    fun launchSmsComposer(
        context: Context,
        phoneNumbers: List<String>,
        messageBody: String
    ): Boolean {
        val validPhones = phoneNumbers
            .map { cleanPhoneNumber(it) }
            .filter { isValidPhoneNumber(it) }
            .distinct()

        if (validPhones.isEmpty()) return false

        val recipientsUri = Uri.parse("smsto:${validPhones.joinToString(",")}")
        val intent = Intent(Intent.ACTION_SENDTO, recipientsUri).apply {
            putExtra("sms_body", messageBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            // Copy message to clipboard gracefully on devices/emulators without an SMS app
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("School SMS Message", messageBody))
            false
        }
    }
}
