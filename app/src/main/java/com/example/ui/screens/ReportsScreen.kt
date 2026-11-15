package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.SchoolViewModel
import com.example.ui.components.FeeStatusBadge
import com.example.ui.components.PdfReceiptExportDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusSuspendedRed
import com.example.util.FeeHelper
import com.example.util.OfficialPdfExporter
import com.example.util.ReportExporter
import com.example.util.StudentFeeStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportCategory(val title: String) {
    ALL("Complete Student Directory"),
    BY_CLASS("Students by Class"),
    BY_GENDER("Students by Gender"),
    NEW_ADMISSIONS("New Admissions"),
    ACTIVE_ONLY("Active Enrolled"),
    FEE_ALL("Tuition Ledger (All)"),
    FEE_OVERDUE("Overdue & Due Fees"),
    FEE_PAID("Cleared / Paid Fees")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: SchoolViewModel,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val classes by viewModel.allClasses.collectAsState()
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val feeSummaries by viewModel.studentFeeSummaries.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isAdmin = isAuthenticated && currentRole.canExportSensitiveData
    val canViewFees = isAuthenticated && currentRole.canViewFeeRecords

    var selectedCategory by remember { mutableStateOf(ReportCategory.ALL) }
    var selectedClassForReport by remember { mutableStateOf(classes.firstOrNull()?.name ?: "Primary 1") }
    var selectedGenderForReport by remember { mutableStateOf("Male") }
    var pendingFeePdfFile by remember { mutableStateOf<File?>(null) }

    val currentYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) }
    val isFeeReport = selectedCategory in setOf(
        ReportCategory.FEE_ALL,
        ReportCategory.FEE_OVERDUE,
        ReportCategory.FEE_PAID
    )

    // Filter students according to current report category
    val reportStudents = remember(selectedCategory, selectedClassForReport, selectedGenderForReport, allStudents) {
        when (selectedCategory) {
            ReportCategory.ALL -> allStudents.sortedBy { it.fullName }
            ReportCategory.BY_CLASS -> allStudents.filter { it.className == selectedClassForReport }.sortedBy { it.fullName }
            ReportCategory.BY_GENDER -> allStudents.filter { it.gender.equals(selectedGenderForReport, ignoreCase = true) }.sortedBy { it.fullName }
            ReportCategory.NEW_ADMISSIONS -> allStudents.filter { it.dateOfAdmission.startsWith(currentYear) }.sortedByDescending { it.dateOfAdmission }
            ReportCategory.ACTIVE_ONLY -> allStudents.filter { it.admissionStatus.equals("Active", ignoreCase = true) }.sortedBy { it.fullName }
            else -> emptyList()
        }
    }

    val reportFeeSummaries = remember(selectedCategory, feeSummaries) {
        when (selectedCategory) {
            ReportCategory.FEE_ALL -> feeSummaries.sortedBy { it.student.fullName }
            ReportCategory.FEE_OVERDUE -> FeeHelper.findDueAndOverdueReminders(feeSummaries)
            ReportCategory.FEE_PAID -> feeSummaries.filter { it.status == StudentFeeStatus.PAID }.sortedBy { it.student.fullName }
            else -> emptyList()
        }
    }

    val reportTitle = when (selectedCategory) {
        ReportCategory.ALL -> "Complete Student Directory"
        ReportCategory.BY_CLASS -> "$selectedClassForReport Student Register"
        ReportCategory.BY_GENDER -> if (selectedGenderForReport == "Male") "Male Students Register" else "Female Students Register"
        ReportCategory.NEW_ADMISSIONS -> "New Admissions Register ($currentYear)"
        ReportCategory.ACTIVE_ONLY -> "Active Students Roster"
        ReportCategory.FEE_ALL -> "Tuition Fee Collection Ledger"
        ReportCategory.FEE_OVERDUE -> "Overdue & Due Tuition Report"
        ReportCategory.FEE_PAID -> "Cleared Tuition Fees Report"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
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
                    text = "Reports & Analytics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Generate, print & export school student registers",
                    fontSize = 12.sp,
                    color = IslamicGoldAccent
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.openAttendanceDashboard() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_reports_attendance_dashboard")
                ) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = IslamicGoldAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Attendance Trends Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Horizontal Category Tabs
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IslamicGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Sub-filters if applicable
        if (selectedCategory == ReportCategory.BY_CLASS) {
            var classMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ExposedDropdownMenuBox(
                    expanded = classMenuExpanded,
                    onExpandedChange = { classMenuExpanded = !classMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedClassForReport,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Class to Generate") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = classMenuExpanded,
                        onDismissRequest = { classMenuExpanded = false }
                    ) {
                        classes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedClassForReport = c.name
                                    classMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        } else if (selectedCategory == ReportCategory.BY_GENDER) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedGenderForReport == "Male",
                    onClick = { selectedGenderForReport = "Male" },
                    label = { Text("Boys (${allStudents.count { it.gender.equals("Male", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0288D1),
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedGenderForReport == "Female",
                    onClick = { selectedGenderForReport = "Female" },
                    label = { Text("Girls (${allStudents.count { it.gender.equals("Female", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF8E24AA),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Report Action Card
        if (isFeeReport && !canViewFees) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("fee_reports_locked_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Financial Reports Restricted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in as Admin or Class Teacher to preview, print, or export tuition fee reports.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequireLogin,
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Sign In / Switch Role", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
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
                                text = reportTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = IslamicGreenPrimary
                            )
                            Text(
                                text = if (isFeeReport) {
                                    val totalPaid = reportFeeSummaries.sumOf { it.totalPaid }
                                    val totalBal = reportFeeSummaries.sumOf { it.balance }
                                    "${reportFeeSummaries.size} Accounts • Paid ${FeeHelper.formatNaira(totalPaid)} • Bal ${FeeHelper.formatNaira(totalBal)}"
                                } else {
                                    "Generated Total: ${reportStudents.size} Records"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // [Print / Save as PDF]
                        Button(
                            onClick = {
                                if (isFeeReport) {
                                    ReportExporter.printFeeSummaryReport(
                                        context = context,
                                        summaries = reportFeeSummaries,
                                        schoolInfo = schoolInfo,
                                        reportTitle = reportTitle
                                    )
                                } else {
                                    ReportExporter.printStudentsReport(
                                        context = context,
                                        students = reportStudents,
                                        schoolInfo = schoolInfo,
                                        reportTitle = reportTitle
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_print_report")
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print / PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // [Export CSV / Excel]
                        OutlinedButton(
                            onClick = {
                                if (isAdmin) {
                                    val file = if (isFeeReport) {
                                        ReportExporter.exportFeesToCsv(
                                            context = context,
                                            summaries = reportFeeSummaries,
                                            schoolInfo = schoolInfo,
                                            reportTitle = reportTitle.replace(' ', '_')
                                        )
                                    } else {
                                        ReportExporter.exportStudentsToCsv(
                                            context = context,
                                            students = reportStudents,
                                            schoolInfo = schoolInfo,
                                            reportTitle = reportTitle.replace(' ', '_')
                                        )
                                    }
                                    if (file != null) {
                                        ReportExporter.shareCsvFile(context, file, reportTitle)
                                    }
                                } else if (!isAuthenticated) {
                                    onRequireLogin()
                                } else {
                                    viewModel.showAccessDeniedMessage("Admin")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_export_csv")
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                        }
                    }

                    if (isFeeReport && isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val file = OfficialPdfExporter.generateFeeSummaryReportPdf(
                                    context = context,
                                    summaries = reportFeeSummaries,
                                    schoolInfo = schoolInfo,
                                    reportTitle = reportTitle
                                )
                                if (file != null) {
                                    pendingFeePdfFile = file
                                } else {
                                    viewModel.showMessage("Could not generate official PDF fee report.")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_download_fee_pdf_report")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = IslamicGreenPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download Official PDF Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = IslamicGreenPrimary
                            )
                        }
                    }
                }
            }

            // Live Table Preview
            Text(
                text = "Report Records Preview",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFeeReport) {
                    itemsIndexed(reportFeeSummaries, key = { _, s -> s.student.id }) { index, summary ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = IslamicGreenPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = IslamicGreenPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = summary.student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${summary.student.className} • Paid ${FeeHelper.formatNaira(summary.totalPaid)} / Bal ${FeeHelper.formatNaira(summary.balance)}",
                                        fontSize = 11.sp,
                                        color = if (summary.balance > 0L) StatusSuspendedRed else StatusActiveGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                FeeStatusBadge(status = summary.status)
                            }
                        }
                    }
                } else {
                    itemsIndexed(reportStudents, key = { _, s -> s.id }) { index, student ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = IslamicGreenPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = IslamicGreenPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = student.registrationNumber,
                                            fontSize = 11.sp,
                                            color = IslamicGreenPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${student.className} (${student.gender})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                StatusBadge(status = student.admissionStatus)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingFeePdfFile?.let { pdfFile ->
        val totalPaid = reportFeeSummaries.sumOf { it.totalPaid }
        val totalBal = reportFeeSummaries.sumOf { it.balance }
        PdfReceiptExportDialog(
            pdfFile = pdfFile,
            title = "Official Fee Report PDF",
            subtitle = reportTitle,
            amountLine = "${reportFeeSummaries.size} Accounts • Paid ${FeeHelper.formatNaira(totalPaid)} • Bal ${FeeHelper.formatNaira(totalBal)}",
            onNotify = { msg -> viewModel.showMessage(msg) },
            onPrintFallback = {
                ReportExporter.printFeeSummaryReport(
                    context = context,
                    summaries = reportFeeSummaries,
                    schoolInfo = schoolInfo,
                    reportTitle = reportTitle
                )
            },
            onDismiss = { pendingFeePdfFile = null }
        )
    }
}
