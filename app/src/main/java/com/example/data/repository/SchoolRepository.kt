package com.example.data.repository

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
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SchoolRepository(
    private val studentDao: StudentDao,
    private val classDao: ClassDao,
    private val teacherDao: TeacherDao,
    private val attendanceDao: AttendanceDao,
    private val studentGradeDao: StudentGradeDao,
    private val smsLogDao: SmsLogDao,
    private val feeDao: FeeDao
) {
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val allClasses: Flow<List<SchoolClass>> = classDao.getAllClasses()
    val allTeachers: Flow<List<Teacher>> = teacherDao.getAllTeachers()
    val allSmsLogs: Flow<List<SmsLog>> = smsLogDao.getAllSmsLogs()
    val allClassFeeSchedules: Flow<List<ClassFeeSchedule>> = feeDao.getAllClassFeeSchedules()
    val allInstallments: Flow<List<StudentFeeInstallment>> = feeDao.getAllInstallments()
    val allFeePayments: Flow<List<FeePayment>> = feeDao.getAllPayments()
    val studentCount: Flow<Int> = studentDao.getStudentCount()
    val maleCount: Flow<Int> = studentDao.getMaleCount()
    val femaleCount: Flow<Int> = studentDao.getFemaleCount()
    val teacherCount: Flow<Int> = teacherDao.getTeacherCount()

    fun getStudentById(id: Long): Flow<Student?> = studentDao.getStudentById(id)

    suspend fun getStudentByIdDirect(id: Long): Student? = studentDao.getStudentByIdDirect(id)

    fun searchStudents(query: String): Flow<List<Student>> = studentDao.searchStudents(query)

    fun getStudentsByClass(className: String): Flow<List<Student>> = studentDao.getStudentsByClass(className)

    suspend fun insertStudent(student: Student): Long = studentDao.insertStudent(student)

    suspend fun updateStudent(student: Student) = studentDao.updateStudent(student)

    suspend fun deleteStudent(student: Student) = studentDao.deleteStudent(student)

    suspend fun deleteStudentById(id: Long) = studentDao.deleteStudentById(id)

    suspend fun insertClass(schoolClass: SchoolClass): Long = classDao.insertClass(schoolClass)

    suspend fun updateClass(schoolClass: SchoolClass) = classDao.updateClass(schoolClass)

    suspend fun deleteClass(schoolClass: SchoolClass) = classDao.deleteClass(schoolClass)

    suspend fun deleteClassById(id: Long) = classDao.deleteClassById(id)

    // Teacher Management
    suspend fun insertTeacher(teacher: Teacher): Long = teacherDao.insertTeacher(teacher)

    suspend fun updateTeacher(teacher: Teacher) = teacherDao.updateTeacher(teacher)

    suspend fun deleteTeacher(teacher: Teacher) = teacherDao.deleteTeacher(teacher)

    suspend fun deleteTeacherById(id: Long) = teacherDao.deleteTeacherById(id)

    suspend fun generateNextStaffId(): String {
        val count = teacherDao.getTotalTeachersDirect()
        val nextSeq = count + 1
        return "UIPS/T/${nextSeq.toString().padStart(3, '0')}"
    }

    // Attendance Management
    fun getAttendanceForClassAndDate(className: String, date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForClassAndDate(className, date)

    suspend fun getAttendanceForClassAndDateDirect(className: String, date: String): List<AttendanceRecord> =
        attendanceDao.getAttendanceForClassAndDateDirect(className, date)

    fun getRecordedDatesForClass(className: String): Flow<List<String>> =
        attendanceDao.getRecordedDatesForClass(className)

    fun getAllAttendanceForDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAllAttendanceForDate(date)

    fun getAllAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAllAttendanceForMonth(monthPrefix)

    fun getRecordedDatesForMonth(monthPrefix: String): Flow<List<String>> =
        attendanceDao.getRecordedDatesForMonth(monthPrefix)

    suspend fun saveAttendanceBatch(records: List<AttendanceRecord>) =
        attendanceDao.insertOrUpdateBatch(records)

    suspend fun generateNextRegistrationNumber(): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val total = studentDao.getTotalCountDirect()
        val nextSeq = total + 1
        return "UIPS/$currentYear/${nextSeq.toString().padStart(3, '0')}"
    }

    suspend fun insertStudentsBatch(students: List<Student>): List<Long> = studentDao.insertStudents(students)

    suspend fun insertClassesBatch(classes: List<SchoolClass>) = classDao.insertClasses(classes)

    suspend fun insertTeachersBatch(teachers: List<Teacher>) = teacherDao.insertTeachers(teachers)

    // Academic Grades & Performance Trends
    fun getGradesForStudent(studentId: Long): Flow<List<StudentGrade>> =
        studentGradeDao.getGradesForStudent(studentId)

    suspend fun insertGrade(grade: StudentGrade): Long =
        studentGradeDao.insertGrade(grade)

    suspend fun deleteGrade(grade: StudentGrade) =
        studentGradeDao.deleteGrade(grade)

    // SMS Notification Logs
    suspend fun insertSmsLog(log: SmsLog): Long =
        smsLogDao.insertSmsLog(log)

    suspend fun deleteSmsLogById(id: Long) =
        smsLogDao.deleteSmsLogById(id)

    // Tuition & Fee Payment Tracking
    fun getInstallmentsForStudent(studentId: Long): Flow<List<StudentFeeInstallment>> =
        feeDao.getInstallmentsForStudent(studentId)

    fun getPaymentsForStudent(studentId: Long): Flow<List<FeePayment>> =
        feeDao.getPaymentsForStudent(studentId)

    suspend fun upsertClassFeeSchedule(schedule: ClassFeeSchedule): Long {
        val existing = feeDao.getScheduleForClassAndTerm(schedule.className, schedule.termLabel)
        return if (existing != null) {
            feeDao.updateClassFeeSchedule(schedule.copy(id = existing.id, updatedAt = System.currentTimeMillis()))
            existing.id
        } else {
            feeDao.insertClassFeeSchedule(schedule)
        }
    }

    suspend fun replaceStudentTermInstallments(
        studentId: Long,
        termLabel: String,
        newInstallments: List<StudentFeeInstallment>
    ) {
        feeDao.deleteInstallmentsForStudentTerm(studentId, termLabel)
        if (newInstallments.isNotEmpty()) {
            feeDao.insertInstallments(newInstallments)
        }
    }

    suspend fun insertInstallment(installment: StudentFeeInstallment): Long =
        feeDao.insertInstallment(installment)

    suspend fun deleteInstallment(installment: StudentFeeInstallment) =
        feeDao.deleteInstallment(installment)

    suspend fun insertFeePayment(payment: FeePayment): Long =
        feeDao.insertPayment(payment)

    suspend fun deleteFeePayment(payment: FeePayment) =
        feeDao.deletePayment(payment)

    suspend fun generateNextReceiptNumber(): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val total = feeDao.getTotalPaymentsCountDirect()
        val nextSeq = total + 1
        return "RCP/$currentYear/${nextSeq.toString().padStart(4, '0')}"
    }
}
