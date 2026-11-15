package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SchoolViewModel
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.StatusActiveContainer
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusSuspendedContainer
import com.example.ui.theme.StatusSuspendedRed
import com.example.ui.theme.StatusTransferredContainer
import com.example.util.CsvRowStatus
import com.example.util.ParsedCsvStudentRow
import com.example.util.ReportExporter
import com.example.util.StudentCsvImporter

@Composable
fun CsvBulkImportDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit,
    onImportCompleted: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val preview by viewModel.csvImportPreview.collectAsState()
    val schoolInfo by viewModel.schoolInfo.collectAsState()

    var pastedCsvText by remember { mutableStateOf("") }
    var showPasteEditor by remember { mutableStateOf(false) }
    var assignTermFees by remember { mutableStateOf(true) }

    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadCsvFromUri(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearCsvImportPreview()
            onDismiss()
        },
        modifier = Modifier.testTag("csv_bulk_import_dialog"),
        title = {
            Column {
                Text(
                    text = "Bulk Student CSV Import",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (preview == null) {
                        "Onboard a class or term roster from a .CSV spreadsheet"
                    } else {
                        "Preview: ${preview!!.fileName}"
                    },
                    fontSize = 12.sp,
                    color = IslamicGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            val currentPreview = preview
            if (currentPreview == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = IslamicGreenPrimary.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Supported Columns",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = IslamicGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Full Name, Class, Gender (required) • Age, Date of Birth, Date of Admission, Registration No (auto-generated if blank), Parent/Guardian, Phone, Home Address, Previous School, Status",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            csvFileLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/csv",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_pick_csv_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select .CSV File from Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasteEditor = !showPasteEditor },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_toggle_paste_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = IslamicGreenPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                val file = StudentCsvImporter.createSampleCsvFile(context, schoolInfo)
                                if (file != null) {
                                    ReportExporter.shareCsvFile(context, file, "Student Bulk Import CSV Template")
                                } else {
                                    viewModel.showMessage("Could not generate sample CSV template.")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_download_csv_template")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = IslamicGreenPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Template", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
                        }
                    }

                    if (showPasteEditor) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste CSV Rows",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = {
                                    pastedCsvText = StudentCsvImporter.buildSampleCsvContent()
                                },
                                modifier = Modifier.testTag("btn_load_sample_csv")
                            ) {
                                Text("Load Sample", fontSize = 11.sp, color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = pastedCsvText,
                            onValueChange = { pastedCsvText = it },
                            placeholder = { Text("Full Name,Class,Gender,Age,...", fontSize = 11.sp) },
                            minLines = 4,
                            maxLines = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_paste_csv")
                        )

                        Button(
                            onClick = {
                                if (pastedCsvText.isBlank()) {
                                    viewModel.showMessage("Paste CSV content first.")
                                } else {
                                    viewModel.loadCsvFromText(pastedCsvText, "pasted_students.csv")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_parse_pasted_csv")
                        ) {
                            Text("Parse & Preview CSV", color = Color(0xFF281E04), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Validation Summary Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PreviewCountPill(
                            label = "Ready",
                            count = currentPreview.validCount,
                            bg = StatusActiveContainer,
                            fg = StatusActiveGreen,
                            modifier = Modifier.weight(1f)
                        )
                        PreviewCountPill(
                            label = "Duplicate",
                            count = currentPreview.duplicateCount,
                            bg = StatusTransferredContainer,
                            fg = Color(0xFFB35300),
                            modifier = Modifier.weight(1f)
                        )
                        PreviewCountPill(
                            label = "Invalid",
                            count = currentPreview.errorCount,
                            bg = StatusSuspendedContainer,
                            fg = StatusSuspendedRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Fee auto-assignment toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assign term tuition fees",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Auto-creates class installments for imported pupils",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = assignTermFees,
                            onCheckedChange = { assignTermFees = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IslamicGreenPrimary)
                        )
                    }

                    if (currentPreview.rows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No student rows found in this CSV.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(currentPreview.rows, key = { it.rowNumber }) { row ->
                                CsvPreviewRowItem(row = row)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val currentPreview = preview
            if (currentPreview != null) {
                Button(
                    enabled = currentPreview.validCount > 0,
                    onClick = {
                        viewModel.confirmCsvBulkImport(assignTermFees = assignTermFees) { count ->
                            onImportCompleted(count)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_confirm_csv_import")
                ) {
                    Text(
                        text = "Import (${currentPreview.validCount})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (preview != null) {
                    TextButton(
                        onClick = { viewModel.clearCsvImportPreview() },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_csv_choose_another")
                    ) {
                        Text("Back")
                    }
                }
                TextButton(
                    onClick = {
                        viewModel.clearCsvImportPreview()
                        onDismiss()
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun PreviewCountPill(
    label: String,
    count: Int,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
private fun CsvPreviewRowItem(row: ParsedCsvStudentRow) {
    val (bg, fg, icon) = when (row.status) {
        CsvRowStatus.VALID -> Triple(
            StatusActiveContainer.copy(alpha = 0.45f),
            StatusActiveGreen,
            Icons.Default.CheckCircle
        )
        CsvRowStatus.DUPLICATE -> Triple(
            StatusTransferredContainer.copy(alpha = 0.55f),
            Color(0xFFB35300),
            Icons.Default.Warning
        )
        CsvRowStatus.ERROR -> Triple(
            StatusSuspendedContainer.copy(alpha = 0.55f),
            StatusSuspendedRed,
            Icons.Default.ErrorOutline
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("csv_preview_row_${row.rowNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = row.status.label,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Row ${row.rowNumber}: ${row.rawName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val meta = buildString {
                    if (row.rawClass.isNotBlank()) append(row.rawClass)
                    if (row.rawRegNo.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(row.rawRegNo)
                    }
                    if (!row.student?.parentGuardianPhone.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(row.student!!.parentGuardianPhone)
                    }
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (row.messages.isNotEmpty()) {
                    Text(
                        text = row.messages.joinToString("; "),
                        fontSize = 10.sp,
                        color = fg,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = row.status.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}
