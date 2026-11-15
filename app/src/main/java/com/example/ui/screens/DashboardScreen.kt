package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.Screen
import com.example.ui.SchoolViewModel
import com.example.ui.components.QrStudentLookupDialog
import com.example.ui.components.SchoolHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.StatusSuspendedRed
import com.example.util.FeeHelper
import com.example.util.StudentFeeStatus

@Composable
fun DashboardScreen(
    viewModel: SchoolViewModel,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val classes by viewModel.allClasses.collectAsState()
    val upcomingAnniversaries by viewModel.upcomingAnniversaries.collectAsState()
    val feeSummaries by viewModel.studentFeeSummaries.collectAsState()
    val dueAndOverdueFees by viewModel.dueAndOverdueFeeSummaries.collectAsState()
    val isAdmin = isAuthenticated && currentRole.canAddOrEditStudents
    val canViewFees = isAuthenticated && currentRole.canViewFeeRecords
    var showQrLookupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // School Brand Header with Crest
        item {
            SchoolHeader(
                schoolInfo = schoolInfo,
                isAdmin = isAuthenticated,
                roleLabel = "${currentRole.badgeShort} Mode",
                onAdminToggleClick = {
                    if (isAuthenticated) {
                        viewModel.logout()
                    } else {
                        onRequireLogin()
                    }
                }
            )
        }

        // Quick Stats Cards Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Students",
                        count = stats.totalStudents.toString(),
                        icon = Icons.Default.Groups,
                        accentColor = IslamicGreenPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_total_students"),
                        onClick = {
                            viewModel.selectedClassFilter.value = null
                            viewModel.selectedGenderFilter.value = null
                            viewModel.navigateTo(Screen.STUDENTS)
                        }
                    )
                    StatCard(
                        title = "New Admissions",
                        count = stats.newlyAdmittedCount.toString(),
                        icon = Icons.Default.PersonAdd,
                        accentColor = IslamicGoldAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_new_admissions"),
                        onClick = {
                            viewModel.navigateTo(Screen.REPORTS)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Male Students",
                        count = stats.maleCount.toString(),
                        icon = Icons.Default.Male,
                        accentColor = Color(0xFF0288D1),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_male_students"),
                        onClick = {
                            viewModel.selectedGenderFilter.value = "Male"
                            viewModel.navigateTo(Screen.STUDENTS)
                        }
                    )
                    StatCard(
                        title = "Female Students",
                        count = stats.femaleCount.toString(),
                        icon = Icons.Default.Female,
                        accentColor = Color(0xFF8E24AA),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_female_students"),
                        onClick = {
                            viewModel.selectedGenderFilter.value = "Female"
                            viewModel.navigateTo(Screen.STUDENTS)
                        }
                    )
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (isAdmin) {
                            viewModel.startAddNewStudent()
                        } else if (!isAuthenticated) {
                            onRequireLogin()
                        } else {
                            viewModel.showAccessDeniedMessage("Admin")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("dashboard_add_student_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Register", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.TEACHERS) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_teachers_button")
                ) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Teachers", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.REPORTS) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_reports_button")
                ) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reports", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                }
            }
        }

        // Attendance Trends & Calendar Overview Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { viewModel.openAttendanceDashboard() }
                    .testTag("dashboard_attendance_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Attendance Trends & Calendar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calendar overview, roll calls & class summaries",
                                fontSize = 11.sp,
                                color = IslamicGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Attendance Dashboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick QR Student ID Lookup Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { showQrLookupDialog = true }
                    .testTag("dashboard_qr_lookup_button"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Student QR Code",
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Quick QR & Student ID Lookup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scan Digital ID card QR or enter ID to open record",
                                fontSize = 11.sp,
                                color = IslamicGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open QR Lookup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Parent SMS Notifications Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        if (isAuthenticated && currentRole.canSendSms) {
                            viewModel.openSmsHub()
                        } else if (!isAuthenticated) {
                            onRequireLogin()
                        } else {
                            viewModel.showAccessDeniedMessage("Class Teacher or Admin")
                        }
                    }
                    .testTag("dashboard_sms_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IslamicGoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "Parent SMS Alerts",
                                tint = Color(0xFF8C6D2E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Parent SMS Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Send attendance alerts, announcements & report card SMS",
                                fontSize = 11.sp,
                                color = IslamicGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Parent SMS Hub",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Tuition & Fee Tracking Banner Card
        item {
            val totalPaidAll = feeSummaries.sumOf { it.totalPaid }
            val totalBalAll = feeSummaries.sumOf { it.balance }
            val overdueCount = dueAndOverdueFees.count { it.status == StudentFeeStatus.OVERDUE }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        if (canViewFees) {
                            viewModel.navigateTo(Screen.FEE_MANAGEMENT)
                        } else if (!isAuthenticated) {
                            onRequireLogin()
                        } else {
                            viewModel.showAccessDeniedMessage("Class Teacher or Admin")
                        }
                    }
                    .testTag("dashboard_fees_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Tuition and Fee Tracking",
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Tuition & Fee Tracking",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (canViewFees && overdueCount > 0) {
                                    Surface(
                                        color = StatusSuspendedRed.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$overdueCount Overdue",
                                            color = StatusSuspendedRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (canViewFees) {
                                    "Collected: ${FeeHelper.formatNaira(totalPaidAll)} • Balance: ${FeeHelper.formatNaira(totalBalAll)}"
                                } else {
                                    "Installments, receipts & parent fee reminders (Admin / Class Teacher)"
                                },
                                fontSize = 11.sp,
                                color = IslamicGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Tuition & Fee Tracking",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Upcoming Admission Anniversaries Alert Card (if any within 14 days)
        if (upcomingAnniversaries.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("dashboard_anniversaries_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = IslamicGoldAccent.copy(alpha = 0.14f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = "Admission Anniversaries",
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upcoming Admission Anniversaries (${upcomingAnniversaries.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = IslamicGreenPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        upcomingAnniversaries.take(3).forEach { anniv ->
                            val dayText = when (anniv.daysUntil) {
                                0 -> "Today"
                                1 -> "Tomorrow"
                                else -> "In ${anniv.daysUntil} days (${anniv.anniversaryDateFormatted})"
                            }
                            Text(
                                text = "• ${anniv.student.fullName} (${anniv.student.className}) — ${anniv.yearsCompleted} yr(s) • $dayText",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.showStudentProfile(anniv.student) }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Students By Class Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SectionHeader(
                    title = "Students by Class",
                    actionText = "Manage Classes",
                    onActionClick = { viewModel.navigateTo(Screen.CLASSES) },
                    modifier = Modifier.testTag("dashboard_classes_header")
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val total = if (stats.totalStudents > 0) stats.totalStudents else 1
                        classes.forEach { schoolClass ->
                            val enrolled = stats.classDistribution[schoolClass.name] ?: 0
                            val progress = (enrolled.toFloat() / total).coerceIn(0f, 1f)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectedClassFilter.value = schoolClass.name
                                        viewModel.navigateTo(Screen.STUDENTS)
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("class_bar_${schoolClass.name.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(IslamicGreenPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = schoolClass.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "$enrolled students",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = IslamicGreenPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = IslamicGreenPrimary,
                                    trackColor = IslamicGreenPrimary.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Admissions Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SectionHeader(
                    title = "Recent Admissions",
                    actionText = "View All",
                    onActionClick = {
                        viewModel.selectedClassFilter.value = null
                        viewModel.selectedGenderFilter.value = null
                        viewModel.searchQuery.value = ""
                        viewModel.navigateTo(Screen.STUDENTS)
                    },
                    modifier = Modifier.testTag("dashboard_recent_header")
                )

                if (stats.recentAdmissions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent admissions recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    stats.recentAdmissions.forEach { student ->
                        RecentStudentCard(
                            student = student,
                            onClick = { viewModel.showStudentProfile(student) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showQrLookupDialog) {
        QrStudentLookupDialog(
            findStudentByPayload = { payload -> viewModel.findStudentByQrPayload(payload) },
            onStudentRetrieved = { student ->
                showQrLookupDialog = false
                viewModel.showStudentProfile(student)
            },
            onDismiss = { showQrLookupDialog = false }
        )
    }
}

@Composable
private fun RecentStudentCard(
    student: Student,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_student_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(
                photoUri = student.photoUri,
                fullName = student.fullName,
                gender = student.gender,
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = student.registrationNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IslamicGreenPrimary
                    )
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = student.className,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(status = student.admissionStatus)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
