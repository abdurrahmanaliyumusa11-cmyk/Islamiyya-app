package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StaffRole
import com.example.data.model.Teacher
import com.example.ui.SchoolViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersListScreen(
    viewModel: SchoolViewModel,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val teachers by viewModel.allTeachers.collectAsState()
    val classes by viewModel.allClasses.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isAdmin = isAuthenticated && currentRole.canManageStaff

    var roleFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var teacherToEdit by remember { mutableStateOf<Teacher?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }

    val filteredTeachers = remember(teachers, roleFilter) {
        if (roleFilter == null) teachers
        else teachers.filter { it.role.equals(roleFilter, ignoreCase = true) }
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.testTag("teachers_list_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Staff & Teachers Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Total Staff: ${teachers.size} • Role Access Control Enabled",
                            fontSize = 11.sp,
                            color = IslamicGoldAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("teachers_back_button")
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isAdmin) {
                        showAddDialog = true
                    } else if (!isAuthenticated) {
                        onRequireLogin()
                    } else {
                        viewModel.showAccessDeniedMessage("Admin")
                    }
                },
                containerColor = IslamicGreenPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("fab_add_teacher")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Teacher",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notice banner for role & admin controls
            Surface(
                color = IslamicGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAdmin) {
                            "Admin Mode: Assign roles (Admin, Class Teacher, Subject Teacher)."
                        } else {
                            "Active Role: ${currentRole.displayName} (Read-Only Staff Directory)"
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f)
                    )
                    if (!isAdmin) {
                        TextButton(onClick = onRequireLogin) {
                            Text("Switch Role", color = IslamicGoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Role Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = roleFilter == null,
                    onClick = { roleFilter = null },
                    label = { Text("All (${teachers.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IslamicGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                StaffRole.allRoles.forEach { role ->
                    val count = teachers.count { it.role.equals(role.displayName, ignoreCase = true) }
                    FilterChip(
                        selected = roleFilter == role.displayName,
                        onClick = {
                            roleFilter = if (roleFilter == role.displayName) null else role.displayName
                        },
                        label = { Text("${role.displayName} ($count)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IslamicGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_role_${role.name.lowercase()}")
                    )
                }
            }

            if (filteredTeachers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No staff members match this role",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+' as Admin to register teachers or assign roles.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTeachers, key = { it.id }) { teacher ->
                        TeacherCardItem(
                            teacher = teacher,
                            canManage = isAdmin,
                            onEdit = {
                                if (isAdmin) {
                                    teacherToEdit = teacher
                                } else if (!isAuthenticated) {
                                    onRequireLogin()
                                } else {
                                    viewModel.showAccessDeniedMessage("Admin")
                                }
                            },
                            onDelete = {
                                if (isAdmin) {
                                    teacherToDelete = teacher
                                } else if (!isAuthenticated) {
                                    onRequireLogin()
                                } else {
                                    viewModel.showAccessDeniedMessage("Admin")
                                }
                            },
                            onSwitchToTeacherRole = {
                                val resolved = StaffRole.fromString(teacher.role)
                                viewModel.switchRoleDirectly(resolved, teacher)
                            },
                            onCall = {
                                if (teacher.phoneNumber.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${teacher.phoneNumber}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Teacher Dialog
    if (showAddDialog) {
        TeacherFormDialog(
            title = "Register New Staff Member",
            initialFullName = "",
            initialStaffId = "",
            initialPhone = "",
            initialGender = "Male",
            initialQualification = "NCE Arabic & Islamic Studies",
            initialAssignedClass = classes.firstOrNull()?.name ?: "",
            initialSubject = "Islamic Studies & Arabic",
            initialRole = StaffRole.CLASS_TEACHER.displayName,
            classList = classes.map { it.name },
            onGenerateStaffId = {
                viewModel.generateNextStaffId()
            },
            onDismiss = { showAddDialog = false },
            onConfirm = { teacher ->
                viewModel.addTeacher(teacher) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Teacher Dialog
    if (teacherToEdit != null) {
        val target = teacherToEdit!!
        TeacherFormDialog(
            title = "Edit Staff: ${target.fullName}",
            initialFullName = target.fullName,
            initialStaffId = target.staffId,
            initialPhone = target.phoneNumber,
            initialGender = target.gender,
            initialQualification = target.qualification,
            initialAssignedClass = target.assignedClass,
            initialSubject = target.subjectSpecialization,
            initialRole = target.role,
            classList = classes.map { it.name },
            onGenerateStaffId = { target.staffId },
            onDismiss = { teacherToEdit = null },
            onConfirm = { updated ->
                viewModel.updateTeacher(updated.copy(id = target.id)) {
                    teacherToEdit = null
                }
            }
        )
    }

    // Delete Teacher Confirmation Dialog
    if (teacherToDelete != null) {
        val target = teacherToDelete!!
        ConfirmDeleteDialog(
            title = "Remove Teacher Record",
            message = "Are you sure you want to remove '${target.fullName}' (${target.staffId}) from the staff registry?",
            onConfirm = {
                viewModel.deleteTeacher(target) {
                    teacherToDelete = null
                }
            },
            onDismiss = { teacherToDelete = null }
        )
    }
}

@Composable
private fun TeacherCardItem(
    teacher: Teacher,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitchToTeacherRole: () -> Unit,
    onCall: () -> Unit
) {
    val staffRole = StaffRole.fromString(teacher.role)
    val (roleBg, roleText) = when (staffRole) {
        StaffRole.ADMIN -> IslamicGoldAccent to Color(0xFF281E04)
        StaffRole.CLASS_TEACHER -> IslamicGreenPrimary.copy(alpha = 0.14f) to IslamicGreenPrimary
        StaffRole.SUBJECT_TEACHER -> Color(0xFF0288D1).copy(alpha = 0.14f) to Color(0xFF0277BD)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_card_${teacher.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (teacher.gender.equals("Female", ignoreCase = true)) Color(0xFFE8D7F1) else IslamicGreenPrimary.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (teacher.gender.equals("Female", ignoreCase = true)) Color(0xFF6A1B9A) else IslamicGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacher.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = IslamicGreenPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = teacher.staffId,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = roleBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = staffRole.displayName,
                                color = roleText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (teacher.assignedClass.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Assigned Class: ${teacher.assignedClass}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IslamicGreenPrimary
                        )
                    }
                }

                if (teacher.phoneNumber.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IslamicGreenPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Teacher",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Qualification: ${teacher.qualification}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Specialization: ${teacher.subjectSpecialization}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (teacher.phoneNumber.isNotBlank()) {
                Text(
                    text = "Phone: ${teacher.phoneNumber}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canManage) {
                    OutlinedButton(
                        onClick = onSwitchToTeacherRole,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_act_as_teacher_${teacher.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use ${staffRole.displayName} View", fontSize = 11.sp, color = IslamicGreenPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Teacher", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Teacher", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherFormDialog(
    title: String,
    initialFullName: String,
    initialStaffId: String,
    initialPhone: String,
    initialGender: String,
    initialQualification: String,
    initialAssignedClass: String,
    initialSubject: String,
    initialRole: String,
    classList: List<String>,
    onGenerateStaffId: suspend () -> String,
    onDismiss: () -> Unit,
    onConfirm: (Teacher) -> Unit
) {
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf(initialFullName) }
    var staffId by remember { mutableStateOf(initialStaffId) }
    var phone by remember { mutableStateOf(initialPhone) }
    var gender by remember { mutableStateOf(initialGender) }
    var qualification by remember { mutableStateOf(initialQualification) }
    var assignedClass by remember { mutableStateOf(initialAssignedClass) }
    var subject by remember { mutableStateOf(initialSubject) }
    var role by remember { mutableStateOf(initialRole) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (staffId.isBlank()) {
            staffId = onGenerateStaffId()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; error = null },
                    label = { Text("Teacher Full Name *") },
                    placeholder = { Text("e.g. Mallam Aliyu Danladi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = staffId,
                        onValueChange = { staffId = it },
                        label = { Text("Staff ID *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            scope.launch { staffId = onGenerateStaffId() }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IslamicGreenPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Auto Staff ID", tint = IslamicGreenPrimary)
                    }
                }

                // Role Assignment Selector
                Text(
                    text = "Access Control Role *",
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
                    StaffRole.allRoles.forEach { staffRole ->
                        FilterChip(
                            selected = role.equals(staffRole.displayName, ignoreCase = true),
                            onClick = { role = staffRole.displayName },
                            label = { Text(staffRole.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IslamicGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("teacher_role_chip_${staffRole.name.lowercase()}")
                        )
                    }
                }
                Text(
                    text = StaffRole.fromString(role).description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Gender Toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = gender == "Male",
                        onClick = { gender = "Male" },
                        label = { Text("Male") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0288D1),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = gender == "Female",
                        onClick = { gender = "Female" },
                        label = { Text("Female") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8E24AA),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification") },
                    placeholder = { Text("e.g. NCE Arabic & Islamic Studies") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                var classExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = classExpanded,
                    onExpandedChange = { classExpanded = !classExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = assignedClass.ifBlank { "Unassigned" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = classExpanded,
                        onDismissRequest = { classExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unassigned / General") },
                            onClick = {
                                assignedClass = ""
                                classExpanded = false
                            }
                        )
                        classList.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    assignedClass = c
                                    classExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Specialization") },
                    placeholder = { Text("e.g. Quranic Recitation & Fiqh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.trim().isBlank()) {
                        error = "Teacher name cannot be empty"
                    } else if (staffId.trim().isBlank()) {
                        error = "Staff ID cannot be empty"
                    } else {
                        onConfirm(
                            Teacher(
                                fullName = fullName.trim(),
                                staffId = staffId.trim(),
                                phoneNumber = phone.trim(),
                                gender = gender,
                                qualification = qualification.trim(),
                                assignedClass = assignedClass.trim(),
                                subjectSpecialization = subject.trim(),
                                role = role
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_teacher")
            ) {
                Text("Save Teacher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
