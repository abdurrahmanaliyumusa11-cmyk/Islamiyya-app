package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.ui.ClassAttendanceSummary
import com.example.ui.SchoolViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.ReportExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDashboardScreen(
    viewModel: SchoolViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val allClasses by viewModel.allClasses.collectAsState()
    val selectedDate by viewModel.selectedDashboardDate.collectAsState()
    val selectedMonth by viewModel.selectedDashboardMonth.collectAsState()

    val monthRecords by viewModel.attendanceForSelectedMonth.collectAsState()
    val dateRecords by viewModel.attendanceForSelectedDate.collectAsState()
    val classSummaries by viewModel.classAttendanceSummaries.collectAsState()

    BackHandler { onBack() }

    // Overall metrics for the selected date
    val totalEnrolledSchool = classSummaries.sumOf { it.totalEnrolled }
    val totalPresentSchool = classSummaries.sumOf { it.presentCount }
    val totalAbsentSchool = classSummaries.sumOf { it.absentCount }
    val totalLateSchool = classSummaries.sumOf { it.lateCount }
    val totalExcusedSchool = classSummaries.sumOf { it.excusedCount }
    val overallAttendanceRate = if (totalEnrolledSchool > 0 && dateRecords.isNotEmpty()) {
        (totalPresentSchool * 100 / totalEnrolledSchool).coerceIn(0, 100)
    } else 0

    // Calendar logic for selected month
    val (yearInt, monthInt) = remember(selectedMonth) {
        val parts = selectedMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 10) - 1
        y to m
    }

    val monthDisplayName = remember(yearInt, monthInt) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, yearInt)
            set(Calendar.MONTH, monthInt)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Days in this month
    val daysInMonth = remember(yearInt, monthInt) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, yearInt)
            set(Calendar.MONTH, monthInt)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Day of week of 1st day (1=Sun, 2=Mon, etc.)
    val firstDayOffset = remember(yearInt, monthInt) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, yearInt)
            set(Calendar.MONTH, monthInt)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // convert so Mon=0, Tue=1, ..., Sun=6
        (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    Scaffold(
        modifier = modifier.testTag("attendance_dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Attendance Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Calendar overview & class trends",
                            fontSize = 11.sp,
                            color = IslamicGoldAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            ReportExporter.printAttendanceSheet(
                                context = context,
                                className = "Whole School",
                                date = selectedDate,
                                teacherName = "Class Teachers",
                                records = dateRecords,
                                schoolInfo = schoolInfo
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print Summary", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicGreenPrimary)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Calendar Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .testTag("attendance_calendar_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month Navigation Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newMonth = changeMonth(selectedMonth, -1)
                                    viewModel.setDashboardMonth(newMonth)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = IslamicGreenPrimary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = IslamicGreenPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = monthDisplayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newMonth = changeMonth(selectedMonth, 1)
                                    viewModel.setDashboardMonth(newMonth)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month", tint = IslamicGreenPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day headers (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayLabel ->
                                Text(
                                    text = dayLabel,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Calendar Grid Days
                        val totalCells = firstDayOffset + daysInMonth
                        val numRows = (totalCells + 6) / 7

                        for (row in 0 until numRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - firstDayOffset + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val dayDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", yearInt, monthInt + 1, dayNum)
                                        val isSelected = dayDateStr == selectedDate

                                        // Compute attendance status for this date from monthRecords
                                        val dayRecords = monthRecords.filter { it.date == dayDateStr }
                                        val hasDayData = dayRecords.isNotEmpty()
                                        val dayPresent = dayRecords.count { it.status.equals("Present", ignoreCase = true) }
                                        val dayRate = if (hasDayData) (dayPresent * 100 / dayRecords.size) else 0

                                        val dotColor = when {
                                            !hasDayData -> Color.Transparent
                                            dayRate >= 90 -> Color(0xFF198754) // Green
                                            dayRate >= 75 -> Color(0xFFFD7E14) // Orange
                                            else -> Color(0xFFDC3545) // Red
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) IslamicGreenPrimary else if (hasDayData) IslamicGreenPrimary.copy(alpha = 0.08f) else Color.Transparent
                                                )
                                                .border(
                                                    if (isSelected) 1.5.dp else 0.dp,
                                                    if (isSelected) IslamicGoldAccent else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.setDashboardDate(dayDateStr)
                                                }
                                                .testTag("calendar_day_$dayNum"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (hasDayData) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) IslamicGoldAccent else dotColor)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Calendar Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(color = Color(0xFF198754), label = "≥ 90% Present")
                            LegendItem(color = Color(0xFFFD7E14), label = "75 - 89%")
                            LegendItem(color = Color(0xFFDC3545), label = "< 75%")
                        }
                    }
                }
            }

            // Daily Summary KPI Card for Selected Date
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Summary for $selectedDate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (dateRecords.isNotEmpty()) "School Overall Attendance" else "No attendance logged for this date",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Big KPI Rate Badge
                            Surface(
                                color = if (overallAttendanceRate >= 80) Color(0xFFD1E7DD) else Color(0xFFF8D7DA),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$overallAttendanceRate% Rate",
                                    color = if (overallAttendanceRate >= 80) Color(0xFF0F5132) else Color(0xFF842029),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Present, Absent, Late, Excused KPI Counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiCounterCard(
                                title = "Present",
                                count = totalPresentSchool,
                                color = Color(0xFF198754),
                                bgColor = Color(0xFFD1E7DD),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCounterCard(
                                title = "Absent",
                                count = totalAbsentSchool,
                                color = Color(0xFFDC3545),
                                bgColor = Color(0xFFF8D7DA),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCounterCard(
                                title = "Late",
                                count = totalLateSchool,
                                color = Color(0xFFFD7E14),
                                bgColor = Color(0xFFFFE5D0),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCounterCard(
                                title = "Excused",
                                count = totalExcusedSchool,
                                color = Color(0xFF0D6EFD),
                                bgColor = Color(0xFFCFE2FF),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section Header: Summary for Each Class
            item {
                SectionHeader(
                    title = "Class-by-Class Attendance Summary",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Class-by-Class Summary Cards
            items(classSummaries, key = { it.className }) { summary ->
                ClassAttendanceSummaryRowCard(
                    summary = summary,
                    onOpenSheet = {
                        val schoolClass = allClasses.firstOrNull { it.name == summary.className }
                        if (schoolClass != null) {
                            viewModel.attendanceDate.value = selectedDate
                            viewModel.openAttendanceSheet(schoolClass)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ClassAttendanceSummaryRowCard(
    summary: ClassAttendanceSummary,
    onOpenSheet: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("class_attendance_summary_${summary.className.replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Class Name, Teacher & Attendance Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, tint = IslamicGreenPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.className,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (summary.classTeacher.isNotBlank()) "Teacher: ${summary.classTeacher}" else "Teacher: Unassigned",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.hasRecords) {
                    Surface(
                        color = if (summary.attendanceRate >= 80) Color(0xFFD1E7DD) else Color(0xFFF8D7DA),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${summary.attendanceRate}% Present",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (summary.attendanceRate >= 80) Color(0xFF0F5132) else Color(0xFF842029),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Not Marked",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Present, Absent, Late Counts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusCountBadge(
                    label = "Present",
                    count = summary.presentCount,
                    textColor = Color(0xFF0F5132),
                    bgColor = Color(0xFFD1E7DD),
                    modifier = Modifier.weight(1f)
                )
                StatusCountBadge(
                    label = "Absent",
                    count = summary.absentCount,
                    textColor = Color(0xFF842029),
                    bgColor = Color(0xFFF8D7DA),
                    modifier = Modifier.weight(1f)
                )
                StatusCountBadge(
                    label = "Late",
                    count = summary.lateCount,
                    textColor = Color(0xFF664D03),
                    bgColor = Color(0xFFFFF3CD),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val progress = if (summary.totalEnrolled > 0 && summary.hasRecords) {
                (summary.presentCount.toFloat() / summary.totalEnrolled).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = IslamicGreenPrimary,
                trackColor = IslamicGreenPrimary.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Enrolled info & Open Sheet action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enrolled: ${summary.totalEnrolled} students",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onOpenSheet,
                    modifier = Modifier.testTag("btn_open_sheet_${summary.className.replace(" ", "_")}")
                ) {
                    Icon(imageVector = Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (summary.hasRecords) "Edit Sheet" else "Record Attendance",
                        color = IslamicGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCountBadge(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = textColor)
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun KpiCounterCard(
    title: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun changeMonth(yearMonth: String, delta: Int): String {
    return try {
        val parts = yearMonth.split("-")
        var y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        var m = parts.getOrNull(1)?.toIntOrNull() ?: 10

        m += delta
        if (m < 1) {
            m = 12
            y -= 1
        } else if (m > 12) {
            m = 1
            y += 1
        }
        String.format(Locale.getDefault(), "%04d-%02d", y, m)
    } catch (e: Exception) {
        yearMonth
    }
}
