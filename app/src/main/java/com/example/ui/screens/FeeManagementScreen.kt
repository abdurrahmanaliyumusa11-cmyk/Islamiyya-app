package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeePayment
import com.example.ui.SchoolViewModel
import com.example.ui.components.ClassFeeSetupDialog
import com.example.ui.components.FeeStatusBadge
import com.example.ui.components.ManageInstallmentDialog
import com.example.ui.components.PdfReceiptExportDialog
import com.example.ui.components.RecordFeePaymentDialog
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusSuspendedContainer
import com.example.ui.theme.StatusSuspendedRed
import com.example.util.FeeHelper
import com.example.util.OfficialPdfExporter
import com.example.util.ReportExporter
import com.example.util.StudentFeeStatus
import com.example.util.StudentFeeSummary
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeManagementScreen(
    viewModel: SchoolViewModel,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val activeTerm by viewModel.activeFeeTerm.collectAsState()
    val summaries by viewModel.studentFeeSummaries.collectAsState()
    val dueAndOverdue by viewModel.dueAndOverdueFeeSummaries.collectAsState()
    val classes by viewModel.allClasses.collectAsState()
    val schedules by viewModel.allClassFeeSchedules.collectAsState()

    val canViewFees = isAuthenticated && currentRole.canViewFeeRecords
    val canManageFees = isAuthenticated && currentRole.canManageFeeRecords
    val canSendSms = isAuthenticated && currentRole.canSendSms

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<StudentFeeStatus?>(null) }

    var paymentDialogSummary by remember { mutableStateOf<StudentFeeSummary?>(null) }
    var installmentDialogSummary by remember { mutableStateOf<StudentFeeSummary?>(null) }
    var showClassFeeSetup by remember { mutableStateOf(false) }
    var activePdfReceipt by remember { mutableStateOf<Triple<File, StudentFeeSummary, FeePayment>?>(null) }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.testTag("fee_management_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tuition & Fee Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$activeTerm • Naira (₦) Ledger",
                            fontSize = 11.sp,
                            color = IslamicGoldAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("fee_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (canManageFees) {
                        TextButton(
                            onClick = { showClassFeeSetup = true },
                            modifier = Modifier.testTag("btn_class_fee_setup")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = IslamicGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Class Fees",
                                color = IslamicGoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicGreenPrimary)
            )
        }
    ) { paddingValues ->
        if (!canViewFees) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted Tuition Records",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tuition & Financial Records Protected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (!isAuthenticated) {
                                "Sign in as Admin (full edit) or Class Teacher (read-only) to access tuition payments, due dates, and parent fee reminders."
                            } else {
                                "Your role (${currentRole.displayName}) does not have permission to view tuition financial records. Switch to Admin or Class Teacher."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_fee_unlock_login")
                        ) {
                            Text("Sign In / Switch Role", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            val totalDueAll = remember(summaries) { summaries.sumOf { it.totalDue } }
            val totalPaidAll = remember(summaries) { summaries.sumOf { it.totalPaid } }
            val totalBalanceAll = remember(summaries) { summaries.sumOf { it.balance } }
            val paidStudentCount = remember(summaries) { summaries.count { it.status == StudentFeeStatus.PAID } }
            val overdueCount = remember(dueAndOverdue) { dueAndOverdue.count { it.status == StudentFeeStatus.OVERDUE } }
            val dueSoonCount = remember(dueAndOverdue) { dueAndOverdue.count { it.status == StudentFeeStatus.DUE_SOON } }

            val filteredSummaries = remember(summaries, searchQuery, selectedClassFilter, selectedStatusFilter) {
                var list = summaries
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().lowercase(Locale.ROOT)
                    list = list.filter {
                        it.student.fullName.lowercase(Locale.ROOT).contains(q) ||
                            it.student.registrationNumber.lowercase(Locale.ROOT).contains(q) ||
                            it.student.parentGuardianName.lowercase(Locale.ROOT).contains(q)
                    }
                }
                if (!selectedClassFilter.isNullOrBlank()) {
                    list = list.filter { it.student.className == selectedClassFilter }
                }
                if (selectedStatusFilter != null) {
                    list = list.filter { it.status == selectedStatusFilter }
                }
                list.sortedWith(
                    compareBy<StudentFeeSummary> {
                        when (it.status) {
                            StudentFeeStatus.OVERDUE -> 0
                            StudentFeeStatus.DUE_SOON -> 1
                            StudentFeeStatus.PARTIAL -> 2
                            StudentFeeStatus.PENDING -> 3
                            StudentFeeStatus.UNBILLED -> 4
                            StudentFeeStatus.PAID -> 5
                        }
                    }.thenBy { it.student.fullName }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Financial Overview Hero Card
                item {
                    val collectionRatio = if (totalDueAll > 0L) (totalPaidAll.toFloat() / totalDueAll.toFloat()).coerceIn(0f, 1f) else 0f
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IslamicGreenDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$activeTerm Overview",
                                    color = IslamicGoldAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (canManageFees) "Admin Edit Access" else "Class Teacher View Only",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Collected", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                    Text(
                                        text = FeeHelper.formatNaira(totalPaidAll),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF7BE8B7)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Outstanding", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                    Text(
                                        text = FeeHelper.formatNaira(totalBalanceAll),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IslamicGoldAccent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { collectionRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = IslamicGoldAccent,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Expected: ${FeeHelper.formatNaira(totalDueAll)}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "$paidStudentCount/${summaries.size} Fully Paid",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Due & Overdue Automated Reminder Banner
                if (dueAndOverdue.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (overdueCount > 0) StatusSuspendedContainer.copy(alpha = 0.85f) else IslamicGoldAccent.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fee_due_reminder_banner")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (overdueCount > 0) StatusSuspendedRed else Color(0xFF8C6D2E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Payment Reminders Required (${dueAndOverdue.size} Accounts)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (overdueCount > 0) StatusSuspendedRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$overdueCount overdue • $dueSoonCount due soon",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.triggerFeeReminderCheckNow() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_fee_trigger_alert")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = IslamicGreenPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Notify Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
                                    }
                                    if (canSendSms) {
                                        Button(
                                            onClick = { viewModel.openFeeSmsReminder(dueAndOverdue) },
                                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(48.dp)
                                                .testTag("btn_fee_remind_parents_sms")
                                        ) {
                                            Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remind Parents SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search & Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search student, reg no, or guardian...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = IslamicGreenPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fee_search_input")
                        )

                        // Class Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedClassFilter == null,
                                onClick = { selectedClassFilter = null },
                                label = { Text("All Classes", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IslamicGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            classes.forEach { cls ->
                                FilterChip(
                                    selected = selectedClassFilter == cls.name,
                                    onClick = {
                                        selectedClassFilter = if (selectedClassFilter == cls.name) null else cls.name
                                    },
                                    label = { Text(cls.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IslamicGreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Status Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedStatusFilter == null,
                                onClick = { selectedStatusFilter = null },
                                label = { Text("All (${summaries.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IslamicGoldAccent,
                                    selectedLabelColor = Color(0xFF281E04)
                                )
                            )
                            listOf(
                                StudentFeeStatus.OVERDUE,
                                StudentFeeStatus.DUE_SOON,
                                StudentFeeStatus.PARTIAL,
                                StudentFeeStatus.PENDING,
                                StudentFeeStatus.PAID
                            ).forEach { st ->
                                val count = summaries.count { it.status == st }
                                FilterChip(
                                    selected = selectedStatusFilter == st,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == st) null else st
                                    },
                                    label = { Text("${st.label} ($count)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IslamicGreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Student Fee Cards
                if (filteredSummaries.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No student fee records match your filters.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSummaries, key = { it.student.id }) { summary ->
                        StudentFeeLedgerRowCard(
                            summary = summary,
                            canManageFees = canManageFees,
                            canSendSms = canSendSms,
                            onRecordPaymentClick = { paymentDialogSummary = summary },
                            onAddInstallmentClick = { installmentDialogSummary = summary },
                            onPrintLatestReceipt = {
                                val latest = summary.payments.firstOrNull()
                                if (latest != null) {
                                    ReportExporter.printFeeReceipt(context, summary.student, latest, summary, schoolInfo)
                                }
                            },
                            onDownloadLatestPdfReceipt = {
                                val latest = summary.payments.firstOrNull()
                                if (latest != null) {
                                    val pdf = OfficialPdfExporter.generateFeeReceiptPdf(
                                        context = context,
                                        student = summary.student,
                                        payment = latest,
                                        summary = summary,
                                        schoolInfo = schoolInfo
                                    )
                                    if (pdf != null) {
                                        activePdfReceipt = Triple(pdf, summary, latest)
                                    } else {
                                        viewModel.showMessage("Could not generate official PDF receipt.")
                                    }
                                }
                            },
                            onSendSmsClick = { viewModel.openFeeSmsReminder(listOf(summary)) },
                            onOpenProfileClick = { viewModel.showStudentProfile(summary.student) }
                        )
                    }
                }
            }
        }
    }

    paymentDialogSummary?.let { target ->
        RecordFeePaymentDialog(
            studentName = target.student.fullName,
            summary = target,
            onDismiss = { paymentDialogSummary = null },
            onConfirm = { amount, instId, date, method, remarks ->
                viewModel.recordFeePayment(
                    studentId = target.student.id,
                    installmentId = instId,
                    termLabel = activeTerm,
                    amountPaid = amount,
                    paymentDate = date,
                    paymentMethod = method,
                    remarks = remarks
                ) {
                    paymentDialogSummary = null
                }
            }
        )
    }

    installmentDialogSummary?.let { target ->
        ManageInstallmentDialog(
            studentName = target.student.fullName,
            termLabel = activeTerm,
            onDismiss = { installmentDialogSummary = null },
            onConfirm = { title, amountDue, dueDate, notes ->
                viewModel.addStudentInstallment(
                    studentId = target.student.id,
                    termLabel = activeTerm,
                    title = title,
                    amountDue = amountDue,
                    dueDate = dueDate,
                    notes = notes
                ) {
                    installmentDialogSummary = null
                }
            }
        )
    }

    if (showClassFeeSetup) {
        ClassFeeSetupDialog(
            classes = classes,
            schedules = schedules,
            termLabel = activeTerm,
            onDismiss = { showClassFeeSetup = false },
            onSave = { className, tuition, count, d1, d2, d3, applyToStudents ->
                viewModel.updateClassFeeSchedule(
                    className = className,
                    termLabel = activeTerm,
                    tuitionAmount = tuition,
                    installmentCount = count,
                    firstDueDate = d1,
                    secondDueDate = d2,
                    thirdDueDate = d3,
                    applyToClassStudents = applyToStudents
                ) {
                    showClassFeeSetup = false
                }
            }
        )
    }

    activePdfReceipt?.let { (pdfFile, summary, payment) ->
        PdfReceiptExportDialog(
            pdfFile = pdfFile,
            title = "Official Fee Receipt PDF",
            subtitle = "Receipt ${payment.receiptNumber} • ${summary.student.fullName}",
            amountLine = "${FeeHelper.formatNaira(payment.amountPaid)} • Paid ${payment.paymentDate} (${payment.paymentMethod})",
            onNotify = { msg -> viewModel.showMessage(msg) },
            onPrintFallback = {
                ReportExporter.printFeeReceipt(context, summary.student, payment, summary, schoolInfo)
            },
            onDismiss = { activePdfReceipt = null }
        )
    }
}

@Composable
private fun StudentFeeLedgerRowCard(
    summary: StudentFeeSummary,
    canManageFees: Boolean,
    canSendSms: Boolean,
    onRecordPaymentClick: () -> Unit,
    onAddInstallmentClick: () -> Unit,
    onPrintLatestReceipt: () -> Unit,
    onDownloadLatestPdfReceipt: () -> Unit,
    onSendSmsClick: () -> Unit,
    onOpenProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProfileClick() }
            .testTag("fee_student_card_${summary.student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.student.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${summary.student.registrationNumber} • ${summary.student.className}",
                        fontSize = 11.sp,
                        color = IslamicGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FeeStatusBadge(status = summary.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tuition", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FeeHelper.formatNaira(summary.totalDue), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FeeHelper.formatNaira(summary.totalPaid), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusActiveGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = FeeHelper.formatNaira(summary.balance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.balance > 0L) StatusSuspendedRed else StatusActiveGreen
                    )
                }
            }

            if (summary.nextDueDate != null && summary.balance > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (summary.status == StudentFeeStatus.OVERDUE) {
                        "Overdue installment (${FeeHelper.formatNaira(summary.overdueAmount)}) • Due since ${summary.nextDueDate}"
                    } else {
                        "Next installment due ${summary.nextDueDate}: ${FeeHelper.formatNaira(summary.nextDueAmount)}"
                    },
                    fontSize = 11.sp,
                    color = if (summary.status == StudentFeeStatus.OVERDUE) StatusSuspendedRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (summary.status == StudentFeeStatus.OVERDUE) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            if (canManageFees) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRecordPaymentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_record_fee_${summary.student.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onAddInstallmentClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_add_inst_fee_${summary.student.id}")
                    ) {
                        Text("+ Installment", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (summary.payments.isNotEmpty() || (canSendSms && summary.balance > 0L)) {
                if (canManageFees) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (summary.payments.isNotEmpty()) {
                        if (canManageFees) {
                            OutlinedButton(
                                onClick = onDownloadLatestPdfReceipt,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_pdf_fee_${summary.student.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Official PDF Receipt",
                                    modifier = Modifier.size(15.dp),
                                    tint = IslamicGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Receipt", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onPrintLatestReceipt,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_receipt_fee_${summary.student.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = "Print Receipt", modifier = Modifier.size(15.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (canSendSms && summary.balance > 0L) {
                        OutlinedButton(
                            onClick = onSendSmsClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_sms_fee_${summary.student.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = "Send Fee SMS", modifier = Modifier.size(15.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!canManageFees && summary.payments.isEmpty() && (!canSendSms || summary.balance <= 0L)) {
                OutlinedButton(
                    onClick = onOpenProfileClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_open_fee_profile_${summary.student.id}")
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = IslamicGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Ledger Details", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
