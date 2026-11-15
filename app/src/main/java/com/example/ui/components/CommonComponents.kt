package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.StatusActiveContainer
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusCompletedBlue
import com.example.ui.theme.StatusCompletedContainer
import com.example.ui.theme.StatusSuspendedContainer
import com.example.ui.theme.StatusSuspendedRed
import com.example.ui.theme.StatusTransferredContainer
import com.example.ui.theme.StatusTransferredOrange

@Composable
fun StudentAvatar(
    photoUri: String?,
    fullName: String,
    gender: String,
    size: Dp = 50.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (gender.equals("Female", ignoreCase = true)) Color(0xFFE8D7F1) else Color(0xFFD4EDE5)
            )
            .border(
                1.5.dp,
                if (gender.equals("Female", ignoreCase = true)) Color(0xFF9C27B0) else IslamicGreenPrimary,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Photo of $fullName",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            // Fallback initials or elegant icon
            val initials = fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
            
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                    color = if (gender.equals("Female", ignoreCase = true)) Color(0xFF6A1B9A) else IslamicGreenDark
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = IslamicGreenPrimary,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "active" -> StatusActiveContainer to StatusActiveGreen
        "completed" -> StatusCompletedContainer to StatusCompletedBlue
        "transferred" -> StatusTransferredContainer to StatusTransferredOrange
        "suspended" -> StatusSuspendedContainer to StatusSuspendedRed
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = count,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(IslamicGreenPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String = "Confirm Deletion",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("dialog_confirm_delete_button")
            ) {
                Text("Delete Record", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminLoginDialog(
    teachers: List<com.example.data.model.Teacher> = emptyList(),
    onDismiss: () -> Unit,
    onLogin: (pin: String, role: com.example.data.model.StaffRole, teacher: com.example.data.model.Teacher?) -> Boolean
) {
    var selectedRole by remember { mutableStateOf(com.example.data.model.StaffRole.ADMIN) }
    var selectedTeacher by remember { mutableStateOf<com.example.data.model.Teacher?>(null) }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val matchingTeachers = remember(teachers, selectedRole) {
        teachers.filter { com.example.data.model.StaffRole.fromString(it.role) == selectedRole }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IslamicGreenPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = IslamicGreenPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Staff & Admin Access",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Select your staff role and enter the Admin PIN (1234) or Staff ID.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Role Chips
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    com.example.data.model.StaffRole.allRoles.forEach { role ->
                        val isSel = selectedRole == role
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) IslamicGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedRole = role
                                    selectedTeacher = teachers.firstOrNull {
                                        com.example.data.model.StaffRole.fromString(it.role) == role
                                    }
                                    errorMessage = null
                                }
                                .testTag("login_role_${role.name.lowercase()}")
                        ) {
                            Text(
                                text = role.badgeShort,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                if (selectedRole != com.example.data.model.StaffRole.ADMIN && matchingTeachers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Staff Profile:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    matchingTeachers.take(3).forEach { teacher ->
                        val isPicked = selectedTeacher?.id == teacher.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPicked) IslamicGreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTeacher = teacher }
                        ) {
                            Text(
                                text = "${teacher.fullName} (${teacher.staffId})",
                                fontSize = 11.sp,
                                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPicked) IslamicGreenPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 16) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text(if (selectedRole == com.example.data.model.StaffRole.ADMIN) "Admin PIN" else "PIN or Staff ID") },
                    placeholder = { Text("Default PIN: 1234") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (selectedRole == com.example.data.model.StaffRole.ADMIN) KeyboardType.NumberPassword else KeyboardType.Text
                    ),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(selectedRole.description, fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.isBlank()) {
                        errorMessage = "Please enter PIN or Staff ID"
                    } else {
                        val success = onLogin(pin, selectedRole, selectedTeacher)
                        if (!success) {
                            errorMessage = "Incorrect credentials. Default PIN is 1234"
                        } else {
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("admin_login_submit_button")
            ) {
                Text("Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
