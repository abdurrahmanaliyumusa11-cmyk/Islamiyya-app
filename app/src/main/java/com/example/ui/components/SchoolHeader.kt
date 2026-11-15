package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SchoolInfo
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary

@Composable
fun SchoolHeader(
    schoolInfo: SchoolInfo,
    isAdmin: Boolean,
    roleLabel: String = "Admin Mode",
    onAdminToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("school_header_card"),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            IslamicGreenDark,
                            IslamicGreenPrimary
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row with school badge and admin status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // School crest logo
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, IslamicGoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_school_logo),
                            contentDescription = "School Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    }

                    // Active Role / Auth Pill Toggle
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onAdminToggleClick() }
                            .testTag("admin_status_toggle"),
                        color = if (isAdmin) IslamicGoldAccent else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isAdmin) roleLabel else "Locked",
                                tint = if (isAdmin) Color(0xFF281E04) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdmin) roleLabel else "Unlock Access",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) Color(0xFF281E04) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // School Name
                Text(
                    text = schoolInfo.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    lineHeight = 22.sp
                )

                // Gold Accent Separator
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(60.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(IslamicGoldAccent)
                )

                // School Location
                Text(
                    text = schoolInfo.location,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )

                // School Motto
                Text(
                    text = "• ${schoolInfo.motto} •",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = IslamicGoldAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
