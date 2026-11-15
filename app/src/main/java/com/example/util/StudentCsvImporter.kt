package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.SchoolInfo
import com.example.data.model.Student
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CsvRowStatus(val label: String) {
    VALID("Ready"),
    DUPLICATE("Duplicate"),
    ERROR("Invalid")
}

private enum class CsvColumnField {
    FULL_NAME,
    REGISTRATION_NO,
    CLASS_NAME,
    GENDER,
    AGE,
    DATE_OF_BIRTH,
    DATE_OF_ADMISSION,
    PARENT_NAME,
    PARENT_PHONE,
    HOME_ADDRESS,
    PREVIOUS_SCHOOL,
    ADMISSION_STATUS,
    NOTES
}

data class ParsedCsvStudentRow(
    val rowNumber: Int,
    val student: Student?,
    val status: CsvRowStatus,
    val messages: List<String>,
    val rawName: String,
    val rawClass: String,
    val rawRegNo: String
)

data class CsvImportPreviewResult(
    val fileName: String,
    val detectedHeaders: List<String>,
    val rows: List<ParsedCsvStudentRow>,
    val headerRecognized: Boolean
) {
    val validCount: Int get() = rows.count { it.status == CsvRowStatus.VALID }
    val duplicateCount: Int get() = rows.count { it.status == CsvRowStatus.DUPLICATE }
    val errorCount: Int get() = rows.count { it.status == CsvRowStatus.ERROR }
    val validStudents: List<Student> get() = rows.filter { it.status == CsvRowStatus.VALID }.mapNotNull { it.student }
}

object StudentCsvImporter {

    /**
     * Parses RFC-4180 compliant CSV text into a list of rows of unescaped field strings.
     * Supports UTF-8 BOM, CRLF/LF/CR newlines, quoted commas/newlines, and doubled quotes ("").
     */
    fun parseCsvRows(csvText: String): List<List<String>> {
        val cleaned = csvText.removePrefix("\uFEFF")
        if (cleaned.isBlank()) return emptyList()

        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = cleaned.length

        while (i < len) {
            val ch = cleaned[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < len && cleaned[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(field.toString().trim())
                        field.setLength(0)
                    }
                    '\r', '\n' -> {
                        if (ch == '\r' && i + 1 < len && cleaned[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(field.toString().trim())
                        field.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow)
                        }
                        currentRow = mutableListOf()
                    }
                    else -> field.append(ch)
                }
            }
            i++
        }

        // Flush final field / row
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow)
            }
        }

        return rows
    }

    /**
     * Parses CSV content into validated Student objects + per-row statuses for UI preview.
     */
    fun parseAndValidate(
        csvText: String,
        existingStudents: List<Student>,
        knownClassNames: List<String> = listOf(
            "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", "Primary 6"
        ),
        fileName: String = "students_import.csv",
        currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        todayDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ): CsvImportPreviewResult {
        val rawRows = parseCsvRows(csvText)
        if (rawRows.isEmpty()) {
            return CsvImportPreviewResult(
                fileName = fileName,
                detectedHeaders = emptyList(),
                rows = emptyList(),
                headerRecognized = false
            )
        }

        // Locate header row (handles optional banner/preamble lines such as exported CSV reports)
        var headerIndex = -1
        var columnMap = emptyMap<Int, CsvColumnField>()

        for (idx in 0 until minOf(rawRows.size, 8)) {
            val mapped = mapHeaderColumns(rawRows[idx])
            if (mapped.containsValue(CsvColumnField.FULL_NAME) &&
                (mapped.containsValue(CsvColumnField.CLASS_NAME) || mapped.size >= 2)
            ) {
                headerIndex = idx
                columnMap = mapped
                break
            }
        }

        if (headerIndex == -1 || columnMap.isEmpty()) {
            // Unrecognized header; still check if first row already looks like headerless data vs bad headers
            return CsvImportPreviewResult(
                fileName = fileName,
                detectedHeaders = rawRows.firstOrNull().orEmpty(),
                rows = rawRows.mapIndexed { idx, cols ->
                    ParsedCsvStudentRow(
                        rowNumber = idx + 1,
                        student = null,
                        status = CsvRowStatus.ERROR,
                        messages = listOf("Missing required CSV header row (needs at least Full Name, Class, Gender)."),
                        rawName = cols.firstOrNull().orEmpty(),
                        rawClass = cols.getOrNull(1).orEmpty(),
                        rawRegNo = ""
                    )
                },
                headerRecognized = false
            )
        }

        val headerRow = rawRows[headerIndex]
        val dataRows = rawRows.drop(headerIndex + 1)

        val existingRegSet = existingStudents
            .map { it.registrationNumber.trim().uppercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .toMutableSet()

        val existingIdentitySet = existingStudents
            .map { identityKey(it.fullName, it.className, it.dateOfBirth) }
            .toMutableSet()

        var seqCounter = existingStudents.size
        val parsedRows = mutableListOf<ParsedCsvStudentRow>()

        dataRows.forEachIndexed { relIdx, cols ->
            val displayRowNum = headerIndex + 2 + relIdx
            fun getCol(target: CsvColumnField): String {
                val colIdx = columnMap.entries.firstOrNull { it.value == target }?.key ?: return ""
                return cols.getOrNull(colIdx)?.trim().orEmpty()
            }

            val rawName = getCol(CsvColumnField.FULL_NAME)
            val rawClass = getCol(CsvColumnField.CLASS_NAME)
            val rawGender = getCol(CsvColumnField.GENDER)
            val rawAge = getCol(CsvColumnField.AGE)
            val rawDob = getCol(CsvColumnField.DATE_OF_BIRTH)
            val rawAdmDate = getCol(CsvColumnField.DATE_OF_ADMISSION)
            val rawRegNo = getCol(CsvColumnField.REGISTRATION_NO)
            val rawParentName = getCol(CsvColumnField.PARENT_NAME)
            val rawParentPhone = getCol(CsvColumnField.PARENT_PHONE)
            val rawAddress = getCol(CsvColumnField.HOME_ADDRESS)
            val rawPrevSchool = getCol(CsvColumnField.PREVIOUS_SCHOOL)
            val rawStatus = getCol(CsvColumnField.ADMISSION_STATUS)
            val rawNotes = getCol(CsvColumnField.NOTES)

            val errors = mutableListOf<String>()
            val notes = mutableListOf<String>()

            if (rawName.isBlank()) {
                errors.add("Missing student full name")
            }

            val normalizedClass = normalizeClassName(rawClass, knownClassNames)
            if (normalizedClass.isBlank()) {
                errors.add("Missing class")
            }

            val normalizedGender = normalizeGender(rawGender)
            if (normalizedGender == null) {
                errors.add(
                    if (rawGender.isBlank()) "Missing gender"
                    else "Invalid gender '$rawGender' (use Male or Female)"
                )
            }

            val resolvedAge: Int? = resolveAge(rawAge, rawDob, currentYear, errors, notes)
            val resolvedDob = resolveDob(rawDob, resolvedAge, currentYear)
            val resolvedAdmDate = if (rawAdmDate.isNotBlank()) rawAdmDate else todayDateStr
            val resolvedStatus = normalizeAdmissionStatus(rawStatus)
            val cleanedPhone = SmsHelper.cleanPhoneNumber(rawParentPhone)

            if (errors.isNotEmpty()) {
                parsedRows.add(
                    ParsedCsvStudentRow(
                        rowNumber = displayRowNum,
                        student = null,
                        status = CsvRowStatus.ERROR,
                        messages = errors,
                        rawName = rawName.ifBlank { "(Unnamed Row $displayRowNum)" },
                        rawClass = rawClass,
                        rawRegNo = rawRegNo
                    )
                )
                return@forEachIndexed
            }

            // Assign or check Registration Number
            val assignedRegNo = if (rawRegNo.isNotBlank()) {
                rawRegNo.trim()
            } else {
                var candidate: String
                do {
                    seqCounter += 1
                    candidate = "UIPS/$currentYear/${seqCounter.toString().padStart(3, '0')}"
                } while (candidate.uppercase(Locale.ROOT) in existingRegSet)
                notes.add("Auto-ID: $candidate")
                candidate
            }

            val regKey = assignedRegNo.uppercase(Locale.ROOT)
            val idKey = identityKey(rawName, normalizedClass, resolvedDob)
            val isDuplicate = (rawRegNo.isNotBlank() && regKey in existingRegSet) || (idKey in existingIdentitySet)

            if (isDuplicate) {
                parsedRows.add(
                    ParsedCsvStudentRow(
                        rowNumber = displayRowNum,
                        student = null,
                        status = CsvRowStatus.DUPLICATE,
                        messages = listOf("Duplicate record ($assignedRegNo / $rawName already exists)"),
                        rawName = rawName,
                        rawClass = normalizedClass,
                        rawRegNo = assignedRegNo
                    )
                )
                return@forEachIndexed
            }

            existingRegSet.add(regKey)
            existingIdentitySet.add(idKey)

            val student = Student(
                fullName = rawName,
                age = resolvedAge ?: 7,
                dateOfBirth = resolvedDob,
                dateOfAdmission = resolvedAdmDate,
                gender = normalizedGender!!,
                className = normalizedClass,
                registrationNumber = assignedRegNo,
                parentGuardianName = rawParentName,
                parentGuardianPhone = cleanedPhone,
                homeAddress = rawAddress,
                previousSchool = rawPrevSchool,
                admissionStatus = resolvedStatus,
                additionalNotes = rawNotes
            )

            parsedRows.add(
                ParsedCsvStudentRow(
                    rowNumber = displayRowNum,
                    student = student,
                    status = CsvRowStatus.VALID,
                    messages = notes,
                    rawName = student.fullName,
                    rawClass = student.className,
                    rawRegNo = student.registrationNumber
                )
            )
        }

        return CsvImportPreviewResult(
            fileName = fileName,
            detectedHeaders = headerRow,
            rows = parsedRows,
            headerRecognized = true
        )
    }

    private fun mapHeaderColumns(headers: List<String>): Map<Int, CsvColumnField> {
        val map = mutableMapOf<Int, CsvColumnField>()
        headers.forEachIndexed { index, rawHeader ->
            val key = rawHeader.lowercase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ")
                .replace("/", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val field = when (key) {
                "full name", "fullname", "student name", "name", "pupil name", "student" -> CsvColumnField.FULL_NAME
                "registration no", "registration number", "reg no", "regno", "reg number", "student id", "admission no", "admission number" -> CsvColumnField.REGISTRATION_NO
                "class", "class name", "classname", "grade", "level" -> CsvColumnField.CLASS_NAME
                "gender", "sex" -> CsvColumnField.GENDER
                "age", "years" -> CsvColumnField.AGE
                "date of birth", "dob", "birth date", "birthdate" -> CsvColumnField.DATE_OF_BIRTH
                "date of admission", "admission date", "dateofadmission", "enrolled date", "admitted" -> CsvColumnField.DATE_OF_ADMISSION
                "parent guardian", "parent name", "guardian name", "parent", "guardian", "parent or guardian" -> CsvColumnField.PARENT_NAME
                "phone", "parent phone", "guardian phone", "phone number", "mobile", "contact", "parent contact" -> CsvColumnField.PARENT_PHONE
                "home address", "address", "residential address" -> CsvColumnField.HOME_ADDRESS
                "previous school", "former school", "prev school" -> CsvColumnField.PREVIOUS_SCHOOL
                "status", "admission status" -> CsvColumnField.ADMISSION_STATUS
                "notes", "additional notes", "remarks" -> CsvColumnField.NOTES
                else -> null
            }

            if (field != null && !map.containsValue(field)) {
                map[index] = field
            }
        }
        return map
    }

    private fun normalizeGender(raw: String): String? {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "m", "male", "boy" -> "Male"
            "f", "female", "girl" -> "Female"
            else -> null
        }
    }

    private fun normalizeClassName(raw: String, knownClasses: List<String>): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        // Direct case-insensitive match against school classes
        knownClasses.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }

        // Shorthand support ("1", "P1", "Pri 1", "Primary1", etc.)
        val compact = trimmed.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val digitMatch = Regex("(?:primary|pri|p)?([1-6])$").matchEntire(compact)
        if (digitMatch != null) {
            val level = digitMatch.groupValues[1]
            val candidate = "Primary $level"
            return knownClasses.firstOrNull { it.equals(candidate, ignoreCase = true) } ?: candidate
        }
        return trimmed
    }

    private fun resolveAge(
        rawAge: String,
        rawDob: String,
        currentYear: Int,
        errors: MutableList<String>,
        notes: MutableList<String>
    ): Int? {
        if (rawAge.isNotBlank()) {
            val numeric = rawAge.trim().toIntOrNull()
            return if (numeric != null && numeric in 3..25) {
                numeric
            } else {
                errors.add("Invalid age '$rawAge' (expected 3–25)")
                null
            }
        }
        // Derive from DOB yyyy-... if available
        val yearMatch = Regex("^(\\d{4})-\\d{2}-\\d{2}$").matchEntire(rawDob.trim())
        if (yearMatch != null) {
            val birthYear = yearMatch.groupValues[1].toIntOrNull()
            if (birthYear != null) {
                val computed = (currentYear - birthYear).coerceIn(3, 25)
                notes.add("Age $computed from DOB")
                return computed
            }
        }
        notes.add("Default age 7")
        return 7
    }

    private fun resolveDob(rawDob: String, age: Int?, currentYear: Int): String {
        val trimmed = rawDob.trim()
        if (trimmed.isNotBlank()) return trimmed
        val estYear = currentYear - (age ?: 7)
        return "$estYear-01-01"
    }

    private fun normalizeAdmissionStatus(raw: String): String {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "completed", "graduated" -> "Completed"
            "transferred", "transfer" -> "Transferred"
            "suspended" -> "Suspended"
            else -> "Active"
        }
    }

    private fun identityKey(name: String, className: String, dob: String): String {
        return "${name.trim().lowercase(Locale.ROOT)}|${className.trim().lowercase(Locale.ROOT)}|${dob.trim()}"
    }

    fun readCsvFromUri(context: Context, uri: Uri): Pair<String, String> {
        var displayName = "imported_students.csv"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                }
            }
        }
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        }.orEmpty()
        return displayName to text
    }

    fun buildSampleCsvContent(): String {
        return """
            Full Name,Class,Gender,Age,Date of Birth,Date of Admission,Parent/Guardian,Phone,Home Address,Previous School,Status
            "Bello, Sadiq Garba",Primary 1,Male,6,2020-04-12,2026-09-01,Malam Garba Bello,08035551122,"Ungogo Quarters, Kano",Direct Entry,Active
            Halima Abdullahi Sani,Primary 2,Female,7,2019-07-19,2026-09-01,Hajiya Zainab Sani,08064442233,"Zango Marikita, Ungogo",Nurul Islam Nursery,Active
            Yusuf Auwal Musa,Primary 4,Male,9,2017-02-05,2026-09-01,Alhaji Auwal Musa,08097773344,"Rimin Gata, Kano",Al-Hikma Primary,Active
        """.trimIndent()
    }

    fun createSampleCsvFile(context: Context, schoolInfo: SchoolInfo): File? {
        return try {
            val file = File(context.cacheDir, "Umuriddin_Student_Import_Template.csv")
            FileWriter(file).use { writer ->
                writer.write(buildSampleCsvContent())
                writer.write("\n")
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
