package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.SchoolInfo
import com.example.data.model.Student
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.StudentIdCardGenerator

@Composable
fun StudentIdCardDialog(
    student: Student,
    schoolInfo: SchoolInfo,
    onDismiss: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val qrPayload = remember(student.id, student.registrationNumber) {
        StudentIdCardGenerator.encodeStudentQrPayload(student)
    }
    val qrBitmap = remember(qrPayload) {
        StudentIdCardGenerator.generateQrBitmap(qrPayload, sizePx = 420)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("student_id_card_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Dialog Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Digital Student ID Card",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("id_card_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Digital ID Card"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Visual Student ID Card Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, IslamicGreenPrimary, RoundedCornerShape(16.dp))
                        .testTag("id_card_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFDFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Branded Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(IslamicGreenPrimary, IslamicGreenDark)
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_school_logo),
                                contentDescription = "School Crest",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, IslamicGoldAccent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schoolInfo.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 14.sp
                                )
                                Text(
                                    text = schoolInfo.location,
                                    color = Color(0xFFD4EDE5),
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp
                                )
                                Text(
                                    text = "DIGITAL STUDENT IDENTITY CARD",
                                    color = IslamicGoldAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        // Gold Accent Strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(IslamicGoldAccent)
                        )

                        // Card Content Body
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(90.dp)
                                ) {
                                    StudentAvatar(
                                        photoUri = student.photoUri,
                                        fullName = student.fullName,
                                        gender = student.gender,
                                        size = 76.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    StatusBadge(status = student.admissionStatus)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "STUDENT NAME",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5D6D66),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = student.fullName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IslamicGreenDark,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.testTag("id_card_student_name")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "CLASS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5D6D66),
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        color = IslamicGreenPrimary,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = student.className,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                                .testTag("id_card_student_class")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "UNIQUE STUDENT ID",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF5D6D66),
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        color = Color(0xFFF5EFE0),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .border(1.dp, IslamicGoldAccent, RoundedCornerShape(6.dp))
                                    ) {
                                        val idText = if (student.id > 0L) {
                                            "${student.registrationNumber} (#${student.id})"
                                        } else {
                                            student.registrationNumber
                                        }
                                        Text(
                                            text = idText,
                                            color = Color(0xFF281E04),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("id_card_student_uid")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // QR Code Container
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(1.5.dp, IslamicGoldAccent, RoundedCornerShape(12.dp))
                                    .testTag("id_card_qr_container")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code for ${student.fullName} (${student.registrationNumber})",
                                        modifier = Modifier
                                            .size(164.dp)
                                            .testTag("id_card_qr_image")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = null,
                                            tint = IslamicGreenPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SCAN QR FOR QUICK RECORD RETRIEVAL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = IslamicGreenPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Text(
                                        text = "${student.gender} • Admitted ${student.dateOfAdmission}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF5D6D66)
                                    )
                                }
                            }
                        }

                        // Card Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IslamicGreenPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"${schoolInfo.motto}\"",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Text(
                                text = schoolInfo.phoneNumber,
                                color = IslamicGoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Print, Share, Copy ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            StudentIdCardGenerator.printStudentIdCard(context, student, schoolInfo)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("id_card_print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Card", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val file = StudentIdCardGenerator.saveIdCardPngToCache(context, student, schoolInfo)
                            if (file != null) {
                                StudentIdCardGenerator.shareIdCardImage(context, file, student)
                            } else {
                                onNotify("Unable to generate ID card image.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("id_card_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF281E04),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Share PNG",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF281E04)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("Student QR ID", qrPayload)
                        )
                        onNotify("Copied Student ID & QR code: ${student.registrationNumber}")
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("id_card_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = IslamicGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copy Unique Student ID (${student.registrationNumber})",
                        color = IslamicGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
