package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.SchoolClass
import com.example.data.model.Student
import com.example.ui.SchoolViewModel
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.ReportExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassAttendanceScreen(
    schoolClass: SchoolClass,
    viewModel: SchoolViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allStudents by viewModel.allStudents.collectAsState()
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val currentDateStr by viewModel.attendanceDate.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val canMarkAttendance = isAuthenticated && currentRole.canMarkAttendance
    val canSendSms = isAuthenticated && currentRole.canSendSms

    // Students enrolled in this class
    val classStudents = remember(allStudents, schoolClass.name) {
        allStudents.filter { it.className == schoolClass.name }.sortedBy { it.fullName }
    }

    // Local mutable state for student attendance status and remarks
    // studentId -> status ("Present", "Absent", "Late", "Excused")
    val attendanceStatusMap = remember { mutableStateMapOf<Long, String>() }
    val attendanceRemarksMap = remember { mutableStateMapOf<Long, String>() }

    // Load existing records from database for this class and date
    LaunchedEffect(schoolClass.name, currentDateStr, classStudents) {
        val existing = viewModel.getExistingAttendance(schoolClass.name, currentDateStr)
        attendanceStatusMap.clear()
        attendanceRemarksMap.clear()

        if (existing.isNotEmpty()) {
            existing.forEach { record ->
                attendanceStatusMap[record.studentId] = record.status
                attendanceRemarksMap[record.studentId] = record.remarks
            }
        }
        // Fill any students not yet marked with default "Present"
        classStudents.forEach { s ->
            if (!attendanceStatusMap.containsKey(s.id)) {
                attendanceStatusMap[s.id] = "Present"
                attendanceRemarksMap[s.id] = ""
            }
        }
    }

    BackHandler { onBack() }

    // Date picker dialog setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        val parts = currentDateStr.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.attendanceDate.value = formatted
        }, y, m, d)
    }

    // Compute stats
    val totalCount = classStudents.size
    val presentCount = classStudents.count { attendanceStatusMap[it.id] == "Present" }
    val absentCount = classStudents.count { attendanceStatusMap[it.id] == "Absent" }
    val lateCount = classStudents.count { attendanceStatusMap[it.id] == "Late" }
    val excusedCount = classStudents.count { attendanceStatusMap[it.id] == "Excused" }
    val attendanceRate = if (totalCount > 0) (presentCount * 100 / totalCount) else 0

    Scaffold(
        modifier = modifier.testTag("class_attendance_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${schoolClass.name} Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Teacher: ${if (schoolClass.classTeacher.isNotBlank()) schoolClass.classTeacher else "Unassigned"}",
                            fontSize = 11.sp,
                            color = IslamicGoldAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("attendance_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick print button
                    IconButton(
                        onClick = {
                            val records = classStudents.map { s ->
                                AttendanceRecord(
                                    date = currentDateStr,
                                    className = schoolClass.name,
                                    studentId = s.id,
                                    studentName = s.fullName,
                                    studentRegNo = s.registrationNumber,
                                    status = attendanceStatusMap[s.id] ?: "Present",
                                    remarks = attendanceRemarksMap[s.id] ?: "",
                                    markedBy = schoolClass.classTeacher
                                )
                            }
                            ReportExporter.printAttendanceSheet(
                                context = context,
                                className = schoolClass.name,
                                date = currentDateStr,
                                teacherName = schoolClass.classTeacher,
                                records = records,
                                schoolInfo = schoolInfo
                            )
                        },
                        modifier = Modifier.testTag("attendance_print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Attendance",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicGreenPrimary)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export CSV
                    OutlinedButton(
                        onClick = {
                            val records = classStudents.map { s ->
                                AttendanceRecord(
                                    date = currentDateStr,
                                    className = schoolClass.name,
                                    studentId = s.id,
                                    studentName = s.fullName,
                                    studentRegNo = s.registrationNumber,
                                    status = attendanceStatusMap[s.id] ?: "Present",
                                    remarks = attendanceRemarksMap[s.id] ?: "",
                                    markedBy = schoolClass.classTeacher
                                )
                            }
                            val file = ReportExporter.exportAttendanceToCsv(
                                context = context,
                                className = schoolClass.name,
                                date = currentDateStr,
                                teacherName = schoolClass.classTeacher,
                                records = records,
                                schoolInfo = schoolInfo
                            )
                            if (file != null) {
                                ReportExporter.shareCsvFile(context, file, "${schoolClass.name} Attendance ($currentDateStr)")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_export_attendance_csv")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Save Attendance Button
                    Button(
                        onClick = {
                            if (!canMarkAttendance) {
                                viewModel.showAccessDeniedMessage("Class Teacher or Admin")
                                return@Button
                            }
                            val recordsToSave = classStudents.map { s ->
                                AttendanceRecord(
                                    date = currentDateStr,
                                    className = schoolClass.name,
                                    studentId = s.id,
                                    studentName = s.fullName,
                                    studentRegNo = s.registrationNumber,
                                    status = attendanceStatusMap[s.id] ?: "Present",
                                    remarks = attendanceRemarksMap[s.id] ?: "",
                                    markedBy = schoolClass.classTeacher
                                )
                            }
                            viewModel.saveAttendance(recordsToSave) {
                                // optional callback
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(48.dp)
                            .testTag("btn_save_attendance")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Attendance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date Picker Header Bar
            Surface(
                color = IslamicGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous day button
                    IconButton(
                        onClick = {
                            changeDayBy(viewModel, currentDateStr, -1)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Center Date selector
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { datePickerDialog.show() }
                            .testTag("attendance_date_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = IslamicGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentDateStr,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Next day button
                    IconButton(
                        onClick = {
                            changeDayBy(viewModel, currentDateStr, 1)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Attendance Statistics Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Register Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$attendanceRate% Present",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = IslamicGreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatMiniPill(label = "Total", count = totalCount, color = MaterialTheme.colorScheme.onSurface)
                        StatMiniPill(label = "Present", count = presentCount, color = Color(0xFF198754))
                        StatMiniPill(label = "Absent", count = absentCount, color = Color(0xFFDC3545))
                        StatMiniPill(label = "Late", count = lateCount, color = Color(0xFFFD7E14))
                        StatMiniPill(label = "Excused", count = excusedCount, color = Color(0xFF0D6EFD))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Mark All Present & Notify Absent Parents Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Students: ${classStudents.size} enrolled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (absentCount + lateCount > 0 && canSendSms) {
                                TextButton(
                                    onClick = {
                                        val flaggedIds = classStudents
                                            .filter {
                                                val st = attendanceStatusMap[it.id]
                                                st == "Absent" || st == "Late"
                                            }
                                            .map { it.id }
                                            .toSet()
                                        viewModel.openSmsHub(
                                            preselectedIds = flaggedIds,
                                            template = com.example.util.SmsEventTemplate.ATTENDANCE_ALERT
                                        )
                                    },
                                    modifier = Modifier.testTag("btn_notify_absent_sms")
                                ) {
                                    Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFDC3545))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS Absent (${absentCount + lateCount})", color = Color(0xFFDC3545), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            TextButton(
                                onClick = {
                                    classStudents.forEach { s ->
                                        attendanceStatusMap[s.id] = "Present"
                                    }
                                },
                                modifier = Modifier.testTag("btn_mark_all_present")
                            ) {
                                Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark All Present", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Students Roll-Call List
            if (classStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No students enrolled in ${schoolClass.name} yet.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Assign students to this class in the Student Registration form.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(classStudents, key = { it.id }) { student ->
                        val currentStatus = attendanceStatusMap[student.id] ?: "Present"
                        val currentRemarks = attendanceRemarksMap[student.id] ?: ""

                        StudentAttendanceRowCard(
                            student = student,
                            currentStatus = currentStatus,
                            currentRemarks = currentRemarks,
                            onStatusSelected = { status ->
                                attendanceStatusMap[student.id] = status
                            },
                            onRemarksChanged = { remarks ->
                                attendanceRemarksMap[student.id] = remarks
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentAttendanceRowCard(
    student: Student,
    currentStatus: String,
    currentRemarks: String,
    onStatusSelected: (String) -> Unit,
    onRemarksChanged: (String) -> Unit
) {
    var showRemarksInput by remember { mutableStateOf(currentRemarks.isNotBlank()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_row_${student.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudentAvatar(
                    photoUri = student.photoUri,
                    fullName = student.fullName,
                    gender = student.gender,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${student.registrationNumber} • ${student.gender}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showRemarksInput = !showRemarksInput },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Remarks",
                        tint = if (currentRemarks.isNotBlank()) IslamicGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Status Pill Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("Present", "P", Color(0xFF198754)),
                    Triple("Absent", "A", Color(0xFFDC3545)),
                    Triple("Late", "L", Color(0xFFFD7E14)),
                    Triple("Excused", "E", Color(0xFF0D6EFD))
                ).forEach { (statusName, shortLabel, color) ->
                    val isSelected = currentStatus.equals(statusName, ignoreCase = true)

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStatusSelected(statusName) }
                            .testTag("btn_${statusName.lowercase()}_${student.id}"),
                        color = if (isSelected) color else color.copy(alpha = 0.08f),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$shortLabel - $statusName",
                                    color = if (isSelected) Color.White else color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Optional Remarks Input Field
            if (showRemarksInput) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentRemarks,
                    onValueChange = onRemarksChanged,
                    placeholder = { Text("Teacher remarks / reason (e.g. sick with doctor's note)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun StatMiniPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun changeDayBy(viewModel: SchoolViewModel, dateStr: String, deltaDays: Int) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        viewModel.attendanceDate.value = sdf.format(cal.time)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
