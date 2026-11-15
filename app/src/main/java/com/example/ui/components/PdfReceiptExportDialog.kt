package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.OfficialPdfExporter
import java.io.File

@Composable
fun PdfReceiptExportDialog(
    pdfFile: File,
    title: String,
    subtitle: String,
    amountLine: String? = null,
    onNotify: (String) -> Unit,
    onPrintFallback: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri != null) {
            val saved = OfficialPdfExporter.writePdfFileToUri(context, pdfFile, targetUri)
            if (saved) {
                onNotify("Official PDF saved: ${pdfFile.name}")
                onDismiss()
            } else {
                onNotify("Failed to save PDF to selected location.")
            }
        }
    }

    val sizeKb = ((pdfFile.length().coerceAtLeast(1024L)) / 1024L).coerceAtLeast(1L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = IslamicGreenPrimary
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = IslamicGreenPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IslamicGreenPrimary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = subtitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!amountLine.isNullOrBlank()) {
                            Text(
                                text = amountLine,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = IslamicGreenPrimary
                            )
                        }
                        Text(
                            text = "${pdfFile.name} • ${sizeKb} KB • A4 Official PDF",
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Primary: Download PDF directly (MediaStore Downloads on API 29+, SAF fallback on API < 29)
                Button(
                    onClick = {
                        val uri = OfficialPdfExporter.savePdfToDownloadsMediaStore(context, pdfFile)
                        if (uri != null) {
                            onNotify("PDF downloaded to Downloads/Umuriddin_Receipts/${pdfFile.name}")
                            onDismiss()
                        } else {
                            createPdfLauncher.launch(pdfFile.name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_pdf_downloads")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download PDF to Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Secondary: Save As via Storage Access Framework
                OutlinedButton(
                    onClick = { createPdfLauncher.launch(pdfFile.name) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_pdf_saf")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = IslamicGreenPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Choose Folder (Save As .pdf)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = IslamicGreenPrimary
                    )
                }

                // Row: Open & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = OfficialPdfExporter.openPdfFile(context, pdfFile)
                            if (!ok) {
                                onNotify("No PDF viewer app found on this device. Use Download PDF instead.")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_open_pdf_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreenPrimary)
                    }

                    Button(
                        onClick = {
                            val ok = OfficialPdfExporter.sharePdfFile(context, pdfFile, subtitle)
                            if (!ok) {
                                onNotify("No sharing app available on this device.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IslamicGoldAccent,
                            contentColor = Color(0xFF281E04)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_pdf_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (onPrintFallback != null) {
                    TextButton(
                        onClick = {
                            onPrintFallback()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_print_pdf_fallback")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open System Print Preview", color = IslamicGreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_close_pdf_dialog")
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
            }
        }
    )
}
