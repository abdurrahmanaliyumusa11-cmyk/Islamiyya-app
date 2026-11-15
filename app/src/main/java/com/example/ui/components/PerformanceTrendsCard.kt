package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentGrade
import com.example.ui.theme.IslamicGoldAccent
import com.example.ui.theme.IslamicGreenDark
import com.example.ui.theme.IslamicGreenPrimary
import kotlin.math.roundToInt

data class TermTrendPoint(
    val termLabel: String,
    val averageScore: Float,
    val sortOrder: Int,
    val subjectCount: Int
)

fun computeTermTrends(
    grades: List<StudentGrade>,
    selectedSubject: String
): List<TermTrendPoint> {
    val filtered = if (selectedSubject == "Overall Avg") {
        grades
    } else {
        grades.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }
    if (filtered.isEmpty()) return emptyList()

    return filtered
        .groupBy { it.termLabel }
        .map { (term, items) ->
            val avg = items.map { it.score.toFloat() }.average().toFloat()
            val minOrder = items.minOfOrNull { it.sortOrder } ?: 0
            TermTrendPoint(
                termLabel = term,
                averageScore = avg,
                sortOrder = minOrder,
                subjectCount = items.size
            )
        }
        .sortedWith(compareBy({ it.sortOrder }, { it.termLabel }))
}

@Composable
fun PerformanceTrendsCard(
    studentId: Long,
    grades: List<StudentGrade>,
    canRecordGrades: Boolean,
    onAddGrade: (StudentGrade) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects = remember(grades) {
        listOf("Overall Avg") + grades.map { it.subject }.distinct().sorted()
    }
    var selectedSubject by remember { mutableStateOf("Overall Avg") }
    if (selectedSubject !in subjects) {
        selectedSubject = "Overall Avg"
    }

    val trendPoints = remember(grades, selectedSubject) {
        computeTermTrends(grades, selectedSubject)
    }

    var selectedTermIndex by remember(trendPoints) {
        mutableIntStateOf((trendPoints.size - 1).coerceAtLeast(0))
    }
    var showAddGradeDialog by remember { mutableStateOf(false) }

    val latestScore = trendPoints.lastOrNull()?.averageScore ?: 0f
    val previousScore = if (trendPoints.size >= 2) trendPoints[trendPoints.size - 2].averageScore else latestScore
    val delta = latestScore - previousScore
    val bestScore = trendPoints.maxOfOrNull { it.averageScore } ?: 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("performance_trends_card"),
        shape = RoundedCornerShape(14.dp),
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IslamicGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Performance Trends",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Performance Trends",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGreenPrimary
                        )
                        Text(
                            text = "Academic grade progression over terms",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (canRecordGrades) {
                    OutlinedButton(
                        onClick = { showAddGradeDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_add_student_grade")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Record Grade",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = IslamicGreenPrimary.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            if (trendPoints.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No term grades recorded yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap 'Add Score' to record term assessments and view progression.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // KPI summary strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrendMetricBox(
                        label = "Latest Term",
                        value = "${latestScore.roundToInt()}%",
                        color = IslamicGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TrendMetricBox(
                        label = "Progression",
                        value = "${if (delta >= 0) "+" else ""}${delta.roundToInt()}%",
                        color = if (delta >= 0) Color(0xFF198754) else Color(0xFFDC3545),
                        icon = if (delta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )
                    TrendMetricBox(
                        label = "Peak Score",
                        value = "${bestScore.roundToInt()}%",
                        color = IslamicGoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subject Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IslamicGreenPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("grade_filter_${subj.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Compose Canvas Bar Chart
                GradeProgressionBarChart(
                    points = trendPoints,
                    selectedIndex = selectedTermIndex.coerceIn(0, trendPoints.lastIndex),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("performance_bar_chart")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Term Selector Pills for detailed subject breakdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    trendPoints.forEachIndexed { idx, point ->
                        val isSel = idx == selectedTermIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) IslamicGreenPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, IslamicGreenPrimary) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTermIndex = idx }
                        ) {
                            Text(
                                text = "${point.termLabel}: ${point.averageScore.roundToInt()}%",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) IslamicGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Detailed Subject Breakdown for Selected Term
                val activeTerm = trendPoints.getOrNull(selectedTermIndex)?.termLabel
                if (activeTerm != null) {
                    val termEntries = grades.filter { it.termLabel == activeTerm }
                    if (termEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$activeTerm Subject Breakdown",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGreenPrimary
                                )
                                termEntries.forEach { entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = entry.subject,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${entry.score}% (${gradeLetter(entry.score)})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = scoreColor(entry.score.toFloat())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Legend
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = IslamicGreenPrimary, label = "Distinction (75%+)")
                    LegendDot(color = IslamicGoldAccent, label = "Pass (50-74%)")
                    LegendDot(color = Color(0xFFDC3545), label = "Needs Support (<50%)")
                }
            }
        }
    }

    if (showAddGradeDialog) {
        AddGradeDialog(
            studentId = studentId,
            onDismiss = { showAddGradeDialog = false },
            onSave = { newGrade ->
                onAddGrade(newGrade)
                showAddGradeDialog = false
            }
        )
    }
}

@Composable
private fun GradeProgressionBarChart(
    points: List<TermTrendPoint>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val passLineColor = Color(0xFFC5A059)

    Canvas(modifier = modifier) {
        val leftPad = 34.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 22.dp.toPx()
        val bottomPad = 28.dp.toPx()

        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        val valuePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = labelTextColor
            isFakeBoldText = true
        }
        val axisPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            color = subTextColor
        }
        val termPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = subTextColor
        }

        // Horizontal Y-axis gridlines: 0, 25, 50, 75, 100
        listOf(0, 25, 50, 75, 100).forEach { tick ->
            val y = topPad + chartHeight * (1f - tick / 100f)
            drawLine(
                color = if (tick == 50) passLineColor.copy(alpha = 0.7f) else axisColor.copy(alpha = 0.25f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartWidth, y),
                strokeWidth = if (tick == 50) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (tick == 50) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$tick",
                leftPad - 6.dp.toPx(),
                y + 3.dp.toPx(),
                axisPaint
            )
        }

        val count = points.size.coerceAtLeast(1)
        val slotWidth = chartWidth / count
        val barWidth = (slotWidth * 0.56f).coerceIn(18.dp.toPx(), 46.dp.toPx())

        val centers = mutableListOf<Offset>()

        points.forEachIndexed { idx, point ->
            val clamped = point.averageScore.coerceIn(0f, 100f)
            val barHeight = chartHeight * (clamped / 100f)
            val centerX = leftPad + slotWidth * idx + slotWidth / 2f
            val barLeft = centerX - barWidth / 2f
            val barTop = topPad + chartHeight - barHeight

            val baseColor = scoreColor(clamped)
            val topColor = if (idx == selectedIndex) baseColor else baseColor.copy(alpha = 0.85f)
            val bottomColor = baseColor.copy(alpha = 0.65f)

            drawRoundRect(
                brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Highlight selected bar outline
            if (idx == selectedIndex) {
                drawRoundRect(
                    color = IslamicGoldAccent,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            centers.add(Offset(centerX, barTop))

            // Score text above bar
            drawContext.canvas.nativeCanvas.drawText(
                "${clamped.roundToInt()}%",
                centerX,
                (barTop - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
                valuePaint
            )

            // Term label below X-axis
            drawContext.canvas.nativeCanvas.drawText(
                point.termLabel,
                centerX,
                topPad + chartHeight + 16.dp.toPx(),
                termPaint
            )
        }

        // Subtle progression line connecting bar tops
        if (centers.size >= 2) {
            for (i in 0 until centers.size - 1) {
                drawLine(
                    color = IslamicGreenDark.copy(alpha = 0.55f),
                    start = centers[i],
                    end = centers[i + 1],
                    strokeWidth = 2.dp.toPx()
                )
            }
            centers.forEach { pt ->
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = IslamicGreenPrimary,
                    radius = 2.8.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun TrendMetricBox(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = value,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = color
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGradeDialog(
    studentId: Long,
    onDismiss: () -> Unit,
    onSave: (StudentGrade) -> Unit
) {
    val presetTerms = listOf("2025 T2", "2025 T3", "2026 T1", "2026 T2", "2026 T3")
    val presetSubjects = listOf(
        "Quran & Tajweed",
        "Arabic Studies",
        "Islamic Studies",
        "Mathematics",
        "English Language",
        "Basic Science"
    )

    var termLabel by remember { mutableStateOf("2026 T2") }
    var subject by remember { mutableStateOf(presetSubjects.first()) }
    var scoreText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var termExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Term Assessment", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                ExposedDropdownMenuBox(
                    expanded = termExpanded,
                    onExpandedChange = { termExpanded = !termExpanded }
                ) {
                    OutlinedTextField(
                        value = termLabel,
                        onValueChange = { termLabel = it },
                        label = { Text("Academic Term") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termExpanded) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = termExpanded,
                        onDismissRequest = { termExpanded = false }
                    ) {
                        presetTerms.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    termLabel = t
                                    termExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        presetSubjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    subject = s
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scoreText,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 3) {
                            scoreText = it
                            error = null
                        }
                    },
                    label = { Text("Score (0 - 100) *") },
                    placeholder = { Text("e.g. 84") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_grade_score")
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Teacher Remarks (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedScore = scoreText.toIntOrNull()
                    when {
                        termLabel.trim().isBlank() -> error = "Enter academic term"
                        subject.trim().isBlank() -> error = "Enter subject name"
                        parsedScore == null || parsedScore !in 0..100 -> error = "Score must be between 0 and 100"
                        else -> {
                            val order = when {
                                termLabel.startsWith("2024 T3") -> 1
                                termLabel.startsWith("2025 T1") -> 2
                                termLabel.startsWith("2025 T2") -> 3
                                termLabel.startsWith("2025 T3") -> 4
                                termLabel.startsWith("2026 T1") -> 5
                                termLabel.startsWith("2026 T2") -> 6
                                termLabel.startsWith("2026 T3") -> 7
                                else -> 8
                            }
                            onSave(
                                StudentGrade(
                                    studentId = studentId,
                                    termLabel = termLabel.trim(),
                                    subject = subject.trim(),
                                    score = parsedScore,
                                    sortOrder = order,
                                    remarks = remarks.trim()
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_grade")
            ) {
                Text("Save Score")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun scoreColor(score: Float): Color {
    return when {
        score >= 75f -> IslamicGreenPrimary
        score >= 50f -> Color(0xFFB8860B)
        else -> Color(0xFFDC3545)
    }
}

private fun gradeLetter(score: Int): String {
    return when {
        score >= 85 -> "A+"
        score >= 75 -> "A"
        score >= 65 -> "B"
        score >= 50 -> "C"
        score >= 40 -> "D"
        else -> "F"
    }
}
