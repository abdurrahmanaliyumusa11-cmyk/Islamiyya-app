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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.SmsLog
import com.example.data.model.Student
import com.example.ui.SchoolViewModel
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.SmsEventTemplate
import com.example.util.SmsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SmsAudienceScope(val label: String) {
    INDIVIDUAL("Individual / Select"),
    BY_CLASS("By Class"),
    ALL_SCHOOL("All School"),
    ABSENT_TODAY("Absent / Late Today"),
    OVERDUE_FEES("Overdue / Due Fees")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsNotificationScreen(
    viewModel: SchoolViewModel,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val allClasses by viewModel.allClasses.collectAsState()
    val schoolInfo by viewModel.schoolInfo.collectAsState()
    val smsLogs by viewModel.allSmsLogs.collectAsState()
    val preselectedIds by viewModel.preselectedSmsStudentIds.collectAsState()
    val preselectedTemplate by viewModel.preselectedSmsTemplate.collectAsState()
    val preselectedCustomBody by viewModel.preselectedSmsCustomBody.collectAsState()
    val dueAndOverdueFees by viewModel.dueAndOverdueFeeSummaries.collectAsState()
    val todayAttendance by viewModel.attendanceForSelectedDate.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()

    val canSendSms = isAuthenticated && currentRole.canSendSms
    val canEditStudents = isAuthenticated && currentRole.canAddOrEditStudents

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Compose, 1 = History
    var audienceScope by remember { mutableStateOf(SmsAudienceScope.INDIVIDUAL) }
    var selectedClassName by remember { mutableStateOf(allClasses.firstOrNull()?.name ?: "Primary 1") }
    var selectedTemplate by remember { mutableStateOf(preselectedTemplate) }
    var recipientSearch by remember { mutableStateOf("") }
    var selectedStudentIds by remember { mutableStateOf(preselectedIds) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val selectedStudents = remember(allStudents, selectedStudentIds) {
        allStudents.filter { it.id in selectedStudentIds }
    }

    var messageBody by remember {
        mutableStateOf(
            preselectedCustomBody ?: SmsHelper.buildTemplateMessage(
                template = preselectedTemplate,
                schoolInfo = schoolInfo,
                selectedStudents = allStudents.filter { it.id in preselectedIds },
                dateStr = todayStr
            )
        )
    }

    LaunchedEffect(preselectedIds, preselectedTemplate, preselectedCustomBody) {
        if (preselectedIds.isNotEmpty()) {
            selectedStudentIds = preselectedIds
            audienceScope = SmsAudienceScope.INDIVIDUAL
        }
        selectedTemplate = preselectedTemplate
        if (!preselectedCustomBody.isNullOrBlank()) {
            messageBody = preselectedCustomBody!!
        }
    }

    // Update template text when template or single student changes (unless custom fee body is active)
    LaunchedEffect(selectedTemplate, selectedStudentIds.size) {
        if (selectedTemplate != SmsEventTemplate.CUSTOM && preselectedCustomBody == null) {
            messageBody = SmsHelper.buildTemplateMessage(
                template = selectedTemplate,
                schoolInfo = schoolInfo,
                selectedStudents = selectedStudents,
                dateStr = todayStr
            )
        }
    }

    val visibleStudents = remember(allStudents, audienceScope, selectedClassName, recipientSearch, todayAttendance, dueAndOverdueFees) {
        val base = when (audienceScope) {
            SmsAudienceScope.INDIVIDUAL -> allStudents
            SmsAudienceScope.BY_CLASS -> allStudents.filter { it.className == selectedClassName }
            SmsAudienceScope.ALL_SCHOOL -> allStudents
            SmsAudienceScope.ABSENT_TODAY -> {
                val flaggedIds = todayAttendance
                    .filter { it.status.equals("Absent", true) || it.status.equals("Late", true) }
                    .map { it.studentId }
                    .toSet()
                allStudents.filter { it.id in flaggedIds }
            }
            SmsAudienceScope.OVERDUE_FEES -> {
                val dueIds = dueAndOverdueFees.map { it.student.id }.toSet()
                allStudents.filter { it.id in dueIds }
            }
        }
        if (recipientSearch.isBlank()) {
            base.sortedBy { it.fullName }
        } else {
            val q = recipientSearch.trim().lowercase(Locale.ROOT)
            base.filter {
                it.fullName.lowercase(Locale.ROOT).contains(q) ||
                    it.parentGuardianName.lowercase(Locale.ROOT).contains(q) ||
                    it.parentGuardianPhone.contains(q) ||
                    it.className.lowercase(Locale.ROOT).contains(q)
            }.sortedBy { it.fullName }
        }
    }

    val validSelectedPhones = remember(selectedStudents) {
        selectedStudents
            .map { SmsHelper.cleanPhoneNumber(it.parentGuardianPhone) }
            .filter { SmsHelper.isValidPhoneNumber(it) }
            .distinct()
    }
    val missingPhoneCount = selectedStudents.count { !SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.testTag("sms_notification_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Parent SMS Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Bulk & Individual Alerts Linked to Student Profiles",
                            fontSize = 11.sp,
                            color = IslamicGoldAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("sms_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IslamicGreenPrimary)
            )
        },
        bottomBar = {
            if (activeTab == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Valid Parent Numbers: ${validSelectedPhones.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (validSelectedPhones.isNotEmpty()) IslamicGreenPrimary else MaterialTheme.colorScheme.error
                            )
                            if (missingPhoneCount > 0) {
                                Text(
                                    text = "$missingPhoneCount missing phone",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB8860B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (!canSendSms) {
                                    if (!isAuthenticated) onRequireLogin()
                                    else viewModel.showAccessDeniedMessage("Class Teacher or Admin")
                                    return@Button
                                }
                                if (validSelectedPhones.isEmpty()) {
                                    viewModel.showMessage("Select at least one student with a linked parent phone number.")
                                    return@Button
                                }
                                if (messageBody.trim().isBlank()) {
                                    viewModel.showMessage("Please enter an SMS message.")
                                    return@Button
                                }

                                val summary = if (selectedStudents.size == 1) {
                                    "${selectedStudents.first().parentGuardianName.ifBlank { selectedStudents.first().fullName }} (${validSelectedPhones.first()})"
                                } else {
                                    "${audienceScope.label} (${validSelectedPhones.size} parents)"
                                }

                                viewModel.logSmsNotification(
                                    eventType = selectedTemplate.title,
                                    recipientSummary = summary,
                                    recipientPhones = validSelectedPhones.joinToString(", "),
                                    recipientCount = validSelectedPhones.size,
                                    messageBody = messageBody.trim()
                                )

                                val launched = SmsHelper.launchSmsComposer(
                                    context = context,
                                    phoneNumbers = validSelectedPhones,
                                    messageBody = messageBody.trim()
                                )
                                if (launched) {
                                    viewModel.showMessage("Opened SMS app for ${validSelectedPhones.size} parent(s) & logged alert.")
                                } else {
                                    viewModel.showMessage("SMS logged & copied to clipboard (no SMS app found on device).")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_send_sms_dispatch")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (validSelectedPhones.size <= 1) "Send Individual SMS" else "Send Bulk SMS (${validSelectedPhones.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
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
            // Compose / History Switcher Bar
            Surface(
                color = IslamicGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPillButton(
                        title = "Compose SMS",
                        icon = Icons.Default.Sms,
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f).testTag("tab_sms_compose")
                    )
                    TabPillButton(
                        title = "Sent History (${smsLogs.size})",
                        icon = Icons.Default.History,
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f).testTag("tab_sms_history")
                    )
                }
            }

            if (activeTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Step 1: Event Template Selector
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "1. Notification Event Type",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = IslamicGreenPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SmsEventTemplate.values().forEach { template ->
                                        FilterChip(
                                            selected = selectedTemplate == template,
                                            onClick = {
                                                viewModel.clearSmsPreselection()
                                                selectedTemplate = template
                                            },
                                            label = { Text(template.title, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = IslamicGreenPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("sms_template_${template.name.lowercase()}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = messageBody,
                                    onValueChange = { messageBody = it },
                                    label = { Text("SMS Message Content") },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_sms_body")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val segments = ((messageBody.length - 1).coerceAtLeast(0) / 160) + 1
                                    Text(
                                        text = "${messageBody.length} chars • $segments SMS segment(s)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            viewModel.clearSmsPreselection()
                                            messageBody = SmsHelper.buildTemplateMessage(
                                                template = selectedTemplate,
                                                schoolInfo = schoolInfo,
                                                selectedStudents = selectedStudents,
                                                dateStr = todayStr
                                            )
                                        }
                                    ) {
                                        Text("Reset Template", fontSize = 11.sp, color = IslamicGreenPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Step 2: Audience Scope & Bulk Actions
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "2. Select Parent / Guardian Recipients",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = IslamicGreenPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SmsAudienceScope.values().forEach { scope ->
                                        FilterChip(
                                            selected = audienceScope == scope,
                                            onClick = {
                                                audienceScope = scope
                                                when (scope) {
                                                    SmsAudienceScope.ALL_SCHOOL -> {
                                                        selectedStudentIds = allStudents
                                                            .filter { SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }
                                                            .map { it.id }
                                                            .toSet()
                                                    }
                                                    SmsAudienceScope.BY_CLASS -> {
                                                        selectedStudentIds = allStudents
                                                            .filter { it.className == selectedClassName && SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }
                                                            .map { it.id }
                                                            .toSet()
                                                    }
                                                    SmsAudienceScope.ABSENT_TODAY -> {
                                                        viewModel.clearSmsPreselection()
                                                        val flaggedIds = todayAttendance
                                                            .filter { it.status.equals("Absent", true) || it.status.equals("Late", true) }
                                                            .map { it.studentId }
                                                            .toSet()
                                                        selectedStudentIds = allStudents
                                                            .filter { it.id in flaggedIds && SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }
                                                            .map { it.id }
                                                            .toSet()
                                                        selectedTemplate = SmsEventTemplate.ATTENDANCE_ALERT
                                                    }
                                                    SmsAudienceScope.OVERDUE_FEES -> {
                                                        viewModel.clearSmsPreselection()
                                                        val dueIds = dueAndOverdueFees.map { it.student.id }.toSet()
                                                        selectedStudentIds = allStudents
                                                            .filter { it.id in dueIds && SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }
                                                            .map { it.id }
                                                            .toSet()
                                                        selectedTemplate = SmsEventTemplate.FEE_REMINDER
                                                    }
                                                    SmsAudienceScope.INDIVIDUAL -> Unit
                                                }
                                            },
                                            label = { Text(scope.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = IslamicGreenPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("sms_scope_${scope.name.lowercase()}")
                                        )
                                    }
                                }

                                if (audienceScope == SmsAudienceScope.BY_CLASS) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    var classExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = classExpanded,
                                        onExpandedChange = { classExpanded = !classExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedClassName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Class Group") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = classExpanded,
                                            onDismissRequest = { classExpanded = false }
                                        ) {
                                            allClasses.forEach { c ->
                                                DropdownMenuItem(
                                                    text = { Text(c.name) },
                                                    onClick = {
                                                        selectedClassName = c.name
                                                        selectedStudentIds = allStudents
                                                            .filter { it.className == c.name && SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }
                                                            .map { it.id }
                                                            .toSet()
                                                        classExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = recipientSearch,
                                    onValueChange = { recipientSearch = it },
                                    placeholder = { Text("Filter by student, parent, or phone...", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = IslamicGreenPrimary)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedStudentIds = visibleStudents
                                                .filter { SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }
                                                .map { it.id }
                                                .toSet()
                                        }
                                    ) {
                                        Text("Select All Valid (${visibleStudents.count { SmsHelper.isValidPhoneNumber(it.parentGuardianPhone) }})", fontSize = 11.sp, color = IslamicGreenPrimary)
                                    }
                                    TextButton(onClick = { selectedStudentIds = emptySet() }) {
                                        Text("Clear Selection", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Student + Linked Parent Phone Cards
                    if (visibleStudents.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (audienceScope) {
                                            SmsAudienceScope.ABSENT_TODAY -> "No absent or late students recorded for today."
                                            SmsAudienceScope.OVERDUE_FEES -> "All student fee accounts are cleared or not yet due."
                                            else -> "No students match your filter."
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(visibleStudents, key = { it.id }) { student ->
                            val hasPhone = SmsHelper.isValidPhoneNumber(student.parentGuardianPhone)
                            val isChecked = student.id in selectedStudentIds

                            RecipientStudentCard(
                                student = student,
                                hasValidPhone = hasPhone,
                                isSelected = isChecked,
                                onToggle = {
                                    selectedStudentIds = if (isChecked) {
                                        selectedStudentIds - student.id
                                    } else {
                                        selectedStudentIds + student.id
                                    }
                                },
                                onLinkPhoneClick = {
                                    if (canEditStudents) {
                                        viewModel.startEditStudent(student)
                                    } else {
                                        onRequireLogin()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // History Tab
                if (smsLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No SMS notifications dispatched yet", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(smsLogs, key = { it.id }) { log ->
                            SmsHistoryCard(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (selected) IslamicGoldAccent else Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF281E04) else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF281E04) else Color.White
            )
        }
    }
}

@Composable
private fun RecipientStudentCard(
    student: Student,
    hasValidPhone: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onLinkPhoneClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("sms_recipient_card_${student.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) IslamicGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = IslamicGreenPrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.fullName} (${student.className})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Parent: ${student.parentGuardianName.ifBlank { "Guardian Not Listed" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                if (hasValidPhone) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Linked Phone",
                            tint = Color(0xFF198754),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = student.parentGuardianPhone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IslamicGreenPrimary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Missing Phone",
                            tint = Color(0xFFB8860B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "No parent phone linked",
                            fontSize = 11.sp,
                            color = Color(0xFFB8860B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!hasValidPhone) {
                OutlinedButton(
                    onClick = onLinkPhoneClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = IslamicGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link", fontSize = 11.sp, color = IslamicGreenPrimary)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = IslamicGreenPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SmsHistoryCard(log: SmsLog) {
    val formattedDate = remember(log.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = IslamicGreenPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = log.eventType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "To: ${log.recipientSummary}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.messageBody,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Prepared by ${log.sentBy} • ${log.recipientCount} recipient(s)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
