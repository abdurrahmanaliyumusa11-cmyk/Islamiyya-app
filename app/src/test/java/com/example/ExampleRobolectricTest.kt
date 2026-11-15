package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassFeeSchedule
import com.example.data.model.FeePayment
import com.example.data.model.SchoolInfo
import com.example.data.model.SmsLog
import com.example.data.model.StaffRole
import com.example.data.model.Student
import com.example.data.model.StudentFeeInstallment
import com.example.data.model.StudentGrade
import com.example.data.model.Teacher
import com.example.ui.SchoolViewModel
import com.example.ui.components.computeTermTrends
import com.example.util.AdminAuthManager
import com.example.util.CsvRowStatus
import com.example.util.FeeHelper
import com.example.util.NotificationHelper
import com.example.util.OfficialPdfExporter
import com.example.util.ReportExporter
import com.example.util.SmsEventTemplate
import com.example.util.SmsHelper
import com.example.util.StudentCsvImporter
import com.example.util.StudentFeeStatus
import com.example.util.StudentIdCardGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read school app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Umuriddin Islamiyya", appName)
    }

    @Test
    fun `verify admin authentication default credentials`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authManager = AdminAuthManager(context)
        assertTrue("Default PIN 1234 should authenticate", authManager.login("1234"))
    }

    @Test
    fun `verify staff role assignment and access control permissions`() {
        assertTrue(StaffRole.ADMIN.canManageStaff)
        assertTrue(StaffRole.ADMIN.canDeleteRecords)
        assertTrue(StaffRole.ADMIN.canViewSensitiveStudentDetails)
        assertTrue(StaffRole.ADMIN.canSendSms)

        assertFalse(StaffRole.CLASS_TEACHER.canManageStaff)
        assertFalse(StaffRole.CLASS_TEACHER.canDeleteRecords)
        assertTrue(StaffRole.CLASS_TEACHER.canViewSensitiveStudentDetails)
        assertTrue(StaffRole.CLASS_TEACHER.canMarkAttendance)
        assertTrue(StaffRole.CLASS_TEACHER.canSendSms)

        assertFalse(StaffRole.SUBJECT_TEACHER.canManageStaff)
        assertFalse(StaffRole.SUBJECT_TEACHER.canViewSensitiveStudentDetails)
        assertFalse(StaffRole.SUBJECT_TEACHER.canMarkAttendance)
        assertTrue(StaffRole.SUBJECT_TEACHER.canRecordGrades)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val authManager = AdminAuthManager(context)
        assertTrue(
            authManager.loginWithRole(
                pinOrCode = "UIPS/T/001",
                role = StaffRole.CLASS_TEACHER,
                staffName = "Mallam Aliyu Danladi",
                expectedStaffId = "UIPS/T/001",
                assignedClass = "Primary 1"
            )
        )
        assertEquals(StaffRole.CLASS_TEACHER, authManager.currentRole.value)
        assertEquals("Mallam Aliyu Danladi", authManager.currentStaffName.value)
    }

    @Test
    fun `verify upcoming admission anniversary detection`() {
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val students = listOf(
            Student(
                id = 1,
                fullName = "Ahmad Muhammad Bello",
                age = 7,
                dateOfBirth = "2019-03-12",
                dateOfAdmission = "2025-09-08",
                gender = "Male",
                className = "Primary 2",
                registrationNumber = "UIPS/2025/001"
            ),
            Student(
                id = 2,
                fullName = "Fatima Zahra Usman",
                age = 6,
                dateOfBirth = "2020-05-18",
                dateOfAdmission = "2026-09-08", // same year -> 0 years completed
                gender = "Female",
                className = "Primary 1",
                registrationNumber = "UIPS/2026/002"
            ),
            Student(
                id = 3,
                fullName = "Zubairu Sani Garba",
                age = 11,
                dateOfBirth = "2015-11-14",
                dateOfAdmission = "2022-09-12",
                gender = "Male",
                className = "Primary 6",
                registrationNumber = "UIPS/2022/008"
            )
        )

        val upcoming = NotificationHelper.findUpcomingAnniversaries(students, refCal, windowDays = 7)
        assertEquals(2, upcoming.size)
        assertEquals("Ahmad Muhammad Bello", upcoming[0].student.fullName)
        assertEquals(1, upcoming[0].yearsCompleted)
        assertEquals(1, upcoming[0].daysUntil)
        assertEquals("Zubairu Sani Garba", upcoming[1].student.fullName)
        assertEquals(4, upcoming[1].yearsCompleted)
        assertEquals(5, upcoming[1].daysUntil)
    }

    @Test
    fun `verify performance trends computation and room persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val grades = listOf(
                StudentGrade(studentId = 1, termLabel = "2025 T1", subject = "Quran & Tajweed", score = 70, sortOrder = 1),
                StudentGrade(studentId = 1, termLabel = "2025 T1", subject = "Mathematics", score = 80, sortOrder = 1),
                StudentGrade(studentId = 1, termLabel = "2025 T2", subject = "Quran & Tajweed", score = 84, sortOrder = 2),
                StudentGrade(studentId = 1, termLabel = "2025 T2", subject = "Mathematics", score = 90, sortOrder = 2)
            )
            db.studentGradeDao().insertGrades(grades)
            val saved = db.studentGradeDao().getGradesForStudent(1).first()
            assertEquals(4, saved.size)

            val overall = computeTermTrends(saved, "Overall Avg")
            assertEquals(2, overall.size)
            assertEquals("2025 T1", overall[0].termLabel)
            assertEquals(75f, overall[0].averageScore, 0.01f)
            assertEquals("2025 T2", overall[1].termLabel)
            assertEquals(87f, overall[1].averageScore, 0.01f)

            val mathOnly = computeTermTrends(saved, "Mathematics")
            assertEquals(2, mathOnly.size)
            assertEquals(80f, mathOnly[0].averageScore, 0.01f)
            assertEquals(90f, mathOnly[1].averageScore, 0.01f)
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify sms template generation phone linking and sms log persistence`() = runBlocking {
        assertTrue(SmsHelper.isValidPhoneNumber("0803-456-7890"))
        assertEquals("08034567890", SmsHelper.cleanPhoneNumber(" 0803-456-7890 "))
        assertFalse(SmsHelper.isValidPhoneNumber("123"))

        val student = Student(
            id = 1,
            fullName = "Ahmad Muhammad Bello",
            age = 7,
            dateOfBirth = "2019-03-12",
            dateOfAdmission = "2025-09-08",
            gender = "Male",
            className = "Primary 2",
            registrationNumber = "UIPS/2025/001",
            parentGuardianName = "Alhaji Muhammad Bello",
            parentGuardianPhone = "08034567890"
        )

        val attendanceMsg = SmsHelper.buildTemplateMessage(
            template = SmsEventTemplate.ATTENDANCE_ALERT,
            schoolInfo = SchoolInfo(),
            selectedStudents = listOf(student),
            dateStr = "2026-10-04"
        )
        assertTrue(attendanceMsg.contains("Ahmad Muhammad Bello"))
        assertTrue(attendanceMsg.contains("Alhaji Muhammad Bello"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            db.smsLogDao().insertSmsLog(
                SmsLog(
                    eventType = "Attendance Alert",
                    recipientSummary = "Alhaji Muhammad Bello",
                    recipientPhones = "08034567890",
                    recipientCount = 1,
                    messageBody = attendanceMsg,
                    sentBy = "Administrator"
                )
            )
            val logs = db.smsLogDao().getAllSmsLogs().first()
            assertEquals(1, logs.size)
            assertEquals("08034567890", logs[0].recipientPhones)
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify student csv export generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val student = Student(
            id = 1,
            fullName = "Ahmad Muhammad Bello",
            age = 7,
            dateOfBirth = "2019-03-12",
            dateOfAdmission = "2025-09-08",
            gender = "Male",
            className = "Primary 2",
            registrationNumber = "UIPS/2025/001",
            parentGuardianName = "Alhaji Muhammad Bello",
            parentGuardianPhone = "08034567890",
            homeAddress = "Zango Marikita, Ungogo, Kano",
            admissionStatus = "Active"
        )
        val file = ReportExporter.exportStudentsToCsv(
            context = context,
            students = listOf(student),
            schoolInfo = SchoolInfo(),
            reportTitle = "TestReport"
        )
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun `verify attendance sheet export generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val record = AttendanceRecord(
            id = 1,
            date = "2026-10-04",
            className = "Primary 1",
            studentId = 101,
            studentName = "Fatima Zahra Usman",
            studentRegNo = "UIPS/2026/002",
            status = "Present",
            remarks = "On time",
            markedBy = "Mallam Aliyu Danladi"
        )
        val file = ReportExporter.exportAttendanceToCsv(
            context = context,
            className = "Primary 1",
            date = "2026-10-04",
            teacherName = "Mallam Aliyu Danladi",
            records = listOf(record),
            schoolInfo = SchoolInfo()
        )
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun `verify teacher data model creation with role`() {
        val teacher = Teacher(
            id = 1,
            fullName = "Mallam Aliyu Danladi",
            staffId = "UIPS/T/001",
            phoneNumber = "08031112233",
            gender = "Male",
            qualification = "NCE Arabic & Islamic Studies",
            assignedClass = "Primary 1",
            subjectSpecialization = "Arabic & Literacy",
            role = "Class Teacher"
        )
        assertEquals("UIPS/T/001", teacher.staffId)
        assertEquals("Primary 1", teacher.assignedClass)
        assertEquals("Class Teacher", teacher.role)
    }

    @Test
    fun `verify digital student id card qr roundtrip printable html and share png`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val student = Student(
            id = 42,
            fullName = "Amina Abdullahi Yusuf",
            age = 8,
            dateOfBirth = "2018-04-10",
            dateOfAdmission = "2025-09-01",
            gender = "Female",
            className = "Primary 3",
            registrationNumber = "UIPS/2025/042",
            parentGuardianName = "Malam Abdullahi Yusuf",
            parentGuardianPhone = "08031234567",
            admissionStatus = "Active"
        )

        // 1. Payload encode & parse
        val payload = StudentIdCardGenerator.encodeStudentQrPayload(student)
        assertEquals("UIPS_STUDENT|42|UIPS/2025/042", payload)

        val parsed = StudentIdCardGenerator.parseStudentQrPayload(payload)
        assertEquals(42L, parsed.studentId)
        assertEquals("UIPS/2025/042", parsed.registrationNumber)

        val bareRegParsed = StudentIdCardGenerator.parseStudentQrPayload("UIPS/2025/042")
        assertEquals(null, bareRegParsed.studentId)
        assertEquals("UIPS/2025/042", bareRegParsed.registrationNumber)

        // 2. QR Bitmap generation & ZXing decode roundtrip
        val qrBitmap = StudentIdCardGenerator.generateQrBitmap(payload, sizePx = 300)
        assertEquals(300, qrBitmap.width)
        assertEquals(300, qrBitmap.height)
        val decodedPayload = StudentIdCardGenerator.decodeQrFromBitmap(qrBitmap)
        assertEquals(payload, decodedPayload)

        // 3. Shareable PNG ID Card rendering & file export
        val pngFile = StudentIdCardGenerator.saveIdCardPngToCache(context, student, SchoolInfo())
        assertNotNull(pngFile)
        assertTrue(pngFile!!.exists())
        assertTrue(pngFile.length() > 1000)

        // 4. Printable HTML generation includes name, class, reg no, and embedded QR image
        val html = StudentIdCardGenerator.buildPrintableIdCardHtml(context, student, SchoolInfo())
        assertTrue(html.contains("Amina Abdullahi Yusuf"))
        assertTrue(html.contains("Primary 3"))
        assertTrue(html.contains("UIPS/2025/042"))
        assertTrue(html.contains("data:image/png;base64,"))

        // 5. Quick record retrieval via QR payload or reg no
        val roster = listOf(
            student,
            student.copy(id = 43, fullName = "Sadiq Ibrahim", registrationNumber = "UIPS/2025/043", className = "Primary 4")
        )
        assertEquals(student, SchoolViewModel.findStudentMatchingPayload(roster, payload))
        assertEquals(student, SchoolViewModel.findStudentMatchingPayload(roster, "UIPS/2025/042"))
        assertEquals(student, SchoolViewModel.findStudentMatchingPayload(roster, "42"))
    }

    @Test
    fun `verify naira fee calculation installments overdue reminders sms and csv export`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val student = Student(
            id = 10,
            fullName = "Zainab Musa Ibrahim",
            age = 8,
            dateOfBirth = "2018-05-10",
            dateOfAdmission = "2024-09-01",
            gender = "Female",
            className = "Primary 3",
            registrationNumber = "UIPS/2024/010",
            parentGuardianName = "Hajiya Aisha Ibrahim",
            parentGuardianPhone = "08039998877"
        )
        val schedule = ClassFeeSchedule(
            id = 1,
            className = "Primary 3",
            termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
            tuitionAmount = 25000L,
            defaultInstallmentCount = 2,
            firstDueDate = "2026-09-25",
            secondDueDate = "2026-10-25"
        )
        val generatedInsts = FeeHelper.buildInstallmentsForStudent(student.id, schedule)
            .mapIndexed { idx, inst -> inst.copy(id = (idx + 1).toLong()) }
        assertEquals(2, generatedInsts.size)
        assertEquals(25000L, generatedInsts.sumOf { it.amountDue })
        assertEquals("₦25,000", FeeHelper.formatNaira(25000L))

        // Partial payment clearing only 1st installment; reference date 2026-10-28 makes 2nd overdue
        val firstPayment = FeePayment(
            id = 1,
            studentId = student.id,
            installmentId = generatedInsts[0].id,
            termLabel = schedule.termLabel,
            amountPaid = generatedInsts[0].amountDue,
            paymentDate = "2026-09-24",
            paymentMethod = "Bank Transfer",
            receiptNumber = "RCP/2026/0001"
        )
        val overdueSummary = FeeHelper.computeStudentFeeSummary(
            student = student,
            installments = generatedInsts,
            payments = listOf(firstPayment),
            termLabel = schedule.termLabel,
            referenceDateStr = "2026-10-28",
            dueSoonWindowDays = 7
        )
        assertEquals(25000L, overdueSummary.totalDue)
        assertEquals(generatedInsts[0].amountDue, overdueSummary.totalPaid)
        assertEquals(generatedInsts[1].amountDue, overdueSummary.balance)
        assertEquals(StudentFeeStatus.OVERDUE, overdueSummary.status)
        assertEquals(1, overdueSummary.overdueCount)

        // Reference date 2026-10-20 puts 2nd installment in DUE_SOON window (5 days away)
        val dueSoonSummary = FeeHelper.computeStudentFeeSummary(
            student = student,
            installments = generatedInsts,
            payments = listOf(firstPayment),
            termLabel = schedule.termLabel,
            referenceDateStr = "2026-10-20",
            dueSoonWindowDays = 7
        )
        assertEquals(StudentFeeStatus.DUE_SOON, dueSoonSummary.status)
        assertEquals(1, FeeHelper.findDueAndOverdueReminders(listOf(dueSoonSummary)).size)

        val smsText = FeeHelper.buildFeeReminderSmsMessage(overdueSummary, SchoolInfo())
        assertTrue(smsText.contains("Zainab Musa Ibrahim"))
        assertTrue(smsText.contains("₦"))

        // Second payment clears remaining balance -> PAID
        val secondPayment = FeePayment(
            id = 2,
            studentId = student.id,
            installmentId = generatedInsts[1].id,
            termLabel = schedule.termLabel,
            amountPaid = generatedInsts[1].amountDue,
            paymentDate = "2026-10-29",
            paymentMethod = "Cash",
            receiptNumber = "RCP/2026/0002"
        )
        val paidSummary = FeeHelper.computeStudentFeeSummary(
            student = student,
            installments = generatedInsts,
            payments = listOf(firstPayment, secondPayment),
            termLabel = schedule.termLabel,
            referenceDateStr = "2026-10-30"
        )
        assertEquals(StudentFeeStatus.PAID, paidSummary.status)
        assertEquals(0L, paidSummary.balance)

        // Fee CSV Export
        val csvFile = ReportExporter.exportFeesToCsv(
            context = context,
            summaries = listOf(overdueSummary),
            schoolInfo = SchoolInfo(),
            reportTitle = "Test_Fees_Ledger"
        )
        assertNotNull(csvFile)
        assertTrue(csvFile!!.exists())
        val csvContent = csvFile.readText()
        assertTrue(csvContent.contains("Zainab Musa Ibrahim"))
        assertTrue(csvContent.contains("NGN"))
    }

    @Test
    fun `verify fee dao room persistence and staff role fee permissions`() = runBlocking {
        // RBAC Checks: Admin edits, Class Teacher views only, Subject Teacher blocked
        assertTrue(StaffRole.ADMIN.canViewFeeRecords)
        assertTrue(StaffRole.ADMIN.canManageFeeRecords)
        assertTrue(StaffRole.CLASS_TEACHER.canViewFeeRecords)
        assertFalse(StaffRole.CLASS_TEACHER.canManageFeeRecords)
        assertFalse(StaffRole.SUBJECT_TEACHER.canViewFeeRecords)
        assertFalse(StaffRole.SUBJECT_TEACHER.canManageFeeRecords)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val feeDao = db.feeDao()
            feeDao.insertClassFeeSchedule(
                ClassFeeSchedule(
                    className = "Primary 2",
                    termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
                    tuitionAmount = 22000L,
                    defaultInstallmentCount = 2
                )
            )
            feeDao.insertInstallment(
                StudentFeeInstallment(
                    studentId = 1L,
                    installmentTitle = "1st Installment",
                    amountDue = 11000L,
                    dueDate = "2026-09-25"
                )
            )
            feeDao.insertPayment(
                FeePayment(
                    studentId = 1L,
                    amountPaid = 11000L,
                    paymentDate = "2026-09-20",
                    paymentMethod = "POS",
                    receiptNumber = "RCP/2026/0101"
                )
            )

            assertEquals(1, feeDao.getAllClassFeeSchedules().first().size)
            assertEquals(22000L, feeDao.getAllClassFeeSchedules().first()[0].tuitionAmount)
            assertEquals(1, feeDao.getInstallmentsForStudent(1L).first().size)
            assertEquals("RCP/2026/0101", feeDao.getPaymentsForStudent(1L).first()[0].receiptNumber)
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify csv bulk student parser validation duplicates and room batch insert`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Sample template roundtrip
        val sampleFile = StudentCsvImporter.createSampleCsvFile(context, SchoolInfo())
        assertNotNull(sampleFile)
        assertTrue(sampleFile!!.exists())
        val samplePreview = StudentCsvImporter.parseAndValidate(
            csvText = sampleFile.readText(),
            existingStudents = emptyList(),
            currentYear = 2026,
            todayDateStr = "2026-09-01"
        )
        assertTrue(samplePreview.headerRecognized)
        assertEquals(3, samplePreview.validCount)
        assertEquals(0, samplePreview.errorCount)
        assertEquals("Bello, Sadiq Garba", samplePreview.validStudents[0].fullName)
        assertEquals("08035551122", samplePreview.validStudents[0].parentGuardianPhone)

        // 2. Quoted fields, BOM, school preamble, shorthand classes, duplicates, and invalid rows
        val existing = Student(
            id = 1,
            fullName = "Ahmad Muhammad Bello",
            age = 7,
            dateOfBirth = "2019-03-12",
            dateOfAdmission = "2025-09-08",
            gender = "Male",
            className = "Primary 2",
            registrationNumber = "UIPS/2025/001"
        )
        val rawCsv = "\uFEFF\"UMURIDDIN ISLAMIYYA PRIMARY SCHOOL\"\r\n" +
            "\"Generated: 15/11/2026\"\r\n\r\n" +
            "Student Name,Class,Sex,Age,DOB,Parent,Phone,Reg No\r\n" +
            "\"Suleiman, Maryam\",p3,F,,2018-05-10,Malam Suleiman,0803-111-2233,\r\n" +
            "Ahmad Duplicate,Primary 2,Male,7,2019-03-12,Alhaji Bello,08030001111,UIPS/2025/001\r\n" +
            ",Primary 1,Male,6,2020-01-01,Test Parent,08030002222,\r\n" +
            "Bashir Umar,Primary 4,Male,notanage,2016-01-01,Umar,08030003333,\r\n"

        val preview = StudentCsvImporter.parseAndValidate(
            csvText = rawCsv,
            existingStudents = listOf(existing),
            currentYear = 2026,
            todayDateStr = "2026-09-10"
        )
        assertTrue(preview.headerRecognized)
        assertEquals(1, preview.validCount)
        assertEquals(1, preview.duplicateCount)
        assertEquals(2, preview.errorCount)

        val validStudent = preview.validStudents.first()
        assertEquals("Suleiman, Maryam", validStudent.fullName)
        assertEquals("Primary 3", validStudent.className)
        assertEquals("Female", validStudent.gender)
        assertEquals(8, validStudent.age) // derived from 2026 - 2018
        assertEquals("08031112233", validStudent.parentGuardianPhone)
        assertTrue(validStudent.registrationNumber.startsWith("UIPS/2026/"))
        assertEquals(CsvRowStatus.DUPLICATE, preview.rows[1].status)
        assertEquals(CsvRowStatus.ERROR, preview.rows[2].status)
        assertEquals(CsvRowStatus.ERROR, preview.rows[3].status)

        // 3. Batch insert into Room returns IDs and persists students
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val insertedIds = db.studentDao().insertStudents(samplePreview.validStudents)
            assertEquals(3, insertedIds.size)
            assertTrue(insertedIds.all { it > 0L })
            val allInDb = db.studentDao().getAllStudentsDirect()
            assertEquals(3, allInDb.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify official pdf exporter builds fee receipts and ledger reports`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val schoolInfo = SchoolInfo()
        val student = Student(
            id = 42L,
            fullName = "Zainab Musa Ibrahim",
            age = 8,
            dateOfBirth = "2018-04-02",
            dateOfAdmission = "2024-09-09",
            gender = "Female",
            className = "Primary 3",
            registrationNumber = "UIPS/2024/042",
            parentGuardianName = "Alhaji Musa Ibrahim",
            parentGuardianPhone = "08035554433"
        )
        val payment = FeePayment(
            id = 10L,
            studentId = 42L,
            termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
            amountPaid = 12500L,
            paymentDate = "2026-09-18",
            paymentMethod = "Bank Transfer",
            receiptNumber = "RCP/2026/0042",
            recordedBy = "Admin",
            remarks = "Part tuition settlement"
        )
        val installments = listOf(
            StudentFeeInstallment(
                id = 1L,
                studentId = 42L,
                installmentTitle = "1st Installment",
                amountDue = 12500L,
                dueDate = "2026-09-15"
            ),
            StudentFeeInstallment(
                id = 2L,
                studentId = 42L,
                installmentTitle = "2nd Installment",
                amountDue = 12500L,
                dueDate = "2026-10-30"
            )
        )
        val summary = FeeHelper.computeStudentFeeSummary(
            student = student,
            installments = installments,
            payments = listOf(payment),
            termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
            referenceDateStr = "2026-09-20"
        )

        val expectedFileName = OfficialPdfExporter.buildReceiptPdfFileName(student, payment)
        assertEquals("Receipt_RCP_2026_0042_Zainab_Musa_Ibrahim.pdf", expectedFileName)

        // 1. Official Single Payment Receipt PDF
        val receiptPdf = OfficialPdfExporter.generateFeeReceiptPdf(
            context = context,
            student = student,
            payment = payment,
            summary = summary,
            schoolInfo = schoolInfo
        )
        assertNotNull(receiptPdf)
        assertTrue(receiptPdf!!.exists())
        assertTrue(receiptPdf.length() > 100L)
        assertEquals(expectedFileName, receiptPdf.name)
        assertEquals("receipts", receiptPdf.parentFile?.name)
        val receiptRaw = receiptPdf.readText()
        assertTrue(receiptRaw.startsWith("%PDF-"))
        assertTrue(receiptRaw.contains("%%EOF"))
        assertTrue(receiptRaw.contains("RCP/2026/0042"))

        // 2. Write PDF to target Uri via SAF stream helper
        val exportedCopy = File(context.cacheDir, "exported_copy.pdf")
        val copied = OfficialPdfExporter.writePdfFileToUri(context, receiptPdf, Uri.fromFile(exportedCopy))
        assertTrue(copied)
        assertTrue(exportedCopy.exists())
        assertEquals(receiptPdf.length(), exportedCopy.length())

        // 3. Multi-page summary report PDF
        val reportPdf = OfficialPdfExporter.generateFeeSummaryReportPdf(
            context = context,
            summaries = listOf(summary),
            schoolInfo = schoolInfo,
            reportTitle = "Tuition Fee Collection Ledger"
        )
        assertNotNull(reportPdf)
        assertTrue(reportPdf!!.exists())
        assertTrue(reportPdf.length() > 100L)
        assertTrue(reportPdf.name.endsWith(".pdf"))
        assertEquals("reports", reportPdf.parentFile?.name)
        assertTrue(reportPdf.readText().startsWith("%PDF-"))
    }
}
