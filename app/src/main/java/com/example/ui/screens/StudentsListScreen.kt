package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.SchoolViewModel
import com.example.ui.SortOption
import com.example.ui.components.CsvBulkImportDialog
import com.example.ui.components.QrStudentLookupDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary

@Composable
fun StudentsListScreen(
    viewModel: SchoolViewModel,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val students by viewModel.filteredStudents.collectAsState()
    val classes by viewModel.allClasses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedClass by viewModel.selectedClassFilter.collectAsState()
    val selectedGender by viewModel.selectedGenderFilter.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isAdmin = isAuthenticated && currentRole.canAddOrEditStudents

    var showSortMenu by remember { mutableStateOf(false) }
    var showQrLookupDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("students_list_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isAdmin) {
                        viewModel.startAddNewStudent()
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
                    .testTag("fab_add_student")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Student",
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
            // Screen Header Bar
            Surface(
                color = IslamicGreenPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Student Directory",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Total Found: ${students.size} Students",
                                fontSize = 12.sp,
                                color = IslamicGoldAccent
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isAdmin) {
                                        showCsvImportDialog = true
                                    } else if (!isAuthenticated) {
                                        onRequireLogin()
                                    } else {
                                        viewModel.showAccessDeniedMessage("Admin")
                                    }
                                },
                                modifier = Modifier.testTag("students_csv_import_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "Bulk Import Students from CSV",
                                    tint = IslamicGoldAccent
                                )
                            }

                            IconButton(
                                onClick = { showQrLookupDialog = true },
                                modifier = Modifier.testTag("students_qr_lookup_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Student QR Code",
                                    tint = IslamicGoldAccent
                                )
                            }

                            // Sort Menu Trigger
                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier.testTag("sort_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort Options",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    fontWeight = if (currentSort == option) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (currentSort == option) IslamicGreenPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                viewModel.sortOption.value = option
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Instant Realtime Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_search_input"),
                        placeholder = {
                            Text(
                                "Search student by name or registration number...",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = IslamicGoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = IslamicGoldAccent
                        )
                    )
                }
            }

            // Filters Horizontal Scroll Row
            val filterScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Classes filter chip
                FilterChip(
                    selected = selectedClass == null,
                    onClick = { viewModel.selectedClassFilter.value = null },
                    label = { Text("All Classes", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IslamicGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )

                // Individual Classes filter chips
                classes.forEach { schoolClass ->
                    FilterChip(
                        selected = selectedClass == schoolClass.name,
                        onClick = {
                            viewModel.selectedClassFilter.value =
                                if (selectedClass == schoolClass.name) null else schoolClass.name
                        },
                        label = { Text(schoolClass.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IslamicGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Gender Filters
                FilterChip(
                    selected = selectedGender == "Male",
                    onClick = {
                        viewModel.selectedGenderFilter.value =
                            if (selectedGender == "Male") null else "Male"
                    },
                    label = { Text("Boys Only", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0288D1),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedGender == "Female",
                    onClick = {
                        viewModel.selectedGenderFilter.value =
                            if (selectedGender == "Female") null else "Female"
                    },
                    label = { Text("Girls Only", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF8E24AA),
                        selectedLabelColor = Color.White
                    )
                )

                // Status Filter
                FilterChip(
                    selected = selectedStatus == "Active",
                    onClick = {
                        viewModel.selectedStatusFilter.value =
                            if (selectedStatus == "Active") null else "Active"
                    },
                    label = { Text("Active Only", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IslamicGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Students List or Empty State
            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No students match your criteria",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your search query or removing class filters.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentListItemCard(
                            student = student,
                            onClick = { viewModel.showStudentProfile(student) }
                        )
                    }
                }
            }
        }
    }

    if (showQrLookupDialog) {
        QrStudentLookupDialog(
            findStudentByPayload = { payload -> viewModel.findStudentByQrPayload(payload) },
            onStudentRetrieved = { student ->
                showQrLookupDialog = false
                viewModel.showStudentProfile(student)
            },
            onDismiss = { showQrLookupDialog = false }
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
fun StudentListItemCard(
    student: Student,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("student_card_${student.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(
                photoUri = student.photoUri,
                fullName = student.fullName,
                gender = student.gender,
                size = 52.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = student.registrationNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IslamicGreenPrimary
                    )
                    Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = student.className,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Adm: ${student.dateOfAdmission} • ${student.gender} • ${student.age} yrs",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                StatusBadge(status = student.admissionStatus)
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
