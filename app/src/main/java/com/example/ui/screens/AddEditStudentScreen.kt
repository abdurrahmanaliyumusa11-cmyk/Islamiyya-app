package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Student
import com.example.ui.SchoolViewModel
import com.example.ui.components.CsvBulkImportDialog
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
    viewModel: SchoolViewModel,
    studentToEdit: Student?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = studentToEdit != null
    val classes by viewModel.allClasses.collectAsState()
    val scope = rememberCoroutineScope()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Form fields
    var fullName by remember { mutableStateOf(studentToEdit?.fullName ?: "") }
    var ageText by remember { mutableStateOf(studentToEdit?.age?.toString() ?: "") }
    var dateOfBirth by remember { mutableStateOf(studentToEdit?.dateOfBirth ?: "") }
    var dateOfAdmission by remember { mutableStateOf(studentToEdit?.dateOfAdmission ?: todayStr) }
    var gender by remember { mutableStateOf(studentToEdit?.gender ?: "Male") }
    var className by remember {
        mutableStateOf(
            studentToEdit?.className ?: classes.firstOrNull()?.name ?: "Primary 1"
        )
    }
    var registrationNumber by remember { mutableStateOf(studentToEdit?.registrationNumber ?: "") }
    var parentGuardianName by remember { mutableStateOf(studentToEdit?.parentGuardianName ?: "") }
    var parentGuardianPhone by remember { mutableStateOf(studentToEdit?.parentGuardianPhone ?: "") }
    var homeAddress by remember { mutableStateOf(studentToEdit?.homeAddress ?: "") }
    var previousSchool by remember { mutableStateOf(studentToEdit?.previousSchool ?: "") }
    var admissionStatus by remember { mutableStateOf(studentToEdit?.admissionStatus ?: "Active") }
    var photoUri by remember { mutableStateOf(studentToEdit?.photoUri) }
    var additionalNotes by remember { mutableStateOf(studentToEdit?.additionalNotes ?: "") }

    // Validation state
    var validationError by remember { mutableStateOf<String?>(null) }
    var showCsvImportDialog by remember { mutableStateOf(false) }

    // Auto-generate Reg Number if new student and empty
    LaunchedEffect(Unit) {
        if (!isEditMode && registrationNumber.isBlank()) {
            registrationNumber = viewModel.generateNextRegistrationNumber()
        }
    }

    // Photo picker launcher (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.testTag("add_edit_student_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Student Record" else "Register New Student",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_student_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        TextButton(
                            onClick = { showCsvImportDialog = true },
                            modifier = Modifier.testTag("add_student_bulk_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = IslamicGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bulk CSV",
                                color = IslamicGoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Validation of mandatory fields:
                            // Full Name, Age, Date of Birth, Date of Admission, Gender, Class
                            val age = ageText.toIntOrNull()
                            when {
                                fullName.trim().isBlank() -> {
                                    validationError = "Please enter the student's full name."
                                }
                                age == null || age <= 0 -> {
                                    validationError = "Please enter a valid age (positive number)."
                                }
                                dateOfBirth.trim().isBlank() -> {
                                    validationError = "Please enter the date of birth (e.g. 2019-05-12)."
                                }
                                dateOfAdmission.trim().isBlank() -> {
                                    validationError = "Please enter the date of admission."
                                }
                                className.trim().isBlank() -> {
                                    validationError = "Please select a class."
                                }
                                else -> {
                                    validationError = null
                                    val finalRegNo = if (registrationNumber.isNotBlank()) {
                                        registrationNumber.trim()
                                    } else {
                                        "UIPS/2026/${System.currentTimeMillis() % 1000}"
                                    }

                                    val student = Student(
                                        id = studentToEdit?.id ?: 0L,
                                        fullName = fullName.trim(),
                                        age = age,
                                        dateOfBirth = dateOfBirth.trim(),
                                        dateOfAdmission = dateOfAdmission.trim(),
                                        gender = gender,
                                        className = className,
                                        registrationNumber = finalRegNo,
                                        parentGuardianName = parentGuardianName.trim(),
                                        parentGuardianPhone = parentGuardianPhone.trim(),
                                        homeAddress = homeAddress.trim(),
                                        previousSchool = previousSchool.trim(),
                                        admissionStatus = admissionStatus,
                                        photoUri = photoUri,
                                        additionalNotes = additionalNotes.trim()
                                    )
                                    viewModel.saveStudent(student) {
                                        onBack()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_student_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditMode) "Save Changes" else "Register Student",
                            fontWeight = FontWeight.Bold
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Validation Error Alert
            if (validationError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { validationError = null }, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Student Photograph Upload Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, IslamicGreenPrimary, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("photo_picker_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Student Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Upload Photo",
                                    tint = IslamicGreenPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Add Photo",
                                    fontSize = 10.sp,
                                    color = IslamicGreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("choose_photo_button")
                        ) {
                            Text("Choose Photo", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (!photoUri.isNullOrBlank()) {
                            TextButton(onClick = { photoUri = null }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Mandatory Fields Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Mandatory Student Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IslamicGreenPrimary
                    )
                    HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))

                    // 1. Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Student Full Name *") },
                        placeholder = { Text("e.g. Ibrahim Khalil Muhammad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_name"),
                        singleLine = true
                    )

                    // 5. Gender Selection (Mandatory)
                    Column {
                        Text("Gender *", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = gender == "Male",
                                onClick = { gender = "Male" },
                                label = { Text("Male (Boy)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0288D1),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_gender_male")
                            )
                            FilterChip(
                                selected = gender == "Female",
                                onClick = { gender = "Female" },
                                label = { Text("Female (Girl)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8E24AA),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_gender_female")
                            )
                        }
                    }

                    // 2. Age & 3. Date of Birth
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) ageText = it },
                            label = { Text("Age *") },
                            placeholder = { Text("7") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("input_student_age"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { dateOfBirth = it },
                            label = { Text("Date of Birth *") },
                            placeholder = { Text("YYYY-MM-DD") },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = IslamicGreenPrimary)
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_student_dob"),
                            singleLine = true
                        )
                    }

                    // 4. Date of Admission
                    OutlinedTextField(
                        value = dateOfAdmission,
                        onValueChange = { dateOfAdmission = it },
                        label = { Text("Date of Admission *") },
                        placeholder = { Text("YYYY-MM-DD") },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = IslamicGreenPrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_admission_date"),
                        singleLine = true
                    )

                    // 6. Class (Dropdown)
                    var classMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = classMenuExpanded,
                        onExpandedChange = { classMenuExpanded = !classMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = className,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Class *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dropdown_student_class")
                        )
                        ExposedDropdownMenu(
                            expanded = classMenuExpanded,
                            onDismissRequest = { classMenuExpanded = false }
                        ) {
                            classes.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        className = c.name
                                        classMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 7. Student Registration Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = registrationNumber,
                            onValueChange = { registrationNumber = it },
                            label = { Text("Registration Number") },
                            placeholder = { Text("e.g. UIPS/2026/001") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_student_reg_no"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                scope.launch {
                                    registrationNumber = viewModel.generateNextRegistrationNumber()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IslamicGreenPrimary.copy(alpha = 0.12f))
                                .testTag("btn_auto_reg_number")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Auto Generate Reg Number",
                                tint = IslamicGreenPrimary
                            )
                        }
                    }
                }
            }

            // Parent / Guardian Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Parent / Guardian Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IslamicGreenPrimary
                    )
                    HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))

                    // 8. Parent Name
                    OutlinedTextField(
                        value = parentGuardianName,
                        onValueChange = { parentGuardianName = it },
                        label = { Text("Parent / Guardian Name") },
                        placeholder = { Text("e.g. Alhaji Muhammad Bello") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_parent_name"),
                        singleLine = true
                    )

                    // 9. Parent Phone Number
                    OutlinedTextField(
                        value = parentGuardianPhone,
                        onValueChange = { parentGuardianPhone = it },
                        label = { Text("Parent Phone Number") },
                        placeholder = { Text("e.g. 08031234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_parent_phone"),
                        singleLine = true
                    )

                    // 10. Home Address
                    OutlinedTextField(
                        value = homeAddress,
                        onValueChange = { homeAddress = it },
                        label = { Text("Home Residential Address") },
                        placeholder = { Text("e.g. Zango Marikita, Ungogo LGA, Kano") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_home_address")
                    )
                }
            }

            // Other Academic Details Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Academic Background & Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = IslamicGreenPrimary
                    )
                    HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))

                    // 11. Previous School
                    OutlinedTextField(
                        value = previousSchool,
                        onValueChange = { previousSchool = it },
                        label = { Text("Previous School") },
                        placeholder = { Text("e.g. Nurul Huda Kindergarten") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 12. Admission Status
                    Column {
                        Text("Admission Status", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Active", "Completed", "Transferred", "Suspended").forEach { status ->
                                FilterChip(
                                    selected = admissionStatus == status,
                                    onClick = { admissionStatus = status },
                                    label = { Text(status, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IslamicGreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 14. Additional Notes
                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Additional Remarks / Notes") },
                        placeholder = { Text("Quranic memorization stage, academic strengths, health notes, etc.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }
        }
    }

    if (showCsvImportDialog) {
        CsvBulkImportDialog(
            viewModel = viewModel,
            onDismiss = { showCsvImportDialog = false },
            onImportCompleted = { onBack() }
        )
    }
}
