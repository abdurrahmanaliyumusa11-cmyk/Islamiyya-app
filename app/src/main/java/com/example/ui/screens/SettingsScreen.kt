package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.UploadFile
import com.example.util.NotificationHelper
import com.example.util.ThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.SchoolInfo
import com.example.ui.SchoolViewModel
import com.example.ui.components.CsvBulkImportDialog
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary

@Composable
fun SettingsScreen(
    viewModel: SchoolViewModel,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentStaffName by viewModel.currentStaffName.collectAsState()
    val isAdmin = isAuthenticated && currentRole.canManageSettingsAndBackups
    val currentThemeMode by viewModel.themeMode.collectAsState()

    val attendanceReminderEnabled by viewModel.attendanceReminderEnabled.collectAsState()
    val attendanceReminderHour by viewModel.attendanceReminderHour.collectAsState()
    val attendanceReminderMinute by viewModel.attendanceReminderMinute.collectAsState()
    val anniversaryAlertsEnabled by viewModel.anniversaryAlertsEnabled.collectAsState()
    val upcomingAnniversaries by viewModel.upcomingAnniversaries.collectAsState()
    val feeReminderEnabled by viewModel.feeReminderEnabled.collectAsState()
    val feeReminderLeadDays by viewModel.feeReminderLeadDays.collectAsState()
    val dueAndOverdueFees by viewModel.dueAndOverdueFeeSummaries.collectAsState()

    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            pendingNotificationAction?.invoke()
        } else {
            viewModel.showMessage("Notification permission denied. Enable it in Android Settings to receive alerts.")
        }
        pendingNotificationAction = null
    }

    fun runWithNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(context)) {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showEditSchoolDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Screen Header Bar
        Surface(
            color = IslamicGreenPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "System Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Configure school details, administrator security & database backups",
                    fontSize = 12.sp,
                    color = IslamicGoldAccent
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // School Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, IslamicGoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_school_logo),
                                contentDescription = "School Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = schoolInfo.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = schoolInfo.location,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Phone: ${schoolInfo.phoneNumber}", fontSize = 13.sp)
                    Text("Email: ${schoolInfo.email}", fontSize = 13.sp)
                    Text("Motto: \"${schoolInfo.motto}\"", fontSize = 13.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (isAdmin) {
                                showEditSchoolDialog = true
                            } else if (!isAuthenticated) {
                                onRequireLogin()
                            } else {
                                viewModel.showAccessDeniedMessage("Admin")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit School Information", color = IslamicGreenPrimary)
                    }
                }
            }

            // Theme & Appearance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessAuto,
                                contentDescription = null,
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Appearance & Theme",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose Light, Dark, or follow System Default",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionPill(
                            title = "System",
                            subtitle = "Default",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = currentThemeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f).testTag("theme_option_system")
                        )
                        ThemeOptionPill(
                            title = "Light",
                            subtitle = "Day",
                            icon = Icons.Default.LightMode,
                            isSelected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f).testTag("theme_option_light")
                        )
                        ThemeOptionPill(
                            title = "Dark",
                            subtitle = "Night",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentThemeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f).testTag("theme_option_dark")
                        )
                    }
                }
            }

            // Local Notifications & Reminders Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notifications_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Notifications",
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Notifications & Reminders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasNotifPermission) "Alerts active for attendance & anniversaries" else "Tap below to grant notification permission",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Daily Attendance Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Attendance Reminder",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Remind teachers to mark daily attendance",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = attendanceReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    runWithNotificationPermission {
                                        viewModel.updateAttendanceReminder(true, attendanceReminderHour, attendanceReminderMinute)
                                    }
                                } else {
                                    viewModel.updateAttendanceReminder(false, attendanceReminderHour, attendanceReminderMinute)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IslamicGreenPrimary),
                            modifier = Modifier.testTag("switch_attendance_reminder")
                        )
                    }

                    if (attendanceReminderEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(7 to 30, 8 to 0, 8 to 30, 9 to 0, 10 to 0).forEach { (h, m) ->
                                val selected = attendanceReminderHour == h && attendanceReminderMinute == m
                                val label = String.format(java.util.Locale.getDefault(), "%02d:%02d AM", h, m)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.updateAttendanceReminder(true, h, m) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IslamicGreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Admission Anniversaries Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admission Anniversary Alerts",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Notify admin (${upcomingAnniversaries.size} upcoming in 14 days)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = anniversaryAlertsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    runWithNotificationPermission {
                                        viewModel.setAnniversaryAlertsEnabled(true)
                                    }
                                } else {
                                    viewModel.setAnniversaryAlertsEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IslamicGreenPrimary),
                            modifier = Modifier.testTag("switch_anniversary_alerts")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tuition Fee Due-Date Alerts Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tuition Due-Date Alerts",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automated fee alerts (${dueAndOverdueFees.size} due/overdue • $feeReminderLeadDays-day lead)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = feeReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    runWithNotificationPermission {
                                        viewModel.updateFeeReminderSettings(true, feeReminderLeadDays)
                                    }
                                } else {
                                    viewModel.updateFeeReminderSettings(false, feeReminderLeadDays)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IslamicGreenPrimary),
                            modifier = Modifier.testTag("switch_fee_reminder")
                        )
                    }

                    if (feeReminderEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(3, 7, 14).forEach { days ->
                                FilterChip(
                                    selected = feeReminderLeadDays == days,
                                    onClick = { viewModel.updateFeeReminderSettings(true, days) },
                                    label = { Text("$days days before", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IslamicGreenPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("fee_lead_days_$days")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                runWithNotificationPermission {
                                    viewModel.triggerTestAttendanceReminder()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_attendance_notification")
                        ) {
                            Text("Test Attend.", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                runWithNotificationPermission {
                                    viewModel.triggerAnniversaryCheckNow()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_check_anniversaries_now")
                        ) {
                            Icon(imageVector = Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(14.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anniv.", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                runWithNotificationPermission {
                                    viewModel.triggerFeeReminderCheckNow()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_check_due_fees_now")
                        ) {
                            Text("Check Fees", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Administrator Security & Role Access Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAuthenticated) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = IslamicGreenPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Role & Security Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAuthenticated) "$currentStaffName (${currentRole.displayName})" else "Status: Locked",
                                    fontSize = 12.sp,
                                    color = if (isAuthenticated) Color(0xFF198754) else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isAuthenticated) {
                                    viewModel.logout()
                                } else {
                                    onRequireLogin()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAuthenticated) MaterialTheme.colorScheme.error else IslamicGreenPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isAuthenticated) "Lock" else "Unlock")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequireLogin,
                            modifier = Modifier.weight(1f).testTag("btn_switch_staff_role")
                        ) {
                            Text("Switch Role", color = IslamicGreenPrimary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (isAdmin) {
                                    showChangePinDialog = true
                                } else if (!isAuthenticated) {
                                    onRequireLogin()
                                } else {
                                    viewModel.showAccessDeniedMessage("Admin")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change PIN", color = IslamicGreenPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Staff & Teachers Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Staff & Teachers Registry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add, edit, or remove teachers, qualifications, assigned classes and contact information.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.navigateTo(com.example.ui.Screen.TEACHERS) },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Teachers & Staff")
                    }
                }
            }

            // Backup & Data Recovery Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Data Backup & Recovery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Export complete student records and class structures to a secure backup file, or restore from a previous backup.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isAdmin) {
                                    val file = viewModel.exportBackupJson(context)
                                    if (file != null) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, "Umuriddin School Database Backup")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Save or Share Database Backup"))
                                    }
                                } else if (!isAuthenticated) {
                                    onRequireLogin()
                                } else {
                                    viewModel.showAccessDeniedMessage("Admin")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup Data")
                        }

                        OutlinedButton(
                            onClick = {
                                if (isAdmin) {
                                    showRestoreDialog = true
                                } else if (!isAuthenticated) {
                                    onRequireLogin()
                                } else {
                                    viewModel.showAccessDeniedMessage("Admin")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", color = IslamicGreenPrimary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (isAdmin) {
                                showCsvImportDialog = true
                            } else if (!isAuthenticated) {
                                onRequireLogin()
                            } else {
                                viewModel.showAccessDeniedMessage("Admin")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_settings_csv_import")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bulk Import Students via CSV", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // About School Record System Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = IslamicGreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IslamicGreenPrimary
                    )
                    Text(
                        text = "Student Record Management System v1.0",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ungogo Local Government Area, Kano, Nigeria",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onSave = { oldPin, newPin ->
                val ok = viewModel.changePin(oldPin, newPin)
                if (ok) showChangePinDialog = false
                ok
            }
        )
    }

    // Edit School Information Dialog
    if (showEditSchoolDialog) {
        EditSchoolInfoDialog(
            currentInfo = schoolInfo,
            onDismiss = { showEditSchoolDialog = false },
            onSave = { updated ->
                viewModel.updateSchoolProfile(updated)
                showEditSchoolDialog = false
            }
        )
    }

    // Restore Backup Dialog
    if (showRestoreDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreDialog = false },
            onRestore = { json ->
                val ok = viewModel.restoreBackupJson(json)
                if (ok) showRestoreDialog = false
                ok
            }
        )
    }

    if (showCsvImportDialog) {
        CsvBulkImportDialog(
            viewModel = viewModel,
            onDismiss = { showCsvImportDialog = false }
        )
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSave: (oldPin: String, newPin: String) -> Boolean
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Admin PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it; error = null },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it; error = null },
                    label = { Text("New PIN (min 4 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it; error = null },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length < 4) {
                        error = "PIN must be at least 4 digits"
                    } else if (newPin != confirmPin) {
                        error = "New PIN and confirmation do not match"
                    } else {
                        val success = onSave(oldPin, newPin)
                        if (!success) {
                            error = "Current PIN is incorrect"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditSchoolInfoDialog(
    currentInfo: SchoolInfo,
    onDismiss: () -> Unit,
    onSave: (SchoolInfo) -> Unit
) {
    var name by remember { mutableStateOf(currentInfo.name) }
    var location by remember { mutableStateOf(currentInfo.location) }
    var phone by remember { mutableStateOf(currentInfo.phoneNumber) }
    var email by remember { mutableStateOf(currentInfo.email) }
    var motto by remember { mutableStateOf(currentInfo.motto) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("School Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("School Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Address / Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("School Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motto,
                    onValueChange = { motto = it },
                    label = { Text("School Motto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentInfo.copy(
                            name = name.trim(),
                            location = location.trim(),
                            phoneNumber = phone.trim(),
                            email = email.trim(),
                            motto = motto.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Boolean
) {
    var jsonText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore From Backup", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste the JSON content from your exported backup file below to restore student records.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it; error = null },
                    placeholder = { Text("Paste backup JSON here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonText.isBlank()) {
                        error = "Please paste JSON backup content"
                    } else {
                        val success = onRestore(jsonText)
                        if (!success) {
                            error = "Invalid JSON backup data"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
            ) {
                Text("Restore Records")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ThemeOptionPill(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) IslamicGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGoldAccent) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
