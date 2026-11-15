package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeePayment
import com.example.data.model.Student
import com.example.ui.SchoolViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ManageInstallmentDialog
import com.example.ui.components.PdfReceiptExportDialog
import com.example.ui.components.PerformanceTrendsCard
import com.example.ui.components.RecordFeePaymentDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudentAvatar
import com.example.ui.components.StudentIdCardDialog
import com.example.ui.components.StudentTuitionCard
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.FeeHelper
import com.example.util.OfficialPdfExporter
import com.example.util.ReportExporter
import com.example.util.SmsEventTemplate
import com.example.util.SmsHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    student: Student,
    viewModel: SchoolViewModel,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val studentGrades by viewModel.getGradesForStudent(student.id).collectAsState(initial = emptyList())
    val studentInstallments by viewModel.getInstallmentsForStudent(student.id).collectAsState(initial = emptyList())
    val studentPayments by viewModel.getPaymentsForStudent(student.id).collectAsState(initial = emptyList())
    val activeFeeTerm by viewModel.activeFeeTerm.collectAsState()
    val feeReminderLeadDays by viewModel.feeReminderLeadDays.collectAsState()

    val feeSummary = remember(student, studentInstallments, studentPayments, activeFeeTerm, feeReminderLeadDays) {
        FeeHelper.computeStudentFeeSummary(
            student = student,
            installments = studentInstallments,
            payments = studentPayments,
            termLabel = activeFeeTerm,
            dueSoonWindowDays = feeReminderLeadDays
        )
    }

    val isAdmin = isAuthenticated && currentRole.canAddOrEditStudents
    val canViewSensitive = isAuthenticated && currentRole.canViewSensitiveStudentDetails
    val canManageFees = isAuthenticated && currentRole.canManageFeeRecords
    val canRecordGrades = isAuthenticated && currentRole.canRecordGrades
    val canSendSms = isAuthenticated && currentRole.canSendSms

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showIdCardDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var showAddInstallmentDialog by remember { mutableStateOf(false) }
    var activePdfReceipt by remember { mutableStateOf<Pair<File, FeePayment>?>(null) }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.testTag("student_profile_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Student Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showIdCardDialog = true },
                        modifier = Modifier.testTag("profile_top_id_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Digital Student ID Card",
                            tint = IslamicGoldAccent
                        )
                    }
                    IconButton(
                        onClick = {
                            ReportExporter.printStudentProfile(context, student, schoolInfo)
                        },
                        modifier = Modifier.testTag("profile_top_print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Profile",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IslamicGreenPrimary
                )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // [EDIT]
                    Button(
                        onClick = {
                            if (isAdmin) {
                                viewModel.startEditStudent(student)
                            } else if (!isAuthenticated) {
                                onRequireLogin()
                            } else {
                                viewModel.showAccessDeniedMessage("Admin")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_edit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // [PRINT]
                    OutlinedButton(
                        onClick = {
                            ReportExporter.printStudentProfile(context, student, schoolInfo)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_print_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PRINT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                    }

                    // [EXPORT]
                    OutlinedButton(
                        onClick = {
                            if (isAdmin) {
                                val file = ReportExporter.exportStudentsToCsv(
                                    context = context,
                                    students = listOf(student),
                                    schoolInfo = schoolInfo,
                                    reportTitle = "Student_${student.registrationNumber.replace('/', '_')}"
                                )
                                if (file != null) {
                                    ReportExporter.shareCsvFile(context, file, "Record: ${student.fullName}")
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
                            .testTag("profile_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                    }

                    // [DELETE]
                    Button(
                        onClick = {
                            if (isAdmin) {
                                showDeleteConfirm = true
                            } else if (!isAuthenticated) {
                                onRequireLogin()
                            } else {
                                viewModel.showAccessDeniedMessage("Admin")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("profile_delete_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(IslamicGreenPrimary, IslamicGreenDark)
                        )
                    )
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StudentAvatar(
                        photoUri = student.photoUri,
                        fullName = student.fullName,
                        gender = student.gender,
                        size = 96.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = student.fullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = IslamicGoldAccent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = student.registrationNumber,
                                color = Color(0xFF281E04),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        StatusBadge(status = student.admissionStatus)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showIdCardDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("profile_digital_id_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = IslamicGoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Digital Student ID Card (QR)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Information Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Academic Information
                InfoCard(title = "Academic Information") {
                    ProfileFieldRow(label = "Class Enrolled", value = student.className)
                    ProfileFieldRow(label = "Date of Admission", value = student.dateOfAdmission)
                    ProfileFieldRow(label = "Registration Number", value = student.registrationNumber)
                    ProfileFieldRow(
                        label = "Previous School",
                        value = if (student.previousSchool.isNotBlank()) student.previousSchool else "None / Direct Entry"
                    )
                }

                // Performance Trends Bar Chart View
                PerformanceTrendsCard(
                    studentId = student.id,
                    grades = studentGrades,
                    canRecordGrades = canRecordGrades,
                    onAddGrade = { grade ->
                        viewModel.addStudentGrade(grade)
                    }
                )

                // Personal Details
                InfoCard(title = "Personal Information") {
                    ProfileFieldRow(label = "Full Name", value = student.fullName)
                    ProfileFieldRow(label = "Gender", value = student.gender)
                    ProfileFieldRow(label = "Age", value = "${student.age} Years")
                    ProfileFieldRow(label = "Date of Birth", value = student.dateOfBirth)
                    ProfileFieldRow(label = "Admission Status", value = student.admissionStatus)
                }

                // Parent / Guardian & Sensitive Records (Role-Protected)
                if (canViewSensitive) {
                    InfoCard(title = "Parent / Guardian Information") {
                        ProfileFieldRow(
                            label = "Parent / Guardian",
                            value = if (student.parentGuardianName.isNotBlank()) student.parentGuardianName else "Not specified"
                        )

                        if (student.parentGuardianPhone.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Linked Parent Phone",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = student.parentGuardianPhone,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${student.parentGuardianPhone}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(IslamicGreenPrimary.copy(alpha = 0.12f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call Parent",
                                            tint = IslamicGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (canSendSms) {
                                                viewModel.openSmsHub(
                                                    preselectedIds = setOf(student.id),
                                                    template = SmsEventTemplate.ATTENDANCE_ALERT
                                                )
                                            } else {
                                                val ok = SmsHelper.launchSmsComposer(
                                                    context,
                                                    listOf(student.parentGuardianPhone),
                                                    ""
                                                )
                                                if (!ok) {
                                                    viewModel.showMessage("No SMS app installed on this device.")
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(IslamicGreenPrimary.copy(alpha = 0.12f))
                                            .testTag("profile_quick_sms_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = "SMS Parent",
                                            tint = IslamicGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No parent phone number linked",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isAdmin) {
                                    OutlinedButton(onClick = { viewModel.startEditStudent(student) }) {
                                        Text("Link Phone", fontSize = 11.sp, color = IslamicGreenPrimary)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }

                        ProfileFieldRow(
                            label = "Home Residential Address",
                            value = if (student.homeAddress.isNotBlank()) student.homeAddress else "Not specified"
                        )

                        if (canSendSms && student.parentGuardianPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.openSmsHub(
                                        preselectedIds = setOf(student.id),
                                        template = SmsEventTemplate.REPORT_CARD
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_compose_sms_button")
                            ) {
                                Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compose SMS to Parent / Guardian", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Student Tuition & Fee Account (Admin edits, Class Teacher views read-only)
                    StudentTuitionCard(
                        summary = feeSummary,
                        canManageFees = canManageFees,
                        canSendSms = canSendSms,
                        onRecordPaymentClick = { showRecordPaymentDialog = true },
                        onAddInstallmentClick = { showAddInstallmentDialog = true },
                        onSendFeeSmsClick = { viewModel.openFeeSmsReminder(listOf(feeSummary)) },
                        onPrintReceipt = { payment ->
                            ReportExporter.printFeeReceipt(context, student, payment, feeSummary, schoolInfo)
                        },
                        onDownloadPdfReceipt = if (canManageFees) {
                            { payment ->
                                val pdf = OfficialPdfExporter.generateFeeReceiptPdf(
                                    context = context,
                                    student = student,
                                    payment = payment,
                                    summary = feeSummary,
                                    schoolInfo = schoolInfo
                                )
                                if (pdf != null) {
                                    activePdfReceipt = pdf to payment
                                } else {
                                    viewModel.showMessage("Could not generate official PDF receipt.")
                                }
                            }
                        } else null,
                        onDeleteInstallment = { inst ->
                            viewModel.deleteInstallment(inst)
                        },
                        onDeletePayment = { pay ->
                            viewModel.deleteFeePayment(pay)
                        }
                    )

                    // Additional Notes
                    if (student.additionalNotes.isNotBlank()) {
                        InfoCard(title = "Administrative Remarks & Notes") {
                            Text(
                                text = student.additionalNotes,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    // Sensitive records restricted view for Subject Teacher / Locked session
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sensitive_records_locked_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Protected Record",
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sensitive Guardian & Address Records Protected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (!isAuthenticated) {
                                    "Unlock as Class Teacher or Admin to view guardian phone, home address, and remarks."
                                } else {
                                    "Current role (${currentRole.displayName}) is restricted from sensitive biodata. Switch to Class Teacher or Admin."
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onRequireLogin) {
                                Text("Switch Role / Unlock", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete Student Record",
            message = "Are you sure you want to permanently delete the student record of '${student.fullName}' (${student.registrationNumber})? This action cannot be undone.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteStudent(student) {
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showIdCardDialog) {
        StudentIdCardDialog(
            student = student,
            schoolInfo = schoolInfo,
            onDismiss = { showIdCardDialog = false },
            onNotify = { msg -> viewModel.showMessage(msg) }
        )
    }

    if (showRecordPaymentDialog) {
        RecordFeePaymentDialog(
            studentName = student.fullName,
            summary = feeSummary,
            onDismiss = { showRecordPaymentDialog = false },
            onConfirm = { amount, instId, date, method, remarks ->
                viewModel.recordFeePayment(
                    studentId = student.id,
                    installmentId = instId,
                    termLabel = activeFeeTerm,
                    amountPaid = amount,
                    paymentDate = date,
                    paymentMethod = method,
                    remarks = remarks
                ) {
                    showRecordPaymentDialog = false
                }
            }
        )
    }

    if (showAddInstallmentDialog) {
        ManageInstallmentDialog(
            studentName = student.fullName,
            termLabel = activeFeeTerm,
            onDismiss = { showAddInstallmentDialog = false },
            onConfirm = { title, amountDue, dueDate, notes ->
                viewModel.addStudentInstallment(
                    studentId = student.id,
                    termLabel = activeFeeTerm,
                    title = title,
                    amountDue = amountDue,
                    dueDate = dueDate,
                    notes = notes
                ) {
                    showAddInstallmentDialog = false
                }
            }
        )
    }

    activePdfReceipt?.let { (pdfFile, payment) ->
        PdfReceiptExportDialog(
            pdfFile = pdfFile,
            title = "Official Fee Receipt PDF",
            subtitle = "Receipt ${payment.receiptNumber} • ${student.fullName}",
            amountLine = "${FeeHelper.formatNaira(payment.amountPaid)} • Paid ${payment.paymentDate} (${payment.paymentMethod})",
            onNotify = { msg -> viewModel.showMessage(msg) },
            onPrintFallback = {
                ReportExporter.printFeeReceipt(context, student, payment, feeSummary, schoolInfo)
            },
            onDismiss = { activePdfReceipt = null }
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = IslamicGreenPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ProfileFieldRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}
