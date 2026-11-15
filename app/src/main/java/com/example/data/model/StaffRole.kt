package com.example.data.model

enum class StaffRole(
    val displayName: String,
    val badgeShort: String,
    val description: String
) {
    ADMIN(
        displayName = "Admin",
        badgeShort = "Admin",
        description = "Full access: manage staff, edit/delete student records, view sensitive info, export & system settings"
    ),
    CLASS_TEACHER(
        displayName = "Class Teacher",
        badgeShort = "Class Teacher",
        description = "View student profiles & parent contacts, mark daily attendance, record grades & send parent SMS"
    ),
    SUBJECT_TEACHER(
        displayName = "Subject Teacher",
        badgeShort = "Subject Teacher",
        description = "View class rosters & record student subject grades; restricted from sensitive biodata & staff/admin controls"
    );

    val canManageStaff: Boolean get() = this == ADMIN
    val canManageClasses: Boolean get() = this == ADMIN
    val canAddOrEditStudents: Boolean get() = this == ADMIN
    val canDeleteRecords: Boolean get() = this == ADMIN
    val canExportSensitiveData: Boolean get() = this == ADMIN
    val canManageSettingsAndBackups: Boolean get() = this == ADMIN
    val canViewSensitiveStudentDetails: Boolean get() = this == ADMIN || this == CLASS_TEACHER
    val canViewFeeRecords: Boolean get() = this == ADMIN || this == CLASS_TEACHER
    val canManageFeeRecords: Boolean get() = this == ADMIN
    val canMarkAttendance: Boolean get() = this == ADMIN || this == CLASS_TEACHER
    val canSendSms: Boolean get() = this == ADMIN || this == CLASS_TEACHER
    val canRecordGrades: Boolean get() = true

    companion object {
        fun fromString(value: String?): StaffRole {
            return when (value?.trim()?.lowercase()) {
                "admin", "administrator" -> ADMIN
                "subject teacher", "subject_teacher" -> SUBJECT_TEACHER
                else -> CLASS_TEACHER
            }
        }

        val allRoles: List<StaffRole> = listOf(ADMIN, CLASS_TEACHER, SUBJECT_TEACHER)
    }
}
