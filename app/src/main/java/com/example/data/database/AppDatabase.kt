package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AttendanceDao
import com.example.data.dao.ClassDao
import com.example.data.dao.FeeDao
import com.example.data.dao.SmsLogDao
import com.example.data.dao.StudentDao
import com.example.data.dao.StudentGradeDao
import com.example.data.dao.TeacherDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassFeeSchedule
import com.example.data.model.FeePayment
import com.example.data.model.SchoolClass
import com.example.data.model.SmsLog
import com.example.data.model.Student
import com.example.data.model.StudentFeeInstallment
import com.example.data.model.StudentGrade
import com.example.data.model.Teacher
import com.example.util.FeeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Database(
    entities = [
        Student::class,
        SchoolClass::class,
        Teacher::class,
        AttendanceRecord::class,
        StudentGrade::class,
        SmsLog::class,
        ClassFeeSchedule::class,
        StudentFeeInstallment::class,
        FeePayment::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun classDao(): ClassDao
    abstract fun teacherDao(): TeacherDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun studentGradeDao(): StudentGradeDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun feeDao(): FeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_fee_schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `className` TEXT NOT NULL,
                        `termLabel` TEXT NOT NULL,
                        `tuitionAmount` INTEGER NOT NULL,
                        `defaultInstallmentCount` INTEGER NOT NULL,
                        `firstDueDate` TEXT NOT NULL,
                        `secondDueDate` TEXT NOT NULL,
                        `thirdDueDate` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `student_fee_installments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studentId` INTEGER NOT NULL,
                        `termLabel` TEXT NOT NULL,
                        `installmentTitle` TEXT NOT NULL,
                        `amountDue` INTEGER NOT NULL,
                        `dueDate` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fee_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studentId` INTEGER NOT NULL,
                        `installmentId` INTEGER,
                        `termLabel` TEXT NOT NULL,
                        `amountPaid` INTEGER NOT NULL,
                        `paymentDate` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `receiptNumber` TEXT NOT NULL,
                        `recordedBy` TEXT NOT NULL,
                        `remarks` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "umuriddin_school_db"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            prepopulateDatabase(database)
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            ensureFeeSeedDataIfEmpty(database)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateDatabase(db: AppDatabase) {
            // Seed Teachers with Roles
            val initialTeachers = listOf(
                Teacher(id = 1, fullName = "Mallam Aliyu Danladi", staffId = "UIPS/T/001", phoneNumber = "08031112233", gender = "Male", qualification = "NCE Islamic Studies", assignedClass = "Primary 1", subjectSpecialization = "Arabic & Literacy", role = "Class Teacher"),
                Teacher(id = 2, fullName = "Ustaz Ibrahim Bello", staffId = "UIPS/T/002", phoneNumber = "08022223344", gender = "Male", qualification = "B.Ed Islamic Studies (BUK)", assignedClass = "Primary 2", subjectSpecialization = "Quranic Recitation & Fiqh", role = "Class Teacher"),
                Teacher(id = 3, fullName = "Malama Zainab Haruna", staffId = "UIPS/T/003", phoneNumber = "08133334455", gender = "Female", qualification = "NCE Early Childhood & Arabic", assignedClass = "Primary 3", subjectSpecialization = "Hadith & English Studies", role = "Class Teacher"),
                Teacher(id = 4, fullName = "Ustaz Mahmud Sanusi", staffId = "UIPS/T/004", phoneNumber = "08044445566", gender = "Male", qualification = "Diploma in Shari'ah & Arabic", assignedClass = "Primary 4", subjectSpecialization = "Arabic Grammar & Basic Science", role = "Subject Teacher"),
                Teacher(id = 5, fullName = "Mallam Sulaiman Umar", staffId = "UIPS/T/005", phoneNumber = "08055556677", gender = "Male", qualification = "B.Sc Ed Mathematics", assignedClass = "Primary 5", subjectSpecialization = "Mathematics & Islamic Studies", role = "Subject Teacher"),
                Teacher(id = 6, fullName = "Ustaz Mukhtar Garba", staffId = "UIPS/T/006", phoneNumber = "08066667788", gender = "Male", qualification = "NCE Islamic Studies (FCE Kano)", assignedClass = "Primary 6", subjectSpecialization = "Senior Islamic Studies Prep", role = "Admin")
            )
            db.teacherDao().insertTeachers(initialTeachers)

            // Seed Classes
            val initialClasses = listOf(
                SchoolClass(id = 1, name = "Primary 1", section = "A", classTeacher = "Mallam Aliyu Danladi", capacity = 40, description = "Foundation Islamic & Western Education"),
                SchoolClass(id = 2, name = "Primary 2", section = "A", classTeacher = "Ustaz Ibrahim Bello", capacity = 40, description = "Quranic recitation & Core Subjects"),
                SchoolClass(id = 3, name = "Primary 3", section = "A", classTeacher = "Malama Zainab Haruna", capacity = 40, description = "Hadeeth, Fiqh & General Literacy"),
                SchoolClass(id = 4, name = "Primary 4", section = "A", classTeacher = "Ustaz Mahmud Sanusi", capacity = 40, description = "Arabic Grammar & Sciences"),
                SchoolClass(id = 5, name = "Primary 5", section = "A", classTeacher = "Mallam Sulaiman Umar", capacity = 40, description = "Advanced Islamic Studies & Mathematics"),
                SchoolClass(id = 6, name = "Primary 6", section = "A", classTeacher = "Ustaz Mukhtar Garba", capacity = 40, description = "Graduation class & National exams prep")
            )
            db.classDao().insertClasses(initialClasses)

            // Seed initial realistic students
            val initialStudents = listOf(
                Student(
                    fullName = "Ahmad Muhammad Bello",
                    age = 7,
                    dateOfBirth = "2019-03-12",
                    dateOfAdmission = "2025-09-08",
                    gender = "Male",
                    className = "Primary 2",
                    registrationNumber = "UIPS/2025/001",
                    parentGuardianName = "Alhaji Muhammad Bello",
                    parentGuardianPhone = "08034567890",
                    homeAddress = "No. 14 Zango Marikita, Ungogo L.G.A., Kano",
                    previousSchool = "Nurul Huda Kindergarten, Kano",
                    admissionStatus = "Active",
                    additionalNotes = "Excels in Quran memorization (Juz Amma). High attendance."
                ),
                Student(
                    fullName = "Fatima Zahra Usman",
                    age = 6,
                    dateOfBirth = "2020-05-18",
                    dateOfAdmission = "2026-01-10",
                    gender = "Female",
                    className = "Primary 1",
                    registrationNumber = "UIPS/2026/002",
                    parentGuardianName = "Malam Usman Abdullahi",
                    parentGuardianPhone = "08023456789",
                    homeAddress = "Koki Ward, Near Central Mosque, Ungogo, Kano",
                    previousSchool = "Al-Bayan Early Learning, Ungogo",
                    admissionStatus = "Active",
                    additionalNotes = "New admission for current academic session."
                ),
                Student(
                    fullName = "Ibrahim Khalil Danbatta",
                    age = 10,
                    dateOfBirth = "2016-08-22",
                    dateOfAdmission = "2023-09-15",
                    gender = "Male",
                    className = "Primary 5",
                    registrationNumber = "UIPS/2023/045",
                    parentGuardianName = "Dr. Khalil Danbatta",
                    parentGuardianPhone = "08098765432",
                    homeAddress = "Plot 8 Zango Gabas, Ungogo, Kano",
                    previousSchool = "Al-Iman Academy, Kano City",
                    admissionStatus = "Active",
                    additionalNotes = "Class prefect. Active in inter-school Quranic competition."
                ),
                Student(
                    fullName = "Aisha Umar Faruq",
                    age = 8,
                    dateOfBirth = "2018-01-30",
                    dateOfAdmission = "2024-09-02",
                    gender = "Female",
                    className = "Primary 3",
                    registrationNumber = "UIPS/2024/019",
                    parentGuardianName = "Umar Faruq Sanusi",
                    parentGuardianPhone = "08145678901",
                    homeAddress = "Marikita Road, Ungogo L.G.A., Kano",
                    previousSchool = "Darul Arqam Nursery, Kano",
                    admissionStatus = "Active",
                    additionalNotes = "Consistently tops Arabic and English classes."
                ),
                Student(
                    fullName = "Zubairu Sani Garba",
                    age = 11,
                    dateOfBirth = "2015-11-14",
                    dateOfAdmission = "2022-09-12",
                    gender = "Male",
                    className = "Primary 6",
                    registrationNumber = "UIPS/2022/008",
                    parentGuardianName = "Alhaji Sani Garba",
                    parentGuardianPhone = "08051234567",
                    homeAddress = "No. 55 Ungogo Central, Kano",
                    previousSchool = "Direct Entry",
                    admissionStatus = "Active",
                    additionalNotes = "Preparing for State Common Entrance and Islamic Certificate exams."
                ),
                Student(
                    fullName = "Khadija Mustapha Aliyu",
                    age = 9,
                    dateOfBirth = "2017-07-04",
                    dateOfAdmission = "2024-01-15",
                    gender = "Female",
                    className = "Primary 4",
                    registrationNumber = "UIPS/2024/033",
                    parentGuardianName = "Engr. Mustapha Aliyu",
                    parentGuardianPhone = "08067890123",
                    homeAddress = "Zango Yamma, Ungogo, Kano",
                    previousSchool = "Al-Huda Islamic School, Fagge",
                    admissionStatus = "Active",
                    additionalNotes = "Transferred from Fagge in 2024. Excellent conduct."
                ),
                Student(
                    fullName = "Abubakar Saddiq Tijjani",
                    age = 6,
                    dateOfBirth = "2020-02-15",
                    dateOfAdmission = "2026-02-01",
                    gender = "Male",
                    className = "Primary 1",
                    registrationNumber = "UIPS/2026/003",
                    parentGuardianName = "Malam Tijjani Idris",
                    parentGuardianPhone = "08123456780",
                    homeAddress = "Ungogo Town Market Road, Kano",
                    previousSchool = "Home Tutoring",
                    admissionStatus = "Active",
                    additionalNotes = "Recently enrolled into Primary 1. Quick learner."
                ),
                Student(
                    fullName = "Maryam Kabir Danladi",
                    age = 7,
                    dateOfBirth = "2019-09-09",
                    dateOfAdmission = "2025-09-10",
                    gender = "Female",
                    className = "Primary 2",
                    registrationNumber = "UIPS/2025/012",
                    parentGuardianName = "Kabir Danladi",
                    parentGuardianPhone = "08076543210",
                    homeAddress = "No. 22 Zango Marikita, Kano",
                    previousSchool = "Umuriddin Early Years Section",
                    admissionStatus = "Active",
                    additionalNotes = "Regular attendee in morning Islamic halaqah."
                )
            )
            db.studentDao().insertStudents(initialStudents)

            // Seed initial attendance records for current dates
            val initialAttendance = listOf(
                AttendanceRecord(date = "2026-10-04", className = "Primary 1", studentId = 2, studentName = "Fatima Zahra Usman", studentRegNo = "UIPS/2026/002", status = "Present", markedBy = "Mallam Aliyu Danladi"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 1", studentId = 7, studentName = "Abubakar Saddiq Tijjani", studentRegNo = "UIPS/2026/003", status = "Present", markedBy = "Mallam Aliyu Danladi"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 2", studentId = 1, studentName = "Ahmad Muhammad Bello", studentRegNo = "UIPS/2025/001", status = "Present", markedBy = "Ustaz Ibrahim Bello"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 2", studentId = 8, studentName = "Maryam Kabir Danladi", studentRegNo = "UIPS/2025/012", status = "Late", remarks = "Traffic delay", markedBy = "Ustaz Ibrahim Bello"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 3", studentId = 4, studentName = "Aisha Umar Faruq", studentRegNo = "UIPS/2024/019", status = "Present", markedBy = "Malama Zainab Haruna"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 4", studentId = 6, studentName = "Khadija Mustapha Aliyu", studentRegNo = "UIPS/2024/033", status = "Absent", remarks = "Fever excuse note", markedBy = "Ustaz Mahmud Sanusi"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 5", studentId = 3, studentName = "Ibrahim Khalil Danbatta", studentRegNo = "UIPS/2023/045", status = "Present", markedBy = "Mallam Sulaiman Umar"),
                AttendanceRecord(date = "2026-10-04", className = "Primary 6", studentId = 5, studentName = "Zubairu Sani Garba", studentRegNo = "UIPS/2022/008", status = "Present", markedBy = "Ustaz Mukhtar Garba"),

                // Previous day sample: 2026-10-03
                AttendanceRecord(date = "2026-10-03", className = "Primary 1", studentId = 2, studentName = "Fatima Zahra Usman", studentRegNo = "UIPS/2026/002", status = "Present", markedBy = "Mallam Aliyu Danladi"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 1", studentId = 7, studentName = "Abubakar Saddiq Tijjani", studentRegNo = "UIPS/2026/003", status = "Present", markedBy = "Mallam Aliyu Danladi"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 2", studentId = 1, studentName = "Ahmad Muhammad Bello", studentRegNo = "UIPS/2025/001", status = "Present", markedBy = "Ustaz Ibrahim Bello"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 2", studentId = 8, studentName = "Maryam Kabir Danladi", studentRegNo = "UIPS/2025/012", status = "Present", markedBy = "Ustaz Ibrahim Bello"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 3", studentId = 4, studentName = "Aisha Umar Faruq", studentRegNo = "UIPS/2024/019", status = "Present", markedBy = "Malama Zainab Haruna"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 4", studentId = 6, studentName = "Khadija Mustapha Aliyu", studentRegNo = "UIPS/2024/033", status = "Present", markedBy = "Ustaz Mahmud Sanusi"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 5", studentId = 3, studentName = "Ibrahim Khalil Danbatta", studentRegNo = "UIPS/2023/045", status = "Present", markedBy = "Mallam Sulaiman Umar"),
                AttendanceRecord(date = "2026-10-03", className = "Primary 6", studentId = 5, studentName = "Zubairu Sani Garba", studentRegNo = "UIPS/2022/008", status = "Late", remarks = "Rain shower", markedBy = "Ustaz Mukhtar Garba")
            )
            db.attendanceDao().insertOrUpdateBatch(initialAttendance)

            // Seed multi-term academic grade progression for Performance Trends charts
            val terms = listOf(
                1 to "2024 T3",
                2 to "2025 T1",
                3 to "2025 T2",
                4 to "2025 T3",
                5 to "2026 T1"
            )
            val seededGrades = mutableListOf<StudentGrade>()
            for (studentIndex in 1..initialStudents.size) {
                val sid = studentIndex.toLong()
                val baseQuran = 66 + (studentIndex % 4) * 4
                val baseArabic = 62 + (studentIndex % 3) * 5
                val baseMath = 58 + (studentIndex % 5) * 4
                val baseEnglish = 64 + (studentIndex % 3) * 4
                terms.forEach { (order, term) ->
                    val growth = (order - 1) * 4
                    seededGrades.add(
                        StudentGrade(studentId = sid, termLabel = term, subject = "Quran & Tajweed", score = (baseQuran + growth).coerceIn(40, 99), sortOrder = order, recordedBy = "Ustaz Ibrahim Bello")
                    )
                    seededGrades.add(
                        StudentGrade(studentId = sid, termLabel = term, subject = "Arabic Studies", score = (baseArabic + growth - (order % 2) * 2).coerceIn(40, 98), sortOrder = order, recordedBy = "Mallam Aliyu Danladi")
                    )
                    seededGrades.add(
                        StudentGrade(studentId = sid, termLabel = term, subject = "Mathematics", score = (baseMath + growth + (if (order >= 3) 2 else -1)).coerceIn(40, 97), sortOrder = order, recordedBy = "Mallam Sulaiman Umar")
                    )
                    seededGrades.add(
                        StudentGrade(studentId = sid, termLabel = term, subject = "English Language", score = (baseEnglish + growth).coerceIn(40, 96), sortOrder = order, recordedBy = "Malama Zainab Haruna")
                    )
                }
            }
            db.studentGradeDao().insertGrades(seededGrades)

            // Seed sample SMS history log
            db.smsLogDao().insertSmsLog(
                SmsLog(
                    eventType = "School Announcement",
                    recipientSummary = "All Enrolled Parents (8)",
                    recipientPhones = "08034567890, 08023456789, 08098765432, 08145678901, 08051234567, 08067890123, 08123456780, 08076543210",
                    recipientCount = 8,
                    messageBody = "Assalamu Alaikum. UMURIDDIN ISLAMIYYA PRIMARY SCHOOL reminds parents that 1st Term Academic Assessments begin on Monday at 8:00 AM.",
                    sentBy = "Administrator"
                )
            )

            ensureFeeSeedDataIfEmpty(db)
        }

        private suspend fun ensureFeeSeedDataIfEmpty(db: AppDatabase) {
            val existingSchedules = db.feeDao().getAllClassFeeSchedulesDirect()
            if (existingSchedules.isNotEmpty()) return

            val students = db.studentDao().getAllStudentsDirect()
            val classes = db.classDao().getAllClassesDirect()
            if (classes.isEmpty()) return

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fun offsetDate(days: Int): String {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, days)
                return sdf.format(c.time)
            }

            val duePast = offsetDate(-10)
            val dueSoon = offsetDate(3)
            val dueFuture = offsetDate(28)

            val feeByClass = mapOf(
                "Primary 1" to 20000L,
                "Primary 2" to 22000L,
                "Primary 3" to 24000L,
                "Primary 4" to 26000L,
                "Primary 5" to 28000L,
                "Primary 6" to 35000L
            )

            val schedules = classes.map { c ->
                ClassFeeSchedule(
                    className = c.name,
                    termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
                    tuitionAmount = feeByClass[c.name] ?: 25000L,
                    defaultInstallmentCount = 2,
                    firstDueDate = duePast,
                    secondDueDate = dueSoon,
                    thirdDueDate = dueFuture
                )
            }
            db.feeDao().insertClassFeeSchedules(schedules)

            if (students.isEmpty()) return

            val scheduleMap = schedules.associateBy { it.className }
            val allInstallments = mutableListOf<StudentFeeInstallment>()
            students.forEach { s ->
                val sched = scheduleMap[s.className] ?: schedules.first()
                allInstallments.addAll(FeeHelper.buildInstallmentsForStudent(s.id, sched))
            }
            db.feeDao().insertInstallments(allInstallments)

            // Seed realistic payments: some Paid in full, some Partial (with 2nd installment due soon), some Overdue
            val payments = mutableListOf<FeePayment>()
            var receiptSeq = 1
            students.forEachIndexed { idx, s ->
                val total = scheduleMap[s.className]?.tuitionAmount ?: 25000L
                val half = total / 2L
                when (idx % 4) {
                    0 -> {
                        // Paid in full across two installments
                        payments.add(
                            FeePayment(
                                studentId = s.id,
                                termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
                                amountPaid = total,
                                paymentDate = offsetDate(-14),
                                paymentMethod = "Bank Transfer",
                                receiptNumber = "RCP/2026/${receiptSeq++.toString().padStart(4, '0')}",
                                recordedBy = "Administrator",
                                remarks = "Full term tuition cleared"
                            )
                        )
                    }
                    1 -> {
                        // First installment paid, second due soon (Due Soon status)
                        payments.add(
                            FeePayment(
                                studentId = s.id,
                                termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
                                amountPaid = half,
                                paymentDate = offsetDate(-12),
                                paymentMethod = "Cash",
                                receiptNumber = "RCP/2026/${receiptSeq++.toString().padStart(4, '0')}",
                                recordedBy = "Administrator",
                                remarks = "1st installment received"
                            )
                        )
                    }
                    2 -> {
                        // Partial payment leaving part of 1st installment overdue
                        payments.add(
                            FeePayment(
                                studentId = s.id,
                                termLabel = ClassFeeSchedule.DEFAULT_TERM_LABEL,
                                amountPaid = 5000L,
                                paymentDate = offsetDate(-20),
                                paymentMethod = "POS",
                                receiptNumber = "RCP/2026/${receiptSeq++.toString().padStart(4, '0')}",
                                recordedBy = "Administrator",
                                remarks = "Part-payment deposit"
                            )
                        )
                    }
                    // idx % 4 == 3 -> No payment yet (Overdue on 1st installment)
                }
            }
            if (payments.isNotEmpty()) {
                db.feeDao().insertPayments(payments)
            }
        }
    }
}
