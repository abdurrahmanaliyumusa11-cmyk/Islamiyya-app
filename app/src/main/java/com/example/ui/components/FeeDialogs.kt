package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassFeeSchedule
import com.example.data.model.FeePayment
import com.example.data.model.SchoolClass
import com.example.data.model.StudentFeeInstallment
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.StatusActiveContainer
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusCompletedBlue
import com.example.ui.theme.StatusCompletedContainer
import com.example.ui.theme.StatusSuspendedContainer
import com.example.ui.theme.StatusSuspendedRed
import com.example.ui.theme.StatusTransferredContainer
import com.example.ui.theme.StatusTransferredOrange
import com.example.util.FeeHelper
import com.example.util.StudentFeeStatus
import com.example.util.StudentFeeSummary

@Composable
fun FeeStatusBadge(
    status: StudentFeeStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        StudentFeeStatus.PAID -> StatusActiveContainer to StatusActiveGreen
        StudentFeeStatus.OVERDUE -> StatusSuspendedContainer to StatusSuspendedRed
        StudentFeeStatus.DUE_SOON, StudentFeeStatus.PARTIAL -> StatusTransferredContainer to Color(0xFFB35300)
        StudentFeeStatus.PENDING -> StatusCompletedContainer to StatusCompletedBlue
        StudentFeeStatus.UNBILLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFeePaymentDialog(
    studentName: String,
    summary: StudentFeeSummary,
    onDismiss: () -> Unit,
    onConfirm: (amountPaid: Long, installmentId: Long?, paymentDate: String, paymentMethod: String, remarks: String) -> Unit
) {
    val openInstallments = remember(summary) {
        summary.installmentBreakdown.filter { !it.isCleared }
    }
    val suggestedAmount = remember(summary, openInstallments) {
        openInstallments.firstOrNull()?.remaining?.takeIf { it > 0L }
            ?: summary.balance.takeIf { it > 0L }
            ?: 5000L
    }

    var amountText by remember { mutableStateOf(suggestedAmount.toString()) }
    var selectedInstallmentId by remember { mutableStateOf<Long?>(openInstallments.firstOrNull()?.installment?.id) }
    var installmentMenuExpanded by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentDate by remember { mutableStateOf(FeeHelper.todayDateString()) }
    var remarks by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedInstallmentLabel = remember(selectedInstallmentId, openInstallments) {
        openInstallments.firstOrNull { it.installment.id == selectedInstallmentId }?.let {
            "${it.installment.installmentTitle} (${FeeHelper.formatNaira(it.remaining)} left)"
        } ?: "Auto-Allocate to Oldest Balance"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Record Tuition Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "$studentName • Balance: ${FeeHelper.formatNaira(summary.balance)}",
                    fontSize = 12.sp,
                    color = IslamicGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }
                        errorMsg = null
                    },
                    label = { Text("Amount Paid (₦ NGN)") },
                    prefix = { Text("₦ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fee_payment_amount_input")
                )

                if (openInstallments.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = installmentMenuExpanded,
                        onExpandedChange = { installmentMenuExpanded = !installmentMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedInstallmentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Apply To Installment") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = installmentMenuExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = installmentMenuExpanded,
                            onDismissRequest = { installmentMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Auto-Allocate (FIFO Balance)") },
                                onClick = {
                                    selectedInstallmentId = null
                                    installmentMenuExpanded = false
                                }
                            )
                            openInstallments.forEach { alloc ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${alloc.installment.installmentTitle} — Due ${alloc.installment.dueDate} (${FeeHelper.formatNaira(alloc.remaining)})")
                                    },
                                    onClick = {
                                        selectedInstallmentId = alloc.installment.id
                                        amountText = alloc.remaining.toString()
                                        installmentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Payment Method",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "Bank Transfer", "POS").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IslamicGreenPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("fee_payment_method_${method.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it; errorMsg = null },
                    label = { Text("Payment Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fee_payment_date_input")
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Depositor Note (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val dateValid = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(paymentDate.trim())
                    when {
                        amount <= 0L -> errorMsg = "Enter a valid Naira amount greater than ₦0."
                        !dateValid -> errorMsg = "Enter date in yyyy-MM-dd format (e.g. 2026-10-15)."
                        else -> onConfirm(amount, selectedInstallmentId, paymentDate.trim(), paymentMethod, remarks.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_confirm_fee_payment")
            ) {
                Text("Record Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManageInstallmentDialog(
    studentName: String,
    termLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amountDue: Long, dueDate: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(FeeHelper.todayDateString()) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Fee Installment", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "$studentName • $termLabel",
                    fontSize = 12.sp,
                    color = IslamicGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Installment Title") },
                    placeholder = { Text("e.g. 3rd Installment or Exam Levy") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("installment_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Amount Due (₦ NGN)") },
                    prefix = { Text("₦ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("installment_amount_input")
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it; errorMsg = null },
                    label = { Text("Due Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("installment_due_date_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val dateValid = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dueDate.trim())
                    when {
                        title.isBlank() -> errorMsg = "Enter an installment title."
                        amount <= 0L -> errorMsg = "Enter a Naira amount greater than ₦0."
                        !dateValid -> errorMsg = "Enter due date in yyyy-MM-dd format."
                        else -> onConfirm(title.trim(), amount, dueDate.trim(), notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_save_installment")
            ) {
                Text("Save Installment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClassFeeSetupDialog(
    classes: List<SchoolClass>,
    schedules: List<ClassFeeSchedule>,
    termLabel: String,
    onDismiss: () -> Unit,
    onSave: (
        className: String,
        tuitionAmount: Long,
        installmentCount: Int,
        firstDueDate: String,
        secondDueDate: String,
        thirdDueDate: String,
        applyToStudents: Boolean
    ) -> Unit
) {
    val classNames = remember(classes, schedules) {
        (classes.map { it.name } + schedules.map { it.className })
            .distinct()
            .ifEmpty { listOf("Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "Primary 6") }
    }

    var selectedClass by remember { mutableStateOf(classNames.first()) }
    val existing = remember(selectedClass, schedules) {
        schedules.firstOrNull { it.className == selectedClass && it.termLabel == termLabel }
            ?: schedules.firstOrNull { it.className == selectedClass }
    }

    var tuitionText by remember(selectedClass, existing) {
        mutableStateOf((existing?.tuitionAmount ?: 20000L).toString())
    }
    var installmentCount by remember(selectedClass, existing) {
        mutableIntStateOf((existing?.defaultInstallmentCount ?: 2).coerceIn(1, 3))
    }
    var firstDueDate by remember(selectedClass, existing) {
        mutableStateOf(existing?.firstDueDate ?: "2026-09-25")
    }
    var secondDueDate by remember(selectedClass, existing) {
        mutableStateOf(existing?.secondDueDate ?: "2026-10-25")
    }
    var thirdDueDate by remember(selectedClass, existing) {
        mutableStateOf(existing?.thirdDueDate ?: "2026-12-05")
    }
    var applyToStudents by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Configure Class Term Fees", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "Term: $termLabel • Currency: Nigerian Naira (₦)",
                    fontSize = 12.sp,
                    color = IslamicGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Select Class",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    classNames.forEach { cls ->
                        FilterChip(
                            selected = selectedClass == cls,
                            onClick = { selectedClass = cls; errorMsg = null },
                            label = { Text(cls, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IslamicGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("class_fee_chip_${cls.replace(" ", "_").lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = tuitionText,
                    onValueChange = { tuitionText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Term Tuition Amount (₦)") },
                    prefix = { Text("₦ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_fee_tuition_input")
                )

                Text(
                    text = "Installment Plan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 Payment", 2 to "2 Installments", 3 to "3 Installments").forEach { (cnt, label) ->
                        FilterChip(
                            selected = installmentCount == cnt,
                            onClick = { installmentCount = cnt },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IslamicGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = firstDueDate,
                    onValueChange = { firstDueDate = it; errorMsg = null },
                    label = { Text("1st Installment Due Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (installmentCount >= 2) {
                    OutlinedTextField(
                        value = secondDueDate,
                        onValueChange = { secondDueDate = it; errorMsg = null },
                        label = { Text("2nd Installment Due Date (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (installmentCount >= 3) {
                    OutlinedTextField(
                        value = thirdDueDate,
                        onValueChange = { thirdDueDate = it; errorMsg = null },
                        label = { Text("3rd Installment Due Date (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply to enrolled students",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Updates installment targets for all $selectedClass pupils",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = applyToStudents,
                        onCheckedChange = { applyToStudents = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IslamicGreenPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = tuitionText.toLongOrNull() ?: 0L
                    val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                    val datesValid = dateRegex.matches(firstDueDate.trim()) &&
                        (installmentCount < 2 || dateRegex.matches(secondDueDate.trim())) &&
                        (installmentCount < 3 || dateRegex.matches(thirdDueDate.trim()))
                    when {
                        amount <= 0L -> errorMsg = "Enter a tuition fee greater than ₦0."
                        !datesValid -> errorMsg = "Check all due dates use yyyy-MM-dd format."
                        else -> onSave(
                            selectedClass,
                            amount,
                            installmentCount,
                            firstDueDate.trim(),
                            secondDueDate.trim(),
                            thirdDueDate.trim(),
                            applyToStudents
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_save_class_fee_schedule")
            ) {
                Text("Save Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StudentTuitionCard(
    summary: StudentFeeSummary,
    canManageFees: Boolean,
    canSendSms: Boolean,
    onRecordPaymentClick: () -> Unit,
    onAddInstallmentClick: () -> Unit,
    onSendFeeSmsClick: () -> Unit,
    onPrintReceipt: (FeePayment) -> Unit,
    onDownloadPdfReceipt: ((FeePayment) -> Unit)? = null,
    onDeleteInstallment: (StudentFeeInstallment) -> Unit,
    onDeletePayment: (FeePayment) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (summary.totalDue > 0L) {
        (summary.totalPaid.toFloat() / summary.totalDue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_tuition_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tuition & Fee Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary
                    )
                    Text(
                        text = summary.termLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FeeStatusBadge(status = summary.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // KPI Figures Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FeeKpiItem("Term Tuition", FeeHelper.formatNaira(summary.totalDue), MaterialTheme.colorScheme.onSurface)
                FeeKpiItem("Paid", FeeHelper.formatNaira(summary.totalPaid), StatusActiveGreen)
                FeeKpiItem(
                    "Balance",
                    FeeHelper.formatNaira(summary.balance),
                    if (summary.balance > 0L) StatusSuspendedRed else StatusActiveGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (summary.status == StudentFeeStatus.OVERDUE) StatusSuspendedRed else IslamicGreenPrimary,
                trackColor = IslamicGreenPrimary.copy(alpha = 0.12f)
            )

            if (summary.balance > 0L && summary.nextDueDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (summary.overdueCount > 0) StatusSuspendedContainer.copy(alpha = 0.7f) else IslamicGoldAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (summary.overdueCount > 0) StatusSuspendedRed else Color(0xFF8C6D2E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (summary.overdueCount > 0) {
                                "Overdue: ${FeeHelper.formatNaira(summary.overdueAmount)} (${summary.overdueCount} installment)"
                            } else {
                                "Next Due: ${FeeHelper.formatNaira(summary.nextDueAmount)} on ${summary.nextDueDate}"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.overdueCount > 0) StatusSuspendedRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Installments Breakdown
            if (summary.installmentBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Installment Schedule (${summary.installmentBreakdown.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                summary.installmentBreakdown.forEach { alloc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alloc.installment.installmentTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Due: ${alloc.installment.dueDate} • Paid ${FeeHelper.formatNaira(alloc.paidTowardInstallment)} / ${FeeHelper.formatNaira(alloc.installment.amountDue)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val tagColor = when {
                                alloc.isCleared -> StatusActiveGreen
                                alloc.isOverdue -> StatusSuspendedRed
                                alloc.isDueSoon -> StatusTransferredOrange
                                else -> IslamicGreenPrimary
                            }
                            val tagLabel = when {
                                alloc.isCleared -> "CLEARED"
                                alloc.isOverdue -> "OVERDUE"
                                alloc.isDueSoon -> "DUE SOON"
                                else -> "UPCOMING"
                            }
                            Text(
                                text = tagLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                            if (canManageFees) {
                                IconButton(
                                    onClick = { onDeleteInstallment(alloc.installment) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Installment",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }

            // Recorded Payments & Printable Receipts
            if (summary.payments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Payment History & Receipts (${summary.payments.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                summary.payments.forEach { payment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${FeeHelper.formatNaira(payment.amountPaid)} • ${payment.paymentMethod}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusActiveGreen
                            )
                            Text(
                                text = "${payment.receiptNumber} • ${payment.paymentDate}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canManageFees && onDownloadPdfReceipt != null) {
                                IconButton(
                                    onClick = { onDownloadPdfReceipt(payment) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("btn_download_pdf_receipt_${payment.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Generate & Download Official PDF Receipt ${payment.receiptNumber}",
                                        tint = IslamicGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onPrintReceipt(payment) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_print_receipt_${payment.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print Receipt ${payment.receiptNumber}",
                                    tint = IslamicGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (canManageFees) {
                                IconButton(
                                    onClick = { onDeletePayment(payment) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Payment",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }

            // Action Buttons
            if (canManageFees || (canSendSms && summary.balance > 0L)) {
                Spacer(modifier = Modifier.height(12.dp))
                if (canManageFees) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRecordPaymentClick,
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("btn_profile_record_payment")
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Pay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onAddInstallmentClick,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_profile_add_installment")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Installment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                        }
                    }
                }
                if (canSendSms && summary.balance > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSendFeeSmsClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_profile_send_fee_sms")
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Fee Reminder SMS to Parent", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeKpiItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}
