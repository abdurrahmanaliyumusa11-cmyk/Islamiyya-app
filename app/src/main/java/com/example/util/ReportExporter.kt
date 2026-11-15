package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.model.FeePayment
import com.example.data.model.SchoolInfo
import com.example.data.model.Student
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun exportStudentsToCsv(
        context: Context,
        students: List<Student>,
        schoolInfo: SchoolInfo,
        reportTitle: String = "Student_List"
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "${reportTitle}_${timestamp}.csv"
            val file = File(context.cacheDir, filename)

            FileWriter(file).use { writer ->
                // Header
                writer.append("\"${schoolInfo.name}\"\n")
                writer.append("\"${schoolInfo.location}\"\n")
                writer.append("\"Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\"\n\n")

                // Column Headers
                writer.append("S/N,Registration No,Full Name,Class,Gender,Age,Date of Birth,Date of Admission,Parent/Guardian,Phone,Home Address,Status,Previous School\n")

                students.forEachIndexed { index, s ->
                    writer.append("${index + 1},")
                    writer.append("\"${escapeCsv(s.registrationNumber)}\",")
                    writer.append("\"${escapeCsv(s.fullName)}\",")
                    writer.append("\"${escapeCsv(s.className)}\",")
                    writer.append("\"${escapeCsv(s.gender)}\",")
                    writer.append("${s.age},")
                    writer.append("\"${escapeCsv(s.dateOfBirth)}\",")
                    writer.append("\"${escapeCsv(s.dateOfAdmission)}\",")
                    writer.append("\"${escapeCsv(s.parentGuardianName)}\",")
                    writer.append("\"${escapeCsv(s.parentGuardianPhone)}\",")
                    writer.append("\"${escapeCsv(s.homeAddress)}\",")
                    writer.append("\"${escapeCsv(s.admissionStatus)}\",")
                    writer.append("\"${escapeCsv(s.previousSchool)}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, file: File, title: String = "Share Student Records") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun printStudentProfile(context: Context, student: Student, schoolInfo: SchoolInfo) {
        val htmlContent = buildStudentProfileHtml(student, schoolInfo)
        printHtml(context, htmlContent, "Student_${student.registrationNumber.replace('/', '_')}")
    }

    fun printStudentsReport(
        context: Context,
        students: List<Student>,
        schoolInfo: SchoolInfo,
        reportTitle: String
    ) {
        val htmlContent = buildStudentsReportHtml(students, schoolInfo, reportTitle)
        printHtml(context, htmlContent, reportTitle.replace(' ', '_'))
    }

    private fun printHtml(context: Context, htmlContent: String, jobName: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("id", "print", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    fun printAttendanceSheet(
        context: Context,
        className: String,
        date: String,
        teacherName: String,
        records: List<com.example.data.model.AttendanceRecord>,
        schoolInfo: SchoolInfo
    ) {
        val htmlContent = buildAttendanceSheetHtml(className, date, teacherName, records, schoolInfo)
        printHtml(context, htmlContent, "Attendance_${className.replace(' ', '_')}_$date")
    }

    fun printFeeReceipt(
        context: Context,
        student: Student,
        payment: FeePayment,
        summary: StudentFeeSummary,
        schoolInfo: SchoolInfo
    ) {
        val htmlContent = buildFeeReceiptHtml(student, payment, summary, schoolInfo)
        printHtml(context, htmlContent, "Receipt_${payment.receiptNumber.replace('/', '_')}")
    }

    fun printFeeSummaryReport(
        context: Context,
        summaries: List<StudentFeeSummary>,
        schoolInfo: SchoolInfo,
        reportTitle: String
    ) {
        val htmlContent = buildFeeSummaryReportHtml(summaries, schoolInfo, reportTitle)
        printHtml(context, htmlContent, reportTitle.replace(' ', '_'))
    }

    fun exportFeesToCsv(
        context: Context,
        summaries: List<StudentFeeSummary>,
        schoolInfo: SchoolInfo,
        reportTitle: String = "Tuition_Fee_Report"
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "${reportTitle}_${timestamp}.csv"
            val file = File(context.cacheDir, filename)

            val totalDue = summaries.sumOf { it.totalDue }
            val totalPaid = summaries.sumOf { it.totalPaid }
            val totalBal = summaries.sumOf { it.balance }

            FileWriter(file).use { writer ->
                writer.append("\"${schoolInfo.name}\"\n")
                writer.append("\"${schoolInfo.location}\"\n")
                writer.append("\"$reportTitle | Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\"\n")
                writer.append("\"Total Billed (NGN): $totalDue | Total Collected (NGN): $totalPaid | Outstanding Balance (NGN): $totalBal\"\n\n")

                writer.append("S/N,Reg No,Student Name,Class,Parent/Guardian,Parent Phone,Term,Total Due (NGN),Total Paid (NGN),Balance (NGN),Next Due Date,Status\n")
                summaries.forEachIndexed { idx, s ->
                    writer.append("${idx + 1},")
                    writer.append("\"${escapeCsv(s.student.registrationNumber)}\",")
                    writer.append("\"${escapeCsv(s.student.fullName)}\",")
                    writer.append("\"${escapeCsv(s.student.className)}\",")
                    writer.append("\"${escapeCsv(s.student.parentGuardianName)}\",")
                    writer.append("\"${escapeCsv(s.student.parentGuardianPhone)}\",")
                    writer.append("\"${escapeCsv(s.termLabel)}\",")
                    writer.append("${s.totalDue},")
                    writer.append("${s.totalPaid},")
                    writer.append("${s.balance},")
                    writer.append("\"${escapeCsv(s.nextDueDate ?: "-")}\",")
                    writer.append("\"${escapeCsv(s.status.label)}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportAttendanceToCsv(
        context: Context,
        className: String,
        date: String,
        teacherName: String,
        records: List<com.example.data.model.AttendanceRecord>,
        schoolInfo: SchoolInfo
    ): File? {
        return try {
            val filename = "Attendance_${className.replace(' ', '_')}_${date}.csv"
            val file = File(context.cacheDir, filename)

            FileWriter(file).use { writer ->
                writer.append("\"${schoolInfo.name}\"\n")
                writer.append("\"CLASS ATTENDANCE SHEET - $className\"\n")
                writer.append("\"Date: $date | Class Teacher: $teacherName\"\n\n")

                val presentCount = records.count { it.status == "Present" }
                val absentCount = records.count { it.status == "Absent" }
                val lateCount = records.count { it.status == "Late" }
                val excusedCount = records.count { it.status == "Excused" }
                writer.append("\"Summary: Total: ${records.size} | Present: $presentCount | Absent: $absentCount | Late: $lateCount | Excused: $excusedCount\"\n\n")

                writer.append("S/N,Registration No,Student Name,Status,Remarks\n")
                records.forEachIndexed { idx, r ->
                    writer.append("${idx + 1},")
                    writer.append("\"${escapeCsv(r.studentRegNo)}\",")
                    writer.append("\"${escapeCsv(r.studentName)}\",")
                    writer.append("\"${escapeCsv(r.status)}\",")
                    writer.append("\"${escapeCsv(r.remarks)}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildAttendanceSheetHtml(
        className: String,
        date: String,
        teacherName: String,
        records: List<com.example.data.model.AttendanceRecord>,
        schoolInfo: SchoolInfo
    ): String {
        val presentCount = records.count { it.status == "Present" }
        val absentCount = records.count { it.status == "Absent" }
        val lateCount = records.count { it.status == "Late" }
        val excusedCount = records.count { it.status == "Excused" }
        val rate = if (records.isNotEmpty()) (presentCount * 100 / records.size) else 0

        val rows = StringBuilder()
        records.forEachIndexed { idx, r ->
            val statusColor = when (r.status) {
                "Present" -> "#198754"
                "Absent" -> "#DC3545"
                "Late" -> "#FD7E14"
                "Excused" -> "#0D6EFD"
                else -> "#333333"
            }
            val statusBg = when (r.status) {
                "Present" -> "#D1E7DD"
                "Absent" -> "#F8D7DA"
                "Late" -> "#FFE5D0"
                "Excused" -> "#CFE2FF"
                else -> "#EEEEEE"
            }
            rows.append("""
            <tr>
                <td style="text-align: center;">${idx + 1}</td>
                <td><strong>${r.studentRegNo}</strong></td>
                <td>${r.studentName}</td>
                <td style="text-align: center;"><span style="color: $statusColor; background: $statusBg; font-weight: bold; padding: 3px 8px; border-radius: 4px; font-size: 11px;">${r.status}</span></td>
                <td>${r.remarks}</td>
            </tr>
            """)
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: Arial, sans-serif; margin: 15px; color: #1a2a22; }
                .school-header { text-align: center; border-bottom: 2px solid #0B5345; padding-bottom: 8px; margin-bottom: 12px; }
                .title { font-size: 19px; font-weight: bold; color: #0B5345; margin: 0; }
                .subtitle { font-size: 12px; color: #555; margin: 2px 0; }
                .sheet-info-bar { background: #0B5345; color: #fff; padding: 8px 12px; font-size: 13px; font-weight: bold; display: flex; justify-content: space-between; border-radius: 4px; margin-bottom: 10px; }
                .stats-bar { display: flex; justify-content: space-around; background: #f0f7f3; padding: 8px; border: 1px solid #d0e2d8; border-radius: 4px; margin-bottom: 12px; font-size: 12px; font-weight: bold; }
                table { width: 100%; border-collapse: collapse; font-size: 12px; }
                th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; }
                th { background-color: #f7faf8; color: #0B5345; font-weight: bold; }
                tr:nth-child(even) { background-color: #fafafa; }
                .footer { margin-top: 30px; display: flex; justify-content: space-between; font-size: 11px; color: #555; }
                .sig { width: 180px; border-top: 1px solid #333; text-align: center; padding-top: 4px; }
            </style>
        </head>
        <body>
            <div class="school-header">
                <div class="title">${schoolInfo.name}</div>
                <div class="subtitle">${schoolInfo.location}</div>
                <div class="subtitle">Official Daily Class Attendance Register</div>
            </div>

            <div class="sheet-info-bar">
                <span>Class: $className</span>
                <span>Date: $date</span>
                <span>Teacher: $teacherName</span>
            </div>

            <div class="stats-bar">
                <span>Total: ${records.size}</span>
                <span style="color: #198754;">Present: $presentCount</span>
                <span style="color: #DC3545;">Absent: $absentCount</span>
                <span style="color: #FD7E14;">Late: $lateCount</span>
                <span style="color: #0D6EFD;">Excused: $excusedCount</span>
                <span>Attendance Rate: $rate%</span>
            </div>

            <table>
                <thead>
                    <tr>
                        <th style="width: 30px;">#</th>
                        <th>Reg No</th>
                        <th>Student Name</th>
                        <th style="text-align: center; width: 90px;">Status</th>
                        <th>Teacher Remarks</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>

            <div class="footer">
                <div class="sig">Class Teacher's Signature</div>
                <div class="sig">Headmaster / Admin Signature</div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"")
    }

    private fun buildStudentProfileHtml(student: Student, schoolInfo: SchoolInfo): String {
        val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    margin: 20px;
                    color: #1a2a22;
                    background-color: #fff;
                }
                .school-header {
                    text-align: center;
                    border-bottom: 3px double #0B5345;
                    padding-bottom: 14px;
                    margin-bottom: 20px;
                }
                .school-title {
                    font-size: 22px;
                    font-weight: bold;
                    color: #0B5345;
                    margin: 0;
                    letter-spacing: 0.5px;
                }
                .school-subtitle {
                    font-size: 13px;
                    color: #555;
                    margin: 4px 0;
                }
                .school-motto {
                    font-size: 12px;
                    font-style: italic;
                    color: #C5A059;
                    font-weight: 600;
                }
                .badge-card {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    background: #f4f8f6;
                    border: 1px solid #d0e2d8;
                    border-radius: 8px;
                    padding: 16px;
                    margin-bottom: 20px;
                }
                .student-name {
                    font-size: 20px;
                    font-weight: bold;
                    color: #0B5345;
                    margin: 0 0 6px 0;
                }
                .reg-tag {
                    display: inline-block;
                    background: #0B5345;
                    color: #fff;
                    font-size: 13px;
                    font-weight: bold;
                    padding: 4px 10px;
                    border-radius: 4px;
                }
                .status-tag {
                    display: inline-block;
                    background: #D4EDE5;
                    color: #022B22;
                    font-size: 12px;
                    font-weight: bold;
                    padding: 4px 10px;
                    border-radius: 4px;
                    margin-left: 8px;
                }
                .section-header {
                    font-size: 15px;
                    font-weight: bold;
                    color: #0B5345;
                    border-bottom: 1.5px solid #0B5345;
                    padding-bottom: 4px;
                    margin-top: 18px;
                    margin-bottom: 10px;
                    text-transform: uppercase;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 12px;
                }
                th, td {
                    padding: 8px 12px;
                    text-align: left;
                    font-size: 13px;
                    border-bottom: 1px solid #eef2f0;
                }
                th {
                    width: 35%;
                    color: #445;
                    font-weight: 600;
                    background-color: #fafcfb;
                }
                td {
                    color: #111;
                    font-weight: 500;
                }
                .footer {
                    margin-top: 40px;
                    display: flex;
                    justify-content: space-between;
                    font-size: 12px;
                    color: #555;
                    border-top: 1px solid #ccc;
                    padding-top: 12px;
                }
                .signature-box {
                    text-align: center;
                    width: 200px;
                    border-top: 1px solid #333;
                    padding-top: 4px;
                    margin-top: 40px;
                }
            </style>
        </head>
        <body>
            <div class="school-header">
                <div class="school-title">${schoolInfo.name}</div>
                <div class="school-subtitle">${schoolInfo.location}</div>
                <div class="school-subtitle">Tel: ${schoolInfo.phoneNumber} | Email: ${schoolInfo.email}</div>
                <div class="school-motto">"${schoolInfo.motto}"</div>
            </div>

            <div class="badge-card">
                <div>
                    <div class="student-name">${student.fullName}</div>
                    <span class="reg-tag">${student.registrationNumber}</span>
                    <span class="status-tag">${student.admissionStatus}</span>
                </div>
            </div>

            <div class="section-header">Academic Information</div>
            <table>
                <tr><th>Class</th><td>${student.className}</td></tr>
                <tr><th>Date of Admission</th><td>${student.dateOfAdmission}</td></tr>
                <tr><th>Previous School</th><td>${if (student.previousSchool.isNotBlank()) student.previousSchool else "N/A"}</td></tr>
            </table>

            <div class="section-header">Personal Information</div>
            <table>
                <tr><th>Gender</th><td>${student.gender}</td></tr>
                <tr><th>Age</th><td>${student.age} Years</td></tr>
                <tr><th>Date of Birth</th><td>${student.dateOfBirth}</td></tr>
            </table>

            <div class="section-header">Parent / Guardian Information</div>
            <table>
                <tr><th>Parent / Guardian</th><td>${if (student.parentGuardianName.isNotBlank()) student.parentGuardianName else "N/A"}</td></tr>
                <tr><th>Phone Number</th><td>${if (student.parentGuardianPhone.isNotBlank()) student.parentGuardianPhone else "N/A"}</td></tr>
                <tr><th>Residential Address</th><td>${if (student.homeAddress.isNotBlank()) student.homeAddress else "N/A"}</td></tr>
            </table>

            ${if (student.additionalNotes.isNotBlank()) """
            <div class="section-header">Administrative Remarks / Notes</div>
            <p style="font-size: 13px; line-height: 1.5; padding: 6px 12px; background: #fafcfb; border-left: 3px solid #0B5345;">
                ${student.additionalNotes}
            </p>
            """ else ""}

            <div class="footer">
                <div>Official Document Issued: $formattedDate</div>
                <div class="signature-box">Headmaster / Administrator Stamp</div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildStudentsReportHtml(
        students: List<Student>,
        schoolInfo: SchoolInfo,
        reportTitle: String
    ): String {
        val formattedDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())
        val rows = StringBuilder()

        students.forEachIndexed { idx, s ->
            rows.append("""
            <tr>
                <td style="text-align: center;">${idx + 1}</td>
                <td><strong>${s.registrationNumber}</strong></td>
                <td>${s.fullName}</td>
                <td>${s.className}</td>
                <td>${s.gender}</td>
                <td>${s.age} yrs</td>
                <td>${s.dateOfAdmission}</td>
                <td>${s.parentGuardianName} (${s.parentGuardianPhone})</td>
                <td><span style="font-size: 11px; padding: 2px 6px; background: #e0f0ea; border-radius: 3px;">${s.admissionStatus}</span></td>
            </tr>
            """)
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 15px;
                    color: #1a2a22;
                }
                .school-header {
                    text-align: center;
                    border-bottom: 2px solid #0B5345;
                    padding-bottom: 10px;
                    margin-bottom: 16px;
                }
                .title {
                    font-size: 20px;
                    font-weight: bold;
                    color: #0B5345;
                    margin: 0;
                }
                .subtitle {
                    font-size: 12px;
                    color: #555;
                    margin: 3px 0;
                }
                .report-title-bar {
                    background: #0B5345;
                    color: #fff;
                    padding: 8px 14px;
                    font-size: 14px;
                    font-weight: bold;
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 12px;
                    border-radius: 4px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 11px;
                }
                th, td {
                    border: 1px solid #ccc;
                    padding: 6px 8px;
                    text-align: left;
                }
                th {
                    background-color: #f0f7f3;
                    color: #0B5345;
                    font-weight: bold;
                }
                tr:nth-child(even) {
                    background-color: #fafafa;
                }
                .footer {
                    margin-top: 20px;
                    font-size: 11px;
                    color: #666;
                    text-align: right;
                }
            </style>
        </head>
        <body>
            <div class="school-header">
                <div class="title">${schoolInfo.name}</div>
                <div class="subtitle">${schoolInfo.location}</div>
                <div class="subtitle">Official Student Administration Report</div>
            </div>

            <div class="report-title-bar">
                <span>$reportTitle (Total: ${students.size})</span>
                <span>$formattedDate</span>
            </div>

            <table>
                <thead>
                    <tr>
                        <th style="width: 30px;">#</th>
                        <th>Reg No</th>
                        <th>Student Name</th>
                        <th>Class</th>
                        <th>Gender</th>
                        <th>Age</th>
                        <th>Admission Date</th>
                        <th>Parent / Contact</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>

            <div class="footer">
                Report generated by Umuriddin Islamiyya School Record System &bull; Total Students: ${students.size}
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildFeeReceiptHtml(
        student: Student,
        payment: FeePayment,
        summary: StudentFeeSummary,
        schoolInfo: SchoolInfo
    ): String {
        val generatedOn = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: Arial, sans-serif; margin: 24px; color: #1a2a22; }
                .receipt-box { border: 2px solid #0B5345; border-radius: 12px; overflow: hidden; max-width: 640px; margin: 0 auto; }
                .header { background: #0B5345; color: #fff; padding: 16px; text-align: center; border-bottom: 4px solid #C5A059; }
                .title { font-size: 19px; font-weight: bold; margin: 0; }
                .sub { font-size: 11px; color: #D4EDE5; margin-top: 3px; }
                .badge { display: inline-block; background: #C5A059; color: #281E04; font-weight: bold; font-size: 11px; padding: 3px 10px; border-radius: 12px; margin-top: 8px; }
                .content { padding: 18px; }
                .meta-row { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #e0e7e3; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 13px; }
                th, td { padding: 8px 10px; border-bottom: 1px solid #eef2f0; text-align: left; }
                th { width: 38%; color: #445; background: #f8fbf9; }
                .amount-box { background: #f0f7f3; border: 1.5px solid #0B5345; border-radius: 8px; padding: 12px; margin: 14px 0; display: flex; justify-content: space-between; align-items: center; }
                .amount-val { font-size: 22px; font-weight: 800; color: #0B5345; }
                .footer { display: flex; justify-content: space-between; margin-top: 28px; font-size: 11px; color: #555; }
                .sig { width: 190px; border-top: 1px solid #333; text-align: center; padding-top: 4px; }
            </style>
        </head>
        <body>
            <div class="receipt-box">
                <div class="header">
                    <div class="title">${schoolInfo.name}</div>
                    <div class="sub">${schoolInfo.location} &bull; Tel: ${schoolInfo.phoneNumber}</div>
                    <div class="badge">OFFICIAL TUITION FEE RECEIPT</div>
                </div>
                <div class="content">
                    <div class="meta-row">
                        <span><strong>Receipt No:</strong> ${payment.receiptNumber}</span>
                        <span><strong>Payment Date:</strong> ${payment.paymentDate}</span>
                    </div>
                    <table>
                        <tr><th>Student Name</th><td><strong>${student.fullName}</strong></td></tr>
                        <tr><th>Registration No</th><td>${student.registrationNumber}</td></tr>
                        <tr><th>Class Enrolled</th><td>${student.className}</td></tr>
                        <tr><th>Academic Term</th><td>${payment.termLabel}</td></tr>
                        <tr><th>Payment Method</th><td>${payment.paymentMethod}</td></tr>
                        <tr><th>Recorded By</th><td>${payment.recordedBy}</td></tr>
                        ${if (payment.remarks.isNotBlank()) "<tr><th>Remarks</th><td>${payment.remarks}</td></tr>" else ""}
                    </table>

                    <div class="amount-box">
                        <div>
                            <div style="font-size:11px; color:#556; font-weight:bold;">AMOUNT PAID THIS RECEIPT</div>
                            <div style="font-size:11px; color:#556;">Total Paid: ${FeeHelper.formatNaira(summary.totalPaid)} / ${FeeHelper.formatNaira(summary.totalDue)}</div>
                        </div>
                        <div class="amount-val">${FeeHelper.formatNaira(payment.amountPaid)}</div>
                    </div>

                    <div class="meta-row" style="border:none;">
                        <span><strong>Remaining Term Balance:</strong> ${FeeHelper.formatNaira(summary.balance)}</span>
                        <span><strong>Account Status:</strong> ${summary.status.label.uppercase(Locale.ROOT)}</span>
                    </div>

                    <div class="footer">
                        <div>Issued: $generatedOn</div>
                        <div class="sig">Bursar / Administrator Signature</div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildFeeSummaryReportHtml(
        summaries: List<StudentFeeSummary>,
        schoolInfo: SchoolInfo,
        reportTitle: String
    ): String {
        val formattedDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())
        val totalDue = summaries.sumOf { it.totalDue }
        val totalPaid = summaries.sumOf { it.totalPaid }
        val totalBalance = summaries.sumOf { it.balance }

        val rows = StringBuilder()
        summaries.forEachIndexed { idx, s ->
            val badgeBg = when (s.status) {
                StudentFeeStatus.PAID -> "#D1E7DD"
                StudentFeeStatus.OVERDUE -> "#F8D7DA"
                StudentFeeStatus.DUE_SOON, StudentFeeStatus.PARTIAL -> "#FFE5D0"
                else -> "#EEEEEE"
            }
            val badgeColor = when (s.status) {
                StudentFeeStatus.PAID -> "#198754"
                StudentFeeStatus.OVERDUE -> "#DC3545"
                StudentFeeStatus.DUE_SOON, StudentFeeStatus.PARTIAL -> "#9A4D00"
                else -> "#333333"
            }
            rows.append("""
            <tr>
                <td style="text-align:center;">${idx + 1}</td>
                <td><strong>${s.student.registrationNumber}</strong></td>
                <td>${s.student.fullName}</td>
                <td>${s.student.className}</td>
                <td style="text-align:right;">${FeeHelper.formatNaira(s.totalDue)}</td>
                <td style="text-align:right;">${FeeHelper.formatNaira(s.totalPaid)}</td>
                <td style="text-align:right;"><strong>${FeeHelper.formatNaira(s.balance)}</strong></td>
                <td style="text-align:center;">${s.nextDueDate ?: "-"}</td>
                <td style="text-align:center;"><span style="background:$badgeBg; color:$badgeColor; font-weight:bold; font-size:10px; padding:2px 6px; border-radius:4px;">${s.status.label}</span></td>
            </tr>
            """)
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: Arial, sans-serif; margin: 16px; color: #1a2a22; }
                .school-header { text-align: center; border-bottom: 2px solid #0B5345; padding-bottom: 8px; margin-bottom: 14px; }
                .title { font-size: 19px; font-weight: bold; color: #0B5345; margin: 0; }
                .subtitle { font-size: 12px; color: #555; margin: 2px 0; }
                .bar { background: #0B5345; color: #fff; padding: 8px 12px; font-size: 13px; font-weight: bold; display: flex; justify-content: space-between; border-radius: 4px; margin-bottom: 10px; }
                .totals { display: flex; justify-content: space-around; background: #f0f7f3; border: 1px solid #d0e2d8; border-radius: 4px; padding: 8px; font-size: 12px; font-weight: bold; margin-bottom: 12px; }
                table { width: 100%; border-collapse: collapse; font-size: 11px; }
                th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; }
                th { background: #f7faf8; color: #0B5345; }
                tr:nth-child(even) { background: #fafafa; }
            </style>
        </head>
        <body>
            <div class="school-header">
                <div class="title">${schoolInfo.name}</div>
                <div class="subtitle">${schoolInfo.location}</div>
                <div class="subtitle">Official Tuition &amp; Fee Collection Report</div>
            </div>
            <div class="bar">
                <span>$reportTitle (${summaries.size} Students)</span>
                <span>$formattedDate</span>
            </div>
            <div class="totals">
                <span>Total Expected: ${FeeHelper.formatNaira(totalDue)}</span>
                <span style="color:#198754;">Collected: ${FeeHelper.formatNaira(totalPaid)}</span>
                <span style="color:#DC3545;">Outstanding: ${FeeHelper.formatNaira(totalBalance)}</span>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Reg No</th>
                        <th>Student Name</th>
                        <th>Class</th>
                        <th style="text-align:right;">Total Due</th>
                        <th style="text-align:right;">Paid</th>
                        <th style="text-align:right;">Balance</th>
                        <th style="text-align:center;">Next Due</th>
                        <th style="text-align:center;">Status</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        </body>
        </html>
        """.trimIndent()
    }
}
