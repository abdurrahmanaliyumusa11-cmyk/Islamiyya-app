package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Student
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.util.StudentIdCardGenerator
import java.util.concurrent.Executors

@Composable
fun QrStudentLookupDialog(
    findStudentByPayload: (String) -> Student?,
    onStudentRetrieved: (Student) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputQuery by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showCameraScanner by remember { mutableStateOf(false) }

    val matchedStudent = remember(inputQuery) {
        if (inputQuery.isBlank()) null else findStudentByPayload(inputQuery)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraScanner = true
            statusMessage = null
        } else {
            statusMessage = "Camera permission denied. You can still scan from an ID card image or type the Student ID."
        }
    }

    val qrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp ->
                        StudentIdCardGenerator.decodeQrFromBitmap(bmp)
                    }
                }
            }.getOrNull()

            if (!decoded.isNullOrBlank()) {
                inputQuery = decoded
                val student = findStudentByPayload(decoded)
                if (student != null) {
                    statusMessage = "QR Verified: ${student.fullName}"
                    onStudentRetrieved(student)
                } else {
                    statusMessage = "QR code read ($decoded), but no matching student was found."
                }
            } else {
                statusMessage = "Could not detect a valid student QR code in the selected image."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("qr_lookup_dialog"),
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Student ID & QR Retrieval",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("qr_lookup_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close QR Lookup")
                    }
                }

                Text(
                    text = "Scan a student's Digital ID QR code with the camera, pick an ID image, or enter their registration number.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Camera Scanner Toggle / Photo Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (showCameraScanner) {
                                showCameraScanner = false
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    showCameraScanner = true
                                    statusMessage = null
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_live_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showCameraScanner) "Stop Camera" else "Scan Camera",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            qrImagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_pick_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageSearch,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "From Image",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = IslamicGreenPrimary
                        )
                    }
                }

                // Live CameraX Preview Box
                if (showCameraScanner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    QrCameraPreviewBox(
                        onQrDecoded = { payload ->
                            inputQuery = payload
                            val student = findStudentByPayload(payload)
                            if (student != null) {
                                showCameraScanner = false
                                onStudentRetrieved(student)
                            } else {
                                statusMessage = "Scanned code '$payload', no student record found."
                            }
                        },
                        onCameraError = { err ->
                            showCameraScanner = false
                            statusMessage = err
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual ID / QR Payload Input
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = {
                        inputQuery = it
                        statusMessage = null
                    },
                    label = { Text("Student ID, Reg No, or QR Payload") },
                    placeholder = { Text("e.g. UIPS/2025/001 or UIPS_STUDENT|1|...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = IslamicGreenPrimary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_lookup_text_input")
                )

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (matchedStudent != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_matched_student_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = IslamicGreenPrimary.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudentAvatar(
                                photoUri = matchedStudent.photoUri,
                                fullName = matchedStudent.fullName,
                                gender = matchedStudent.gender,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = matchedStudent.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${matchedStudent.className} • ${matchedStudent.registrationNumber}",
                                    fontSize = 12.sp,
                                    color = IslamicGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onStudentRetrieved(matchedStudent) },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("qr_open_matched_student_button")
                    ) {
                        Text("Open Student Record", fontWeight = FontWeight.Bold)
                    }
                } else if (inputQuery.isNotBlank() && statusMessage == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No matching student record found for '$inputQuery'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCameraPreviewBox(
    onQrDecoded: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(2.dp, IslamicGoldAccent, RoundedCornerShape(14.dp))
            .testTag("qr_camera_preview_box"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        @Suppress("DEPRECATION")
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(960, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        var handled = false
                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            try {
                                if (!handled) {
                                    val yBuffer = imageProxy.planes[0].buffer
                                    val yBytes = ByteArray(yBuffer.remaining())
                                    yBuffer.get(yBytes)
                                    val decoded = StudentIdCardGenerator.decodeQrFromYuvBytes(
                                        yData = yBytes,
                                        width = imageProxy.width,
                                        height = imageProxy.height
                                    )
                                    if (!decoded.isNullOrBlank()) {
                                        handled = true
                                        mainExecutor.execute { onQrDecoded(decoded) }
                                    }
                                }
                            } catch (_: Exception) {
                                // Ignore individual frame read errors
                            } finally {
                                imageProxy.close()
                            }
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        mainExecutor.execute {
                            onCameraError("Camera unavailable on this device. Use 'From Image' or enter ID below.")
                        }
                    }
                }, mainExecutor)
                previewView
            }
        )

        // Targeting reticle overlay
        Box(
            modifier = Modifier
                .size(140.dp)
                .border(2.dp, IslamicGoldAccent.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
        )
    }
}
