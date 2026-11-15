package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassFeeSchedule
import com.example.data.model.FeePayment
import com.example.data.model.SchoolClass
import com.example.data.model.SchoolInfo
import com.example.data.model.SmsLog
import com.example.data.model.StaffRole
import com.example.data.model.Student
import com.example.data.model.StudentFeeInstallment
import com.example.data.model.StudentGrade
import com.example.data.model.Teacher
import com.example.data.repository.SchoolRepository
import com.example.util.AdminAuthManager
import com.example.util.CsvImportPreviewResult
import com.example.util.FeeHelper
import com.example.util.NotificationHelper
import com.example.util.SmsEventTemplate
import com.example.util.SmsHelper
import com.example.util.StudentCsvImporter
import com.example.util.StudentFeeSummary
import com.example.util.StudentIdCardGenerator
import com.example.util.ThemeMode
import com.example.util.UpcomingAnniversary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    DASHBOARD,
    STUDENTS,
    ADD_STUDENT,
    STUDENT_PROFILE,
    CLASSES,
    CLASS_ATTENDANCE,
    ATTENDANCE_DASHBOARD,
    TEACHERS,
    SMS_NOTIFICATIONS,
    FEE_MANAGEMENT,
    REPORTS,
    SETTINGS
}

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A - Z)"),
    NAME_DESC("Name (Z - A)"),
    DATE_DESC("Newest Admission"),
    DATE_ASC("Oldest Admission"),
    REG_NO("Registration Number")
}

data class DashboardStats(
    val totalStudents: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val newlyAdmittedCount: Int = 0,
    val totalTeachers: Int = 0,
    val classDistribution: Map<String, Int> = emptyMap(),
    val recentAdmissions: List<Student> = emptyList()
)

data class ClassAttendanceSummary(
    val className: String,
    val classTeacher: String,
    val totalEnrolled: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val hasRecords: Boolean
) {
    val attendanceRate: Int
        get() = if (totalEnrolled > 0 && hasRecords) (presentCount * 100 / totalEnrolled).coerceIn(0, 100) else if (!hasRecords) 0 else 0
}

private data class FilterCriteria(
    val query: String,
    val classFilter: String?,
    val genderFilter: String?,
    val statusFilter: String?,
    val sort: SortOption
)

class SchoolViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SchoolRepository
    private val authManager: AdminAuthManager

    val isAuthenticated: StateFlow<Boolean>
    val currentRole: StateFlow<StaffRole>
    val currentStaffName: StateFlow<String>
    val currentAssignedClass: StateFlow<String>
    val schoolInfo: StateFlow<SchoolInfo>
    val themeMode: StateFlow<ThemeMode>

    val attendanceReminderEnabled: StateFlow<Boolean>
    val attendanceReminderHour: StateFlow<Int>
    val attendanceReminderMinute: StateFlow<Int>
    val anniversaryAlertsEnabled: StateFlow<Boolean>
    val feeReminderEnabled: StateFlow<Boolean>
    val feeReminderLeadDays: StateFlow<Int>

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>()

    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

    private val _studentToEdit = MutableStateFlow<Student?>(null)
    val studentToEdit: StateFlow<Student?> = _studentToEdit.asStateFlow()

    // SMS Composer Preselection State
    private val _preselectedSmsStudentIds = MutableStateFlow<Set<Long>>(emptySet())
    val preselectedSmsStudentIds: StateFlow<Set<Long>> = _preselectedSmsStudentIds.asStateFlow()

    private val _preselectedSmsTemplate = MutableStateFlow(SmsEventTemplate.SCHOOL_ANNOUNCEMENT)
    val preselectedSmsTemplate: StateFlow<SmsEventTemplate> = _preselectedSmsTemplate.asStateFlow()

    private val _preselectedSmsCustomBody = MutableStateFlow<String?>(null)
    val preselectedSmsCustomBody: StateFlow<String?> = _preselectedSmsCustomBody.asStateFlow()

    // Attendance State
    private val _selectedClassForAttendance = MutableStateFlow<SchoolClass?>(null)
    val selectedClassForAttendance: StateFlow<SchoolClass?> = _selectedClassForAttendance.asStateFlow()

    val attendanceDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )

    val selectedDashboardDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDashboardMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceForSelectedDate: StateFlow<List<AttendanceRecord>>
    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceForSelectedMonth: StateFlow<List<AttendanceRecord>>
    val classAttendanceSummaries: StateFlow<List<ClassAttendanceSummary>>

    // Filters and search
    val searchQuery = MutableStateFlow("")
    val selectedClassFilter = MutableStateFlow<String?>(null)
    val selectedGenderFilter = MutableStateFlow<String?>(null)
    val selectedStatusFilter = MutableStateFlow<String?>(null)
    val sortOption = MutableStateFlow(SortOption.NAME_ASC)

    val allStudents: StateFlow<List<Student>>
    val allClasses: StateFlow<List<SchoolClass>>
    val allTeachers: StateFlow<List<Teacher>>
    val allSmsLogs: StateFlow<List<SmsLog>>
    val upcomingAnniversaries: StateFlow<List<UpcomingAnniversary>>

    // Tuition & Fee Tracking Flows
    val activeFeeTerm = MutableStateFlow(ClassFeeSchedule.DEFAULT_TERM_LABEL)
    val allClassFeeSchedules: StateFlow<List<ClassFeeSchedule>>
    val allInstallments: StateFlow<List<StudentFeeInstallment>>
    val allFeePayments: StateFlow<List<FeePayment>>
    val studentFeeSummaries: StateFlow<List<StudentFeeSummary>>
    val dueAndOverdueFeeSummaries: StateFlow<List<StudentFeeSummary>>

    val filteredStudents: StateFlow<List<Student>>
    val dashboardStats: StateFlow<DashboardStats>

    private val _csvImportPreview = MutableStateFlow<CsvImportPreviewResult?>(null)
    val csvImportPreview: StateFlow<CsvImportPreviewResult?> = _csvImportPreview.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = SchoolRepository(
            database.studentDao(),
            database.classDao(),
            database.teacherDao(),
            database.attendanceDao(),
            database.studentGradeDao(),
            database.smsLogDao(),
            database.feeDao()
        )
        authManager = AdminAuthManager(application)

        isAuthenticated = authManager.isAuthenticated
        currentRole = authManager.currentRole
        currentStaffName = authManager.currentStaffName
        currentAssignedClass = authManager.currentAssignedClass
        schoolInfo = authManager.schoolInfo
        themeMode = authManager.themeMode
        attendanceReminderEnabled = authManager.attendanceReminderEnabled
        attendanceReminderHour = authManager.attendanceReminderHour
        attendanceReminderMinute = authManager.attendanceReminderMinute
        anniversaryAlertsEnabled = authManager.anniversaryAlertsEnabled
        feeReminderEnabled = authManager.feeReminderEnabled
        feeReminderLeadDays = authManager.feeReminderLeadDays

        NotificationHelper.createNotificationChannels(application)
        if (authManager.isAttendanceReminderEnabledDirect()) {
            NotificationHelper.scheduleDailyAttendanceReminder(
                application,
                authManager.getAttendanceReminderHourDirect(),
                authManager.getAttendanceReminderMinuteDirect()
            )
        }
        if (authManager.isAnniversaryAlertsEnabledDirect()) {
            NotificationHelper.scheduleDailyAnniversaryCheck(application)
        }
        if (authManager.isFeeReminderEnabledDirect()) {
            NotificationHelper.scheduleDailyFeeReminderCheck(application)
        }

        allStudents = repository.allStudents
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allClasses = repository.allClasses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTeachers = repository.allTeachers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSmsLogs = repository.allSmsLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allClassFeeSchedules = repository.allClassFeeSchedules
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allInstallments = repository.allInstallments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allFeePayments = repository.allFeePayments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        studentFeeSummaries = combine(
            allStudents,
            allInstallments,
            allFeePayments,
            activeFeeTerm,
            feeReminderLeadDays
        ) { students, installments, payments, term, leadDays ->
            val today = FeeHelper.todayDateString()
            students.map { student ->
                FeeHelper.computeStudentFeeSummary(
                    student = student,
                    installments = installments,
                    payments = payments,
                    termLabel = term,
                    referenceDateStr = today,
                    dueSoonWindowDays = leadDays
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dueAndOverdueFeeSummaries = studentFeeSummaries
            .map { summaries -> FeeHelper.findDueAndOverdueReminders(summaries) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        upcomingAnniversaries = allStudents
            .map { students -> NotificationHelper.findUpcomingAnniversaries(students, windowDays = 14) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val criteriaFlow = combine(
            searchQuery,
            selectedClassFilter,
            selectedGenderFilter,
            selectedStatusFilter,
            sortOption
        ) { query, classFilter, genderFilter, statusFilter, sort ->
            FilterCriteria(query, classFilter, genderFilter, statusFilter, sort)
        }

        filteredStudents = allStudents.combine(criteriaFlow) { students, criteria ->
            var list = students

            // Search filter (also matches structured QR payloads)
            if (criteria.query.isNotBlank()) {
                val q = criteria.query.trim().lowercase(Locale.ROOT)
                val parsedQr = StudentIdCardGenerator.parseStudentQrPayload(criteria.query)
                list = list.filter { s ->
                    s.fullName.lowercase(Locale.ROOT).contains(q) ||
                    s.registrationNumber.lowercase(Locale.ROOT).contains(q) ||
                    s.parentGuardianName.lowercase(Locale.ROOT).contains(q) ||
                    (parsedQr.studentId != null && s.id == parsedQr.studentId) ||
                    (!parsedQr.registrationNumber.isNullOrBlank() &&
                        s.registrationNumber.lowercase(Locale.ROOT).contains(parsedQr.registrationNumber.lowercase(Locale.ROOT)))
                }
            }

            // Class filter
            if (!criteria.classFilter.isNullOrBlank()) {
                list = list.filter { it.className == criteria.classFilter }
            }

            // Gender filter
            if (!criteria.genderFilter.isNullOrBlank()) {
                list = list.filter { it.gender.equals(criteria.genderFilter, ignoreCase = true) }
            }

            // Status filter
            if (!criteria.statusFilter.isNullOrBlank()) {
                list = list.filter { it.admissionStatus.equals(criteria.statusFilter, ignoreCase = true) }
            }

            // Sorting
            when (criteria.sort) {
                SortOption.NAME_ASC -> list.sortedBy { it.fullName.lowercase(Locale.ROOT) }
                SortOption.NAME_DESC -> list.sortedByDescending { it.fullName.lowercase(Locale.ROOT) }
                SortOption.DATE_DESC -> list.sortedByDescending { it.dateOfAdmission }
                SortOption.DATE_ASC -> list.sortedBy { it.dateOfAdmission }
                SortOption.REG_NO -> list.sortedBy { it.registrationNumber }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dashboardStats = combine(allStudents, allClasses, allTeachers) { students, classes, teachers ->
            val total = students.size
            val male = students.count { it.gender.equals("Male", ignoreCase = true) }
            val female = students.count { it.gender.equals("Female", ignoreCase = true) }

            // Newly admitted (year 2026 or last 3 months)
            val currentYearStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
            val newlyAdmitted = students.count { it.dateOfAdmission.startsWith(currentYearStr) }

            // Class distribution map
            val classMap = mutableMapOf<String, Int>()
            classes.forEach { c ->
                classMap[c.name] = students.count { it.className == c.name }
            }
            students.forEach { s ->
                if (!classMap.containsKey(s.className)) {
                    classMap[s.className] = (classMap[s.className] ?: 0) + 1
                }
            }

            // Recent admissions (sorted by admission date descending, take 5)
            val recent = students.sortedByDescending { it.dateOfAdmission }.take(5)

            DashboardStats(
                totalStudents = total,
                maleCount = male,
                femaleCount = female,
                newlyAdmittedCount = newlyAdmitted,
                totalTeachers = teachers.size,
                classDistribution = classMap,
                recentAdmissions = recent
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

        attendanceForSelectedDate = selectedDashboardDate
            .flatMapLatest { date -> repository.getAllAttendanceForDate(date) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        attendanceForSelectedMonth = selectedDashboardMonth
            .flatMapLatest { month -> repository.getAllAttendanceForMonth(month) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        classAttendanceSummaries = combine(
            allClasses,
            allStudents,
            attendanceForSelectedDate
        ) { classes, students, records ->
            classes.map { c ->
                val enrolledStudents = students.filter { it.className == c.name }
                val classRecords = records.filter { it.className == c.name }
                val hasRecs = classRecords.isNotEmpty()

                val pCount = classRecords.count { it.status.equals("Present", ignoreCase = true) }
                val aCount = classRecords.count { it.status.equals("Absent", ignoreCase = true) }
                val lCount = classRecords.count { it.status.equals("Late", ignoreCase = true) }
                val eCount = classRecords.count { it.status.equals("Excused", ignoreCase = true) }

                ClassAttendanceSummary(
                    className = c.name,
                    classTeacher = c.classTeacher,
                    totalEnrolled = enrolledStudents.size,
                    presentCount = pCount,
                    absentCount = aCount,
                    lateCount = lCount,
                    excusedCount = eCount,
                    hasRecords = hasRecs
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun openAttendanceDashboard() {
        navigateTo(Screen.ATTENDANCE_DASHBOARD)
    }

    fun setDashboardDate(date: String) {
        selectedDashboardDate.value = date
        if (date.length >= 7) {
            selectedDashboardMonth.value = date.substring(0, 7)
        }
    }

    fun setDashboardMonth(yearMonth: String) {
        selectedDashboardMonth.value = yearMonth
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_screenHistory.isNotEmpty()) {
            val previous = _screenHistory.removeAt(_screenHistory.size - 1)
            _currentScreen.value = previous
            return true
        }
        if (_currentScreen.value != Screen.DASHBOARD) {
            _currentScreen.value = Screen.DASHBOARD
            return true
        }
        return false
    }

    fun showStudentProfile(student: Student) {
        _selectedStudent.value = student
        navigateTo(Screen.STUDENT_PROFILE)
    }

    fun findStudentByQrPayload(payload: String): Student? {
        return findStudentMatchingPayload(allStudents.value, payload)
    }

    fun openStudentByQrPayload(payload: String): Boolean {
        val student = findStudentByQrPayload(payload)
        return if (student != null) {
            showStudentProfile(student)
            _snackbarMessage.value = "Retrieved: ${student.fullName} (${student.className})"
            true
        } else {
            _snackbarMessage.value = "No student record matched QR/ID '$payload'."
            false
        }
    }

    fun startAddNewStudent() {
        _studentToEdit.value = null
        navigateTo(Screen.ADD_STUDENT)
    }

    fun startEditStudent(student: Student) {
        _studentToEdit.value = student
        navigateTo(Screen.ADD_STUDENT)
    }

    fun saveStudent(student: Student, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (student.id == 0L) {
                val newId = repository.insertStudent(student)
                // Auto-assign termly tuition installments based on the student's class schedule
                val classSched = allClassFeeSchedules.value.firstOrNull {
                    it.className == student.className && it.termLabel == activeFeeTerm.value
                }
                if (classSched != null) {
                    val installments = FeeHelper.buildInstallmentsForStudent(newId, classSched)
                    installments.forEach { repository.insertInstallment(it) }
                }
                _snackbarMessage.value = "Student registered successfully!"
            } else {
                repository.updateStudent(student.copy(updatedAt = System.currentTimeMillis()))
                _snackbarMessage.value = "Student record updated!"
                if (_selectedStudent.value?.id == student.id) {
                    _selectedStudent.value = student
                }
            }
            onSuccess()
        }
    }

    fun deleteStudent(student: Student, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _snackbarMessage.value = "${student.fullName} deleted successfully."
            if (_selectedStudent.value?.id == student.id) {
                _selectedStudent.value = null
            }
            onDeleted()
        }
    }

    // Bulk CSV Student Onboarding (Admin Only)
    fun loadCsvFromUri(context: Context, uri: Uri) {
        if (!hasAdminPermission()) {
            showAccessDeniedMessage("Admin")
            return
        }
        viewModelScope.launch {
            try {
                val (fileName, csvText) = withContext(Dispatchers.IO) {
                    StudentCsvImporter.readCsvFromUri(context, uri)
                }
                if (csvText.isBlank()) {
                    _snackbarMessage.value = "Selected CSV file is empty or unreadable."
                    return@launch
                }
                loadCsvFromText(csvText, fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Unable to read CSV file: ${e.message ?: "Unknown error"}"
            }
        }
    }

    fun loadCsvFromText(csvText: String, fileName: String = "students_import.csv") {
        if (!hasAdminPermission()) {
            showAccessDeniedMessage("Admin")
            return
        }
        val knownClasses = allClasses.value.map { it.name }.ifEmpty {
            listOf("Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "Primary 6")
        }
        val preview = StudentCsvImporter.parseAndValidate(
            csvText = csvText,
            existingStudents = allStudents.value,
            knownClassNames = knownClasses,
            fileName = fileName
        )
        _csvImportPreview.value = preview
        if (preview.rows.isEmpty()) {
            _snackbarMessage.value = "No data rows detected in CSV."
        }
    }

    fun clearCsvImportPreview() {
        _csvImportPreview.value = null
    }

    fun confirmCsvBulkImport(
        assignTermFees: Boolean = true,
        onComplete: (Int) -> Unit = {}
    ) {
        if (!hasAdminPermission()) {
            showAccessDeniedMessage("Admin")
            return
        }
        val preview = _csvImportPreview.value ?: return
        val studentsToInsert = preview.validStudents
        if (studentsToInsert.isEmpty()) {
            _snackbarMessage.value = "No valid new student rows available to import."
            return
        }

        viewModelScope.launch {
            val insertedIds = repository.insertStudentsBatch(studentsToInsert)

            // Ensure any new class names referenced in the CSV exist in SchoolClass table
            val existingClassNames = allClasses.value.map { it.name.lowercase(Locale.ROOT) }.toSet()
            val missingClasses = studentsToInsert
                .map { it.className }
                .distinct()
                .filter { it.isNotBlank() && it.lowercase(Locale.ROOT) !in existingClassNames }
                .map { clsName ->
                    SchoolClass(
                        name = clsName,
                        section = "A",
                        classTeacher = "",
                        capacity = 40
                    )
                }
            if (missingClasses.isNotEmpty()) {
                repository.insertClassesBatch(missingClasses)
            }

            if (assignTermFees) {
                val schedulesByClass = allClassFeeSchedules.value
                    .filter { it.termLabel == activeFeeTerm.value }
                    .associateBy { it.className }
                studentsToInsert.forEachIndexed { idx, student ->
                    val newId = insertedIds.getOrNull(idx) ?: return@forEachIndexed
                    val sched = schedulesByClass[student.className] ?: return@forEachIndexed
                    val installments = FeeHelper.buildInstallmentsForStudent(newId, sched)
                    installments.forEach { repository.insertInstallment(it) }
                }
            }

            _csvImportPreview.value = null
            _snackbarMessage.value = "Successfully imported ${studentsToInsert.size} student record(s)!"
            onComplete(studentsToInsert.size)
        }
    }

    // Class Management
    fun addClass(name: String, section: String, teacher: String, capacity: Int) {
        viewModelScope.launch {
            repository.insertClass(
                SchoolClass(
                    name = name.trim(),
                    section = section.ifBlank { "A" },
                    classTeacher = teacher.trim(),
                    capacity = if (capacity > 0) capacity else 40
                )
            )
            _snackbarMessage.value = "Class $name added!"
        }
    }

    fun updateClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            repository.updateClass(schoolClass)
            _snackbarMessage.value = "Class ${schoolClass.name} updated!"
        }
    }

    fun deleteClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            repository.deleteClass(schoolClass)
            _snackbarMessage.value = "Class ${schoolClass.name} removed."
        }
    }

    // Attendance Management
    fun openAttendanceSheet(schoolClass: SchoolClass) {
        _selectedClassForAttendance.value = schoolClass
        navigateTo(Screen.CLASS_ATTENDANCE)
    }

    fun getAttendanceForSelectedClassAndDate(className: String, date: String) =
        repository.getAttendanceForClassAndDate(className, date)

    suspend fun getExistingAttendance(className: String, date: String): List<AttendanceRecord> =
        repository.getAttendanceForClassAndDateDirect(className, date)

    fun saveAttendance(records: List<AttendanceRecord>, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveAttendanceBatch(records)
            _snackbarMessage.value = "Attendance for ${records.size} students saved successfully!"
            onComplete()
        }
    }

    // Teacher Management (Admin)
    fun addTeacher(teacher: Teacher, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertTeacher(teacher)
            _snackbarMessage.value = "Teacher ${teacher.fullName} registered successfully!"
            onComplete()
        }
    }

    fun updateTeacher(teacher: Teacher, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateTeacher(teacher)
            _snackbarMessage.value = "Teacher ${teacher.fullName} updated!"
            onComplete()
        }
    }

    fun deleteTeacher(teacher: Teacher, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTeacher(teacher)
            _snackbarMessage.value = "Teacher ${teacher.fullName} removed."
            onComplete()
        }
    }

    suspend fun generateNextStaffId(): String {
        return repository.generateNextStaffId()
    }

    suspend fun generateNextRegistrationNumber(): String {
        return repository.generateNextRegistrationNumber()
    }

    // Academic Performance Grades
    fun getGradesForStudent(studentId: Long) = repository.getGradesForStudent(studentId)

    fun addStudentGrade(grade: StudentGrade, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertGrade(grade.copy(recordedBy = currentStaffName.value))
            _snackbarMessage.value = "Grade recorded for ${grade.subject} (${grade.termLabel})!"
            onComplete()
        }
    }

    fun deleteStudentGrade(grade: StudentGrade) {
        viewModelScope.launch {
            repository.deleteGrade(grade)
            _snackbarMessage.value = "Grade entry removed."
        }
    }

    // SMS Notifications
    fun openSmsHub(
        preselectedIds: Set<Long> = emptySet(),
        template: SmsEventTemplate = SmsEventTemplate.SCHOOL_ANNOUNCEMENT,
        customBody: String? = null
    ) {
        _preselectedSmsStudentIds.value = preselectedIds
        _preselectedSmsTemplate.value = template
        _preselectedSmsCustomBody.value = customBody
        navigateTo(Screen.SMS_NOTIFICATIONS)
    }

    fun clearSmsPreselection() {
        _preselectedSmsStudentIds.value = emptySet()
        _preselectedSmsCustomBody.value = null
    }

    fun openFeeSmsReminder(summaries: List<StudentFeeSummary>) {
        if (summaries.isEmpty()) {
            _snackbarMessage.value = "No overdue or due-soon student accounts selected."
            return
        }
        val ids = summaries.map { it.student.id }.toSet()
        val body = if (summaries.size == 1) {
            FeeHelper.buildFeeReminderSmsMessage(summaries.first(), schoolInfo.value)
        } else {
            FeeHelper.buildBulkFeeReminderSmsMessage(schoolInfo.value, activeFeeTerm.value)
        }
        openSmsHub(
            preselectedIds = ids,
            template = SmsEventTemplate.FEE_REMINDER,
            customBody = body
        )
    }

    // Tuition & Fee Payment Tracking Actions
    fun getInstallmentsForStudent(studentId: Long) = repository.getInstallmentsForStudent(studentId)

    fun getPaymentsForStudent(studentId: Long) = repository.getPaymentsForStudent(studentId)

    fun recordFeePayment(
        studentId: Long,
        installmentId: Long?,
        termLabel: String,
        amountPaid: Long,
        paymentDate: String,
        paymentMethod: String,
        remarks: String,
        onComplete: (FeePayment) -> Unit = {}
    ) {
        if (!canManageFees()) {
            showAccessDeniedMessage("Admin")
            return
        }
        if (amountPaid <= 0L) {
            _snackbarMessage.value = "Payment amount must be greater than ₦0."
            return
        }
        viewModelScope.launch {
            val receiptNo = repository.generateNextReceiptNumber()
            val payment = FeePayment(
                studentId = studentId,
                installmentId = installmentId,
                termLabel = termLabel,
                amountPaid = amountPaid,
                paymentDate = paymentDate,
                paymentMethod = paymentMethod,
                receiptNumber = receiptNo,
                recordedBy = currentStaffName.value,
                remarks = remarks.trim()
            )
            val newId = repository.insertFeePayment(payment)
            val saved = payment.copy(id = newId)
            _snackbarMessage.value = "Recorded ${FeeHelper.formatNaira(amountPaid)} payment ($receiptNo)"
            onComplete(saved)
        }
    }

    fun deleteFeePayment(payment: FeePayment) {
        if (!canManageFees()) {
            showAccessDeniedMessage("Admin")
            return
        }
        viewModelScope.launch {
            repository.deleteFeePayment(payment)
            _snackbarMessage.value = "Removed payment receipt ${payment.receiptNumber}."
        }
    }

    fun addStudentInstallment(
        studentId: Long,
        termLabel: String,
        title: String,
        amountDue: Long,
        dueDate: String,
        notes: String = "",
        onComplete: () -> Unit = {}
    ) {
        if (!canManageFees()) {
            showAccessDeniedMessage("Admin")
            return
        }
        if (title.isBlank() || amountDue <= 0L || dueDate.isBlank()) {
            _snackbarMessage.value = "Enter a valid installment title, amount, and due date."
            return
        }
        viewModelScope.launch {
            repository.insertInstallment(
                StudentFeeInstallment(
                    studentId = studentId,
                    termLabel = termLabel,
                    installmentTitle = title.trim(),
                    amountDue = amountDue,
                    dueDate = dueDate.trim(),
                    notes = notes.trim()
                )
            )
            _snackbarMessage.value = "Added $title (${FeeHelper.formatNaira(amountDue)}) due $dueDate."
            onComplete()
        }
    }

    fun deleteInstallment(installment: StudentFeeInstallment) {
        if (!canManageFees()) {
            showAccessDeniedMessage("Admin")
            return
        }
        viewModelScope.launch {
            repository.deleteInstallment(installment)
            _snackbarMessage.value = "Installment '${installment.installmentTitle}' removed."
        }
    }

    fun updateClassFeeSchedule(
        className: String,
        termLabel: String,
        tuitionAmount: Long,
        installmentCount: Int,
        firstDueDate: String,
        secondDueDate: String,
        thirdDueDate: String,
        applyToClassStudents: Boolean,
        onComplete: () -> Unit = {}
    ) {
        if (!canManageFees()) {
            showAccessDeniedMessage("Admin")
            return
        }
        if (tuitionAmount <= 0L) {
            _snackbarMessage.value = "Class tuition amount must be greater than ₦0."
            return
        }
        viewModelScope.launch {
            val schedule = ClassFeeSchedule(
                className = className,
                termLabel = termLabel,
                tuitionAmount = tuitionAmount,
                defaultInstallmentCount = installmentCount.coerceIn(1, 3),
                firstDueDate = firstDueDate.trim(),
                secondDueDate = secondDueDate.trim(),
                thirdDueDate = thirdDueDate.trim()
            )
            repository.upsertClassFeeSchedule(schedule)
            if (applyToClassStudents) {
                val classStudents = allStudents.value.filter { it.className == className }
                classStudents.forEach { s ->
                    val newInsts = FeeHelper.buildInstallmentsForStudent(s.id, schedule)
                    repository.replaceStudentTermInstallments(s.id, termLabel, newInsts)
                }
            }
            _snackbarMessage.value = "Updated $className tuition (${FeeHelper.formatNaira(tuitionAmount)})"
            onComplete()
        }
    }

    fun updateFeeReminderSettings(enabled: Boolean, leadDays: Int) {
        val ctx = getApplication<Application>()
        authManager.setFeeReminderSettings(enabled, leadDays)
        if (enabled) {
            NotificationHelper.scheduleDailyFeeReminderCheck(ctx)
            _snackbarMessage.value = "Automated fee alerts active (${leadDays}-day notice before due date)."
        } else {
            NotificationHelper.cancelFeeReminderCheck(ctx)
            _snackbarMessage.value = "Automated fee due-date alerts paused."
        }
    }

    fun triggerFeeReminderCheckNow() {
        val ctx = getApplication<Application>()
        val dueList = dueAndOverdueFeeSummaries.value
        if (dueList.isEmpty()) {
            _snackbarMessage.value = "All student accounts are current — no due or overdue installments!"
        } else {
            val sent = NotificationHelper.showFeeDueReminderNotification(ctx, dueList)
            _snackbarMessage.value = if (sent) {
                "Sent alert for ${dueList.size} due/overdue student account(s)!"
            } else {
                "Found ${dueList.size} due/overdue account(s); grant notification permission to show alerts."
            }
        }
    }

    fun logSmsNotification(
        eventType: String,
        recipientSummary: String,
        recipientPhones: String,
        recipientCount: Int,
        messageBody: String
    ) {
        viewModelScope.launch {
            repository.insertSmsLog(
                SmsLog(
                    eventType = eventType,
                    recipientSummary = recipientSummary,
                    recipientPhones = recipientPhones,
                    recipientCount = recipientCount,
                    messageBody = messageBody,
                    sentBy = currentStaffName.value
                )
            )
        }
    }

    // Local Notifications & Reminders
    fun updateAttendanceReminder(enabled: Boolean, hour: Int, minute: Int) {
        val ctx = getApplication<Application>()
        authManager.setAttendanceReminderSettings(enabled, hour, minute)
        if (enabled) {
            NotificationHelper.scheduleDailyAttendanceReminder(ctx, hour, minute)
            _snackbarMessage.value = String.format(
                Locale.getDefault(),
                "Daily attendance reminder set for %02d:%02d",
                hour,
                minute
            )
        } else {
            NotificationHelper.cancelAttendanceReminder(ctx)
            _snackbarMessage.value = "Daily attendance reminder paused."
        }
    }

    fun setAnniversaryAlertsEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>()
        authManager.setAnniversaryAlertsEnabled(enabled)
        if (enabled) {
            NotificationHelper.scheduleDailyAnniversaryCheck(ctx)
            _snackbarMessage.value = "Admission anniversary alerts enabled."
        } else {
            NotificationHelper.cancelAnniversaryCheck(ctx)
            _snackbarMessage.value = "Admission anniversary alerts disabled."
        }
    }

    fun triggerTestAttendanceReminder() {
        val ctx = getApplication<Application>()
        val sent = NotificationHelper.showAttendanceReminderNotification(
            context = ctx,
            title = "Daily Attendance Reminder",
            message = "Class teachers: Please mark today's attendance register in UMURIDDIN ISLAMIYYA PRIMARY SCHOOL."
        )
        _snackbarMessage.value = if (sent) {
            "Attendance reminder notification sent!"
        } else {
            "Notification permission required to display alerts."
        }
    }

    fun triggerAnniversaryCheckNow() {
        val ctx = getApplication<Application>()
        val list = NotificationHelper.findUpcomingAnniversaries(allStudents.value, windowDays = 14)
        if (list.isEmpty()) {
            _snackbarMessage.value = "No student admission anniversaries in the next 14 days."
        } else {
            val sent = NotificationHelper.showAnniversaryNotification(ctx, list)
            _snackbarMessage.value = if (sent) {
                "Notified ${list.size} upcoming admission anniversary(s)!"
            } else {
                "Found ${list.size} upcoming anniversary(s); grant notification permission to view alerts."
            }
        }
    }

    // Authentication & Role Access Control
    fun hasAdminPermission(): Boolean = isAuthenticated.value && currentRole.value == StaffRole.ADMIN

    fun canViewSensitiveRecords(): Boolean =
        isAuthenticated.value && currentRole.value.canViewSensitiveStudentDetails

    fun canViewFees(): Boolean =
        isAuthenticated.value && currentRole.value.canViewFeeRecords

    fun canManageFees(): Boolean =
        isAuthenticated.value && currentRole.value.canManageFeeRecords

    fun canMarkAttendance(): Boolean =
        isAuthenticated.value && currentRole.value.canMarkAttendance

    fun canSendSms(): Boolean =
        isAuthenticated.value && currentRole.value.canSendSms

    fun canRecordGrades(): Boolean =
        isAuthenticated.value && currentRole.value.canRecordGrades

    fun showAccessDeniedMessage(requiredRole: String) {
        _snackbarMessage.value = "Restricted: Requires $requiredRole access."
    }

    fun login(pin: String): Boolean {
        val success = authManager.login(pin)
        if (success) {
            _snackbarMessage.value = "Welcome, Administrator!"
        } else {
            _snackbarMessage.value = "Invalid Admin PIN. (Default: ${AdminAuthManager.DEFAULT_PIN})"
        }
        return success
    }

    fun loginWithRole(
        pinOrCode: String,
        role: StaffRole,
        teacher: Teacher? = null
    ): Boolean {
        val success = authManager.loginWithRole(
            pinOrCode = pinOrCode,
            role = role,
            staffName = teacher?.fullName ?: role.displayName,
            expectedStaffId = teacher?.staffId,
            assignedClass = teacher?.assignedClass ?: ""
        )
        if (success) {
            val label = teacher?.fullName ?: role.displayName
            _snackbarMessage.value = "Signed in as $label (${role.displayName})"
        } else {
            _snackbarMessage.value = "Invalid credentials. Use Admin PIN or Staff ID."
        }
        return success
    }

    fun switchRoleDirectly(role: StaffRole, teacher: Teacher? = null) {
        val name = teacher?.fullName ?: role.displayName
        authManager.switchActiveRole(role, name, teacher?.assignedClass ?: "")
        _snackbarMessage.value = "Switched active role to $name (${role.displayName})"
    }

    fun logout() {
        authManager.logout()
        _snackbarMessage.value = "Session locked."
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val success = authManager.changePin(oldPin, newPin)
        if (success) {
            _snackbarMessage.value = "Admin PIN updated successfully!"
        } else {
            _snackbarMessage.value = "Incorrect current PIN."
        }
        return success
    }

    fun updateSchoolProfile(info: SchoolInfo) {
        authManager.updateSchoolInfo(info)
        _snackbarMessage.value = "School profile updated!"
    }

    fun setThemeMode(mode: ThemeMode) {
        authManager.setThemeMode(mode)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // Backup & Restore
    fun exportBackupJson(context: Context): File? {
        return try {
            val root = JSONObject()
            root.put("school_info", JSONObject().apply {
                put("name", schoolInfo.value.name)
                put("location", schoolInfo.value.location)
                put("phone", schoolInfo.value.phoneNumber)
                put("email", schoolInfo.value.email)
            })

            val studentsArray = JSONArray()
            allStudents.value.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("fullName", s.fullName)
                    put("age", s.age)
                    put("dateOfBirth", s.dateOfBirth)
                    put("dateOfAdmission", s.dateOfAdmission)
                    put("gender", s.gender)
                    put("className", s.className)
                    put("registrationNumber", s.registrationNumber)
                    put("parentGuardianName", s.parentGuardianName)
                    put("parentGuardianPhone", s.parentGuardianPhone)
                    put("homeAddress", s.homeAddress)
                    put("previousSchool", s.previousSchool)
                    put("admissionStatus", s.admissionStatus)
                    put("additionalNotes", s.additionalNotes)
                }
                studentsArray.put(obj)
            }
            root.put("students", studentsArray)

            val classesArray = JSONArray()
            allClasses.value.forEach { c ->
                val obj = JSONObject().apply {
                    put("name", c.name)
                    put("section", c.section)
                    put("classTeacher", c.classTeacher)
                    put("capacity", c.capacity)
                }
                classesArray.put(obj)
            }
            root.put("classes", classesArray)

            val teachersArray = JSONArray()
            allTeachers.value.forEach { t ->
                val obj = JSONObject().apply {
                    put("fullName", t.fullName)
                    put("staffId", t.staffId)
                    put("phoneNumber", t.phoneNumber)
                    put("gender", t.gender)
                    put("qualification", t.qualification)
                    put("assignedClass", t.assignedClass)
                    put("subjectSpecialization", t.subjectSpecialization)
                    put("role", t.role)
                    put("status", t.status)
                    put("joinedDate", t.joinedDate)
                }
                teachersArray.put(obj)
            }
            root.put("teachers", teachersArray)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "Umuriddin_Backup_$timestamp.json")
            file.writeText(root.toString(2))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val studentsArray = root.optJSONArray("students")
            if (studentsArray != null) {
                val list = mutableListOf<Student>()
                for (i in 0 until studentsArray.length()) {
                    val obj = studentsArray.getJSONObject(i)
                    list.add(
                        Student(
                            fullName = obj.getString("fullName"),
                            age = obj.optInt("age", 6),
                            dateOfBirth = obj.optString("dateOfBirth", "2019-01-01"),
                            dateOfAdmission = obj.optString("dateOfAdmission", "2026-01-01"),
                            gender = obj.optString("gender", "Male"),
                            className = obj.optString("className", "Primary 1"),
                            registrationNumber = obj.optString("registrationNumber", "UIPS/2026/001"),
                            parentGuardianName = obj.optString("parentGuardianName", ""),
                            parentGuardianPhone = obj.optString("parentGuardianPhone", ""),
                            homeAddress = obj.optString("homeAddress", ""),
                            previousSchool = obj.optString("previousSchool", ""),
                            admissionStatus = obj.optString("admissionStatus", "Active"),
                            additionalNotes = obj.optString("additionalNotes", "")
                        )
                    )
                }
                viewModelScope.launch {
                    repository.insertStudentsBatch(list)
                    _snackbarMessage.value = "Restored ${list.size} student records!"
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _snackbarMessage.value = "Failed to restore backup: Invalid file format"
            false
        }
    }

    companion object {
        fun findStudentMatchingPayload(students: List<Student>, payload: String): Student? {
            val parsed = StudentIdCardGenerator.parseStudentQrPayload(payload)
            if (parsed.rawText.isBlank()) return null

            if (parsed.studentId != null) {
                students.firstOrNull { it.id == parsed.studentId }?.let { return it }
            }
            if (!parsed.registrationNumber.isNullOrBlank()) {
                students.firstOrNull {
                    it.registrationNumber.equals(parsed.registrationNumber, ignoreCase = true)
                }?.let { return it }

                // Fallback partial registration number match when unambiguous
                val q = parsed.registrationNumber.lowercase(Locale.ROOT)
                val partials = students.filter {
                    it.registrationNumber.lowercase(Locale.ROOT).contains(q) ||
                        it.fullName.lowercase(Locale.ROOT).contains(q)
                }
                if (partials.size == 1) return partials.first()
            }
            return null
        }
    }
}
