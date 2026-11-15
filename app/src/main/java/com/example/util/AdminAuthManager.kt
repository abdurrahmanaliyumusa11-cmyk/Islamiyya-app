package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SchoolInfo
import com.example.data.model.StaffRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

class AdminAuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("admin_auth_prefs", Context.MODE_PRIVATE)

    private val _isAuthenticated = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, true))
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentRole = MutableStateFlow(loadCurrentRole())
    val currentRole: StateFlow<StaffRole> = _currentRole.asStateFlow()

    private val _currentStaffName = MutableStateFlow(
        prefs.getString(KEY_STAFF_NAME, "Administrator") ?: "Administrator"
    )
    val currentStaffName: StateFlow<String> = _currentStaffName.asStateFlow()

    private val _currentAssignedClass = MutableStateFlow(
        prefs.getString(KEY_STAFF_ASSIGNED_CLASS, "") ?: ""
    )
    val currentAssignedClass: StateFlow<String> = _currentAssignedClass.asStateFlow()

    private val _schoolInfo = MutableStateFlow(loadSchoolInfo())
    val schoolInfo: StateFlow<SchoolInfo> = _schoolInfo.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _attendanceReminderEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ATTENDANCE_REMINDER_ENABLED, true)
    )
    val attendanceReminderEnabled: StateFlow<Boolean> = _attendanceReminderEnabled.asStateFlow()

    private val _attendanceReminderHour = MutableStateFlow(
        prefs.getInt(KEY_ATTENDANCE_REMINDER_HOUR, 8)
    )
    val attendanceReminderHour: StateFlow<Int> = _attendanceReminderHour.asStateFlow()

    private val _attendanceReminderMinute = MutableStateFlow(
        prefs.getInt(KEY_ATTENDANCE_REMINDER_MINUTE, 30)
    )
    val attendanceReminderMinute: StateFlow<Int> = _attendanceReminderMinute.asStateFlow()

    private val _anniversaryAlertsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ANNIVERSARY_ALERTS_ENABLED, true)
    )
    val anniversaryAlertsEnabled: StateFlow<Boolean> = _anniversaryAlertsEnabled.asStateFlow()

    private val _feeReminderEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_FEE_REMINDER_ENABLED, true)
    )
    val feeReminderEnabled: StateFlow<Boolean> = _feeReminderEnabled.asStateFlow()

    private val _feeReminderLeadDays = MutableStateFlow(
        prefs.getInt(KEY_FEE_REMINDER_LEAD_DAYS, 7)
    )
    val feeReminderLeadDays: StateFlow<Int> = _feeReminderLeadDays.asStateFlow()

    fun login(pinOrPass: String): Boolean {
        return loginWithRole(pinOrCode = pinOrPass, role = StaffRole.ADMIN, staffName = "Administrator")
    }

    fun loginWithRole(
        pinOrCode: String,
        role: StaffRole,
        staffName: String? = null,
        expectedStaffId: String? = null,
        assignedClass: String = ""
    ): Boolean {
        val savedPin = prefs.getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        val trimmedInput = pinOrCode.trim()
        val isAuthorized = when (role) {
            StaffRole.ADMIN -> trimmedInput == savedPin
            StaffRole.CLASS_TEACHER, StaffRole.SUBJECT_TEACHER -> {
                trimmedInput == savedPin ||
                    (!expectedStaffId.isNullOrBlank() && trimmedInput.equals(expectedStaffId.trim(), ignoreCase = true))
            }
        }
        if (isAuthorized) {
            val resolvedName = staffName?.takeIf { it.isNotBlank() } ?: role.displayName
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_STAFF_ROLE, role.name)
                .putString(KEY_STAFF_NAME, resolvedName)
                .putString(KEY_STAFF_ASSIGNED_CLASS, assignedClass)
                .apply()
            _isAuthenticated.value = true
            _currentRole.value = role
            _currentStaffName.value = resolvedName
            _currentAssignedClass.value = assignedClass
            return true
        }
        return false
    }

    fun switchActiveRole(role: StaffRole, staffName: String, assignedClass: String = "") {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_STAFF_ROLE, role.name)
            .putString(KEY_STAFF_NAME, staffName)
            .putString(KEY_STAFF_ASSIGNED_CLASS, assignedClass)
            .apply()
        _isAuthenticated.value = true
        _currentRole.value = role
        _currentStaffName.value = staffName
        _currentAssignedClass.value = assignedClass
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        _isAuthenticated.value = false
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val savedPin = prefs.getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        if (oldPin == savedPin && newPin.isNotBlank()) {
            prefs.edit().putString(KEY_ADMIN_PIN, newPin).apply()
            return true
        }
        return false
    }

    fun updateSchoolInfo(info: SchoolInfo) {
        prefs.edit()
            .putString(KEY_SCHOOL_NAME, info.name)
            .putString(KEY_SCHOOL_LOCATION, info.location)
            .putString(KEY_SCHOOL_PHONE, info.phoneNumber)
            .putString(KEY_SCHOOL_EMAIL, info.email)
            .putString(KEY_SCHOOL_MOTTO, info.motto)
            .apply()
        _schoolInfo.value = info
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAttendanceReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_ATTENDANCE_REMINDER_ENABLED, enabled)
            .putInt(KEY_ATTENDANCE_REMINDER_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_ATTENDANCE_REMINDER_MINUTE, minute.coerceIn(0, 59))
            .apply()
        _attendanceReminderEnabled.value = enabled
        _attendanceReminderHour.value = hour.coerceIn(0, 23)
        _attendanceReminderMinute.value = minute.coerceIn(0, 59)
    }

    fun setAnniversaryAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANNIVERSARY_ALERTS_ENABLED, enabled).apply()
        _anniversaryAlertsEnabled.value = enabled
    }

    fun setFeeReminderSettings(enabled: Boolean, leadDays: Int) {
        val safeDays = leadDays.coerceIn(1, 30)
        prefs.edit()
            .putBoolean(KEY_FEE_REMINDER_ENABLED, enabled)
            .putInt(KEY_FEE_REMINDER_LEAD_DAYS, safeDays)
            .apply()
        _feeReminderEnabled.value = enabled
        _feeReminderLeadDays.value = safeDays
    }

    fun isAttendanceReminderEnabledDirect(): Boolean =
        prefs.getBoolean(KEY_ATTENDANCE_REMINDER_ENABLED, true)

    fun isAnniversaryAlertsEnabledDirect(): Boolean =
        prefs.getBoolean(KEY_ANNIVERSARY_ALERTS_ENABLED, true)

    fun isFeeReminderEnabledDirect(): Boolean =
        prefs.getBoolean(KEY_FEE_REMINDER_ENABLED, true)

    fun getFeeReminderLeadDaysDirect(): Int =
        prefs.getInt(KEY_FEE_REMINDER_LEAD_DAYS, 7)

    fun getAttendanceReminderHourDirect(): Int =
        prefs.getInt(KEY_ATTENDANCE_REMINDER_HOUR, 8)

    fun getAttendanceReminderMinuteDirect(): Int =
        prefs.getInt(KEY_ATTENDANCE_REMINDER_MINUTE, 30)

    private fun loadCurrentRole(): StaffRole {
        val saved = prefs.getString(KEY_STAFF_ROLE, StaffRole.ADMIN.name) ?: StaffRole.ADMIN.name
        return try {
            StaffRole.valueOf(saved)
        } catch (e: Exception) {
            StaffRole.ADMIN
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun loadSchoolInfo(): SchoolInfo {
        return SchoolInfo(
            name = prefs.getString(KEY_SCHOOL_NAME, "UMURIDDINI ISLAMIYYA PRIMARY SCHOOL") ?: "UMURIDDINI ISLAMIYYA PRIMARY SCHOOL",
            location = prefs.getString(KEY_SCHOOL_LOCATION, "ZANGO MARIKITA, UNGOGO LOCAL GOVERNMENT, KANO, NIGERIA") ?: "ZANGO MARIKITA, UNGOGO LOCAL GOVERNMENT, KANO, NIGERIA",
            phoneNumber = prefs.getString(KEY_SCHOOL_PHONE, "+234 803 000 1234") ?: "+234 803 000 1234",
            email = prefs.getString(KEY_SCHOOL_EMAIL, "admin@umuriddin.sch.ng") ?: "admin@umuriddin.sch.ng",
            motto = prefs.getString(KEY_SCHOOL_MOTTO, "Knowledge • Faith • Character") ?: "Knowledge • Faith • Character",
            establishedYear = "2005"
        )
    }

    companion object {
        private const val KEY_ADMIN_PIN = "admin_pin_code"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_STAFF_ROLE = "active_staff_role"
        private const val KEY_STAFF_NAME = "active_staff_name"
        private const val KEY_STAFF_ASSIGNED_CLASS = "active_staff_assigned_class"
        const val DEFAULT_PIN = "1234"

        private const val KEY_SCHOOL_NAME = "school_name"
        private const val KEY_SCHOOL_LOCATION = "school_location"
        private const val KEY_SCHOOL_PHONE = "school_phone"
        private const val KEY_SCHOOL_EMAIL = "school_email"
        private const val KEY_SCHOOL_MOTTO = "school_motto"
        private const val KEY_THEME_MODE = "app_theme_mode"

        private const val KEY_ATTENDANCE_REMINDER_ENABLED = "attendance_reminder_enabled"
        private const val KEY_ATTENDANCE_REMINDER_HOUR = "attendance_reminder_hour"
        private const val KEY_ATTENDANCE_REMINDER_MINUTE = "attendance_reminder_minute"
        private const val KEY_ANNIVERSARY_ALERTS_ENABLED = "anniversary_alerts_enabled"
        private const val KEY_FEE_REMINDER_ENABLED = "fee_reminder_enabled"
        private const val KEY_FEE_REMINDER_LEAD_DAYS = "fee_reminder_lead_days"
    }
}
