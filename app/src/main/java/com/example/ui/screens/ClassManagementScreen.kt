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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SchoolClass
import com.example.data.model.Teacher
import com.example.ui.Screen
import com.example.ui.SchoolViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary

@Composable
fun ClassManagementScreen(
    viewModel: SchoolViewModel,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val classes by viewModel.allClasses.collectAsState()
    val teachers by viewModel.allTeachers.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isAdmin = isAuthenticated && currentRole.canManageClasses

    var showAddDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<SchoolClass?>(null) }
    var classToDelete by remember { mutableStateOf<SchoolClass?>(null) }

    Scaffold(
        modifier = modifier.testTag("class_management_screen"),
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
                    .testTag("fab_add_class")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Class",
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
            // Header bar
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
                        text = "Class & Attendance Management",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Manage primary classes, daily attendance sheets & teacher assignments",
                        fontSize = 12.sp,
                        color = IslamicGoldAccent
                    )
                }
            }

            // Teachers Management Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable { viewModel.navigateTo(Screen.TEACHERS) }
                    .testTag("btn_manage_teachers_banner"),
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
                                .background(IslamicGoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color(0xFF9A7B39),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Staff & Teachers Management",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${teachers.size} Teachers Registered • Tap to add/remove",
                                fontSize = 11.sp,
                                color = IslamicGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Manage Teachers",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Classes List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(classes, key = { it.id }) { schoolClass ->
                    val enrolledCount = stats.classDistribution[schoolClass.name] ?: 0

                    ClassItemCard(
                        schoolClass = schoolClass,
                        enrolledCount = enrolledCount,
                        isAdmin = isAdmin,
                        onOpenAttendance = {
                            viewModel.openAttendanceSheet(schoolClass)
                        },
                        onViewStudents = {
                            viewModel.selectedClassFilter.value = schoolClass.name
                            viewModel.navigateTo(Screen.STUDENTS)
                        },
                        onEdit = {
                            if (isAdmin) {
                                classToEdit = schoolClass
                            } else if (!isAuthenticated) {
                                onRequireLogin()
                            } else {
                                viewModel.showAccessDeniedMessage("Admin")
                            }
                        },
                        onDelete = {
                            if (isAdmin) {
                                classToDelete = schoolClass
                            } else if (!isAuthenticated) {
                                onRequireLogin()
                            } else {
                                viewModel.showAccessDeniedMessage("Admin")
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Class Dialog
    if (showAddDialog) {
        ClassFormDialog(
            title = "Add New Class",
            initialName = "",
            initialSection = "A",
            initialTeacher = "",
            initialCapacity = 40,
            teacherList = teachers,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, section, teacher, cap ->
                viewModel.addClass(name, section, teacher, cap)
                showAddDialog = false
            }
        )
    }

    // Edit Class Dialog
    if (classToEdit != null) {
        val target = classToEdit!!
        ClassFormDialog(
            title = "Edit ${target.name}",
            initialName = target.name,
            initialSection = target.section,
            initialTeacher = target.classTeacher,
            initialCapacity = target.capacity,
            teacherList = teachers,
            onDismiss = { classToEdit = null },
            onConfirm = { name, section, teacher, cap ->
                viewModel.updateClass(
                    target.copy(
                        name = name,
                        section = section,
                        classTeacher = teacher,
                        capacity = cap
                    )
                )
                classToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (classToDelete != null) {
        val target = classToDelete!!
        ConfirmDeleteDialog(
            title = "Delete Class '${target.name}'",
            message = "Are you sure you want to remove ${target.name}? Note that students registered in this class will keep their records.",
            onConfirm = {
                viewModel.deleteClass(target)
                classToDelete = null
            },
            onDismiss = { classToDelete = null }
        )
    }
}

@Composable
private fun ClassItemCard(
    schoolClass: SchoolClass,
    enrolledCount: Int,
    isAdmin: Boolean,
    onOpenAttendance: () -> Unit,
    onViewStudents: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("class_card_${schoolClass.id}"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = schoolClass.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Section ${schoolClass.section}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Enrolled Pill
                Surface(
                    color = IslamicGreenPrimary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "$enrolledCount Students",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (schoolClass.classTeacher.isNotBlank()) {
                Text(
                    text = "Class Teacher: ${schoolClass.classTeacher}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Target Capacity: ${schoolClass.capacity} students",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Attendance Sheet & View Students Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // [Attendance Sheet Button]
                    Button(
                        onClick = onOpenAttendance,
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_class_attendance_${schoolClass.id}")
                    ) {
                        Icon(imageVector = Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attendance Sheet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // [View Students]
                    OutlinedButton(
                        onClick = onViewStudents,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_view_class_students_${schoolClass.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Students", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Class", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Class", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassFormDialog(
    title: String,
    initialName: String,
    initialSection: String,
    initialTeacher: String,
    initialCapacity: Int,
    teacherList: List<Teacher>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, section: String, teacher: String, capacity: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var section by remember { mutableStateOf(initialSection) }
    var teacher by remember { mutableStateOf(initialTeacher) }
    var capacityStr by remember { mutableStateOf(initialCapacity.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    var teacherDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Class Name *") },
                    placeholder = { Text("e.g. Primary 1 or Tahfeez Section") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section / Arm") },
                    placeholder = { Text("e.g. A, B or Morning") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Teacher selection dropdown
                ExposedDropdownMenuBox(
                    expanded = teacherDropdownExpanded,
                    onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Assigned Teacher") },
                        placeholder = { Text("Select from staff or type name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = teacherDropdownExpanded,
                        onDismissRequest = { teacherDropdownExpanded = false }
                    ) {
                        teacherList.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.fullName} (${t.staffId})") },
                                onClick = {
                                    teacher = t.fullName
                                    teacherDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = capacityStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) capacityStr = it },
                    label = { Text("Class Capacity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        error = "Class name cannot be empty"
                    } else {
                        val cap = capacityStr.toIntOrNull() ?: 40
                        onConfirm(name.trim(), section.trim(), teacher.trim(), cap)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
            ) {
                Text("Save Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
