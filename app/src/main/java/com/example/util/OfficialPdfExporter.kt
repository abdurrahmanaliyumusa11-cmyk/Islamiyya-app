package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.FeePayment
import com.example.data.model.SchoolInfo
import com.example.data.model.Student
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android PDF Export Library for UMURIDDIN ISLAMIYYA PRIMARY SCHOOL.
 * Uses Android's built-in PdfDocument vector/canvas engine to render official A4 PDFs
 * with full Nigerian Naira (₦) Unicode glyph support, bundled school crest logo,
 * and ZXing verification QR codes — with zero storage permissions required.
 */
object OfficialPdfExporter {

    private const val PAGE_WIDTH = 595   // A4 width in PostScript points (72 DPI)
    private const val PAGE_HEIGHT = 842  // A4 height in PostScript points (72 DPI)
    private const val MARGIN = 36f

    private val COLOR_GREEN = Color.rgb(11, 83, 69)       // #0B5345
    private val COLOR_GREEN_DARK = Color.rgb(6, 51, 42)   // #06332A
    private val COLOR_GOLD = Color.rgb(197, 160, 89)      // #C5A059
    private val COLOR_SOFT_MINT = Color.rgb(240, 247, 243)
    private val COLOR_ALT_ROW = Color.rgb(249, 251, 250)
    private val COLOR_BORDER = Color.rgb(208, 226, 216)
    private val COLOR_TEXT_DARK = Color.rgb(22, 31, 26)
    private val COLOR_TEXT_MUTED = Color.rgb(83, 98, 89)
    private val COLOR_STATUS_PAID = Color.rgb(25, 135, 84)
    private val COLOR_STATUS_OVERDUE = Color.rgb(220, 53, 69)
    private val COLOR_STATUS_WARN = Color.rgb(179, 83, 0)

    fun buildReceiptPdfFileName(student: Student, payment: FeePayment): String {
        val safeReceipt = payment.receiptNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val safeName = student.fullName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(28)
        return "Receipt_${safeReceipt}_${safeName}.pdf"
    }

    /**
     * Generates an official A4 PDF fee payment receipt in app cache directory.
     * Uses Android's native PdfDocument Skia canvas on devices, with a pure-Kotlin %PDF-1.4
     * stream fallback if native PdfDocument is unavailable in headless/test environments.
     */
    fun generateFeeReceiptPdf(
        context: Context,
        student: Student,
        payment: FeePayment,
        summary: StudentFeeSummary,
        schoolInfo: SchoolInfo
    ): File? {
        val outputDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val outputFile = File(outputDir, buildReceiptPdfFileName(student, payment))

        val nativeOk = runCatching {
            val pdfDocument = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                drawFeeReceiptPage(
                    context = context,
                    canvas = page.canvas,
                    student = student,
                    payment = payment,
                    summary = summary,
                    schoolInfo = schoolInfo
                )
                pdfDocument.finishPage(page)
                FileOutputStream(outputFile).use { fos ->
                    pdfDocument.writeTo(fos)
                }
            } finally {
                runCatching { pdfDocument.close() }
            }
            outputFile.exists() && outputFile.length() > 0L
        }.getOrDefault(false)

        if (nativeOk) return outputFile

        return try {
            val lines = buildFallbackReceiptLines(student, payment, summary, schoolInfo)
            writeMinimalPdf14File(
                outputFile = outputFile,
                title = schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" },
                subtitle = "OFFICIAL FEE PAYMENT RECEIPT - ${payment.receiptNumber}",
                bodyLines = lines
            )
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a paginated A4 PDF tuition ledger report for a list of student summaries.
     */
    fun generateFeeSummaryReportPdf(
        context: Context,
        summaries: List<StudentFeeSummary>,
        schoolInfo: SchoolInfo,
        reportTitle: String
    ): File? {
        val safeTitle = reportTitle.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(outputDir, "${safeTitle}_${timestamp}.pdf")

        val nativeOk = runCatching {
            val pdfDocument = PdfDocument()
            try {
                val rowsPerPage = 18
                val totalPages = ((summaries.size - 1).coerceAtLeast(0) / rowsPerPage) + 1
                val generatedAt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())

                val totalExpected = summaries.sumOf { it.totalDue }
                val totalCollected = summaries.sumOf { it.totalPaid }
                val totalBalance = summaries.sumOf { it.balance }

                for (pageIndex in 0 until totalPages) {
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    var y = 32f
                    y = drawCompactSchoolBanner(canvas, schoolInfo, reportTitle, generatedAt, pageIndex + 1, totalPages, y)

                    if (pageIndex == 0) {
                        y = drawLedgerTotalsStrip(canvas, summaries.size, totalExpected, totalCollected, totalBalance, y)
                    }

                    val sliceStart = pageIndex * rowsPerPage
                    val sliceEnd = minOf(sliceStart + rowsPerPage, summaries.size)
                    val pageRows = if (summaries.isEmpty()) emptyList() else summaries.subList(sliceStart, sliceEnd)

                    drawLedgerTable(canvas, pageRows, sliceStart, y)
                    pdfDocument.finishPage(page)
                }

                FileOutputStream(outputFile).use { fos ->
                    pdfDocument.writeTo(fos)
                }
            } finally {
                runCatching { pdfDocument.close() }
            }
            outputFile.exists() && outputFile.length() > 0L
        }.getOrDefault(false)

        if (nativeOk) return outputFile

        return try {
            val totalExpected = summaries.sumOf { it.totalDue }
            val totalCollected = summaries.sumOf { it.totalPaid }
            val totalBalance = summaries.sumOf { it.balance }
            val lines = mutableListOf(
                "Accounts: ${summaries.size}",
                "Expected: ${FeeHelper.formatNaira(totalExpected).replace("₦", "NGN ")}",
                "Collected: ${FeeHelper.formatNaira(totalCollected).replace("₦", "NGN ")}",
                "Outstanding Balance: ${FeeHelper.formatNaira(totalBalance).replace("₦", "NGN ")}",
                "----------------------------------------------------------------"
            )
            summaries.take(30).forEachIndexed { idx, s ->
                val line = "${idx + 1}. ${s.student.fullName} (${s.student.className}) | " +
                    "Paid: NGN ${s.totalPaid} | Bal: NGN ${s.balance} | ${s.status.label}"
                lines.add(line)
            }
            writeMinimalPdf14File(
                outputFile = outputFile,
                title = schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" },
                subtitle = reportTitle,
                bodyLines = lines
            )
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies a generated PDF file into any user-selected Storage Access Framework Uri (CreateDocument).
     */
    fun writePdfFileToUri(context: Context, pdfFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                pdfFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves the generated PDF directly into public Downloads/Umuriddin_Receipts on Android 10+ (API 29+)
     * with zero storage permissions required. Returns null on API < 29 so caller can use SAF CreateDocument.
     */
    fun savePdfToDownloadsMediaStore(context: Context, pdfFile: File): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, pdfFile.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/Umuriddin_Receipts"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                pdfFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openPdfFile(context: Context, pdfFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF Receipt"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File, subject: String): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, subject))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawFeeReceiptPage(
        context: Context,
        canvas: Canvas,
        student: Student,
        payment: FeePayment,
        summary: StudentFeeSummary,
        schoolInfo: SchoolInfo
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        // Outer Official Receipt Frame
        val outerRect = RectF(24f, 24f, PAGE_WIDTH - 24f, PAGE_HEIGHT - 24f)
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(outerRect, 12f, 12f, fillPaint)
        strokePaint.color = COLOR_GREEN
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(outerRect, 12f, 12f, strokePaint)

        // Header Band
        val headerRect = RectF(24f, 24f, PAGE_WIDTH - 24f, 136f)
        fillPaint.color = COLOR_GREEN
        canvas.drawRoundRect(headerRect, 12f, 12f, fillPaint)
        // Square off bottom corners of header
        canvas.drawRect(24f, 110f, PAGE_WIDTH - 24f, 136f, fillPaint)

        // Gold Accent Bottom Rule on Header
        fillPaint.color = COLOR_GOLD
        canvas.drawRect(24f, 136f, PAGE_WIDTH - 24f, 141f, fillPaint)

        // School Logo Crest
        val logoBitmap = loadRoundedLogoBitmap(context, 62)
        if (logoBitmap != null) {
            val logoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawCircle(72f, 80f, 34f, logoBgPaint)
            canvas.drawBitmap(logoBitmap, 41f, 49f, null)
            strokePaint.color = COLOR_GOLD
            strokePaint.strokeWidth = 2f
            canvas.drawCircle(72f, 80f, 34f, strokePaint)
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 237, 229)
            textSize = 9.5f
        }
        val mottoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val textStartX = 118f
        canvas.drawText(
            ellipsizeText(schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" }, titlePaint, 430f),
            textStartX,
            56f,
            titlePaint
        )
        canvas.drawText(
            ellipsizeText(schoolInfo.location, subPaint, 430f),
            textStartX,
            72f,
            subPaint
        )
        canvas.drawText(
            ellipsizeText("Tel: ${schoolInfo.phoneNumber}  •  Email: ${schoolInfo.email}", subPaint, 430f),
            textStartX,
            86f,
            subPaint
        )
        canvas.drawText(
            ellipsizeText("\"${schoolInfo.motto}\"", mottoPaint, 260f),
            textStartX,
            104f,
            mottoPaint
        )

        // Gold Receipt Pill Badge
        val badgeRect = RectF(PAGE_WIDTH - 228f, 102f, PAGE_WIDTH - 38f, 124f)
        fillPaint.color = COLOR_GOLD
        canvas.drawRoundRect(badgeRect, 11f, 11f, fillPaint)
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 30, 4)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OFFICIAL FEE PAYMENT RECEIPT", badgeRect.centerX(), badgeRect.centerY() + 3.5f, badgeTextPaint)

        // Metadata Strip
        var y = 160f
        val metaBg = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 32f)
        fillPaint.color = COLOR_SOFT_MINT
        canvas.drawRoundRect(metaBg, 6f, 6f, fillPaint)
        strokePaint.color = COLOR_BORDER
        strokePaint.strokeWidth = 1f
        canvas.drawRoundRect(metaBg, 6f, 6f, strokePaint)

        val metaBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_DARK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Receipt No: ${payment.receiptNumber}", MARGIN + 12f, y + 20f, metaBold)
        val datePaint = TextPaint(metaBold).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Payment Date: ${payment.paymentDate}", PAGE_WIDTH - MARGIN - 12f, y + 20f, datePaint)

        y += 46f

        // Student & Payment Details Table
        val detailsRows = mutableListOf(
            "Student Full Name" to student.fullName,
            "Registration No" to student.registrationNumber,
            "Class / Term" to "${student.className}  •  ${payment.termLabel}",
            "Parent / Guardian" to student.parentGuardianName.ifBlank { "N/A" },
            "Parent Contact" to student.parentGuardianPhone.ifBlank { "N/A" },
            "Payment Method" to payment.paymentMethod,
            "Recorded By" to payment.recordedBy
        )
        if (payment.remarks.isNotBlank()) {
            detailsRows.add("Remarks" to payment.remarks)
        }

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val rowHeight = 23f
        detailsRows.forEachIndexed { idx, (label, value) ->
            val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight)
            if (idx % 2 == 0) {
                fillPaint.color = COLOR_ALT_ROW
                canvas.drawRect(rowRect, fillPaint)
            }
            strokePaint.color = Color.rgb(232, 238, 235)
            strokePaint.strokeWidth = 0.8f
            canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, strokePaint)

            canvas.drawText(label, MARGIN + 10f, y + 15f, labelPaint)
            canvas.drawText(
                ellipsizeText(value, valuePaint, PAGE_WIDTH - MARGIN - 190f),
                MARGIN + 155f,
                y + 15f,
                valuePaint
            )
            y += rowHeight
        }

        y += 14f

        // Highlighted Naira Amount Box
        val amountBox = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 68f)
        fillPaint.color = COLOR_SOFT_MINT
        canvas.drawRoundRect(amountBox, 10f, 10f, fillPaint)
        strokePaint.color = COLOR_GREEN
        strokePaint.strokeWidth = 1.6f
        canvas.drawRoundRect(amountBox, 10f, 10f, strokePaint)

        val amountLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_DARK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountHeroPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val balanceRightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (summary.balance > 0L) COLOR_STATUS_OVERDUE else COLOR_STATUS_PAID
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subRightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("AMOUNT PAID (THIS RECEIPT)", MARGIN + 14f, y + 21f, amountLabelPaint)
        canvas.drawText(FeeHelper.formatNaira(payment.amountPaid), MARGIN + 14f, y + 48f, amountHeroPaint)

        canvas.drawText(
            "Remaining Balance: ${FeeHelper.formatNaira(summary.balance)}",
            PAGE_WIDTH - MARGIN - 14f,
            y + 23f,
            balanceRightPaint
        )
        canvas.drawText(
            "Total Paid: ${FeeHelper.formatNaira(summary.totalPaid)} / ${FeeHelper.formatNaira(summary.totalDue)}",
            PAGE_WIDTH - MARGIN - 14f,
            y + 40f,
            subRightPaint
        )
        canvas.drawText(
            "Account Status: ${summary.status.label.uppercase(Locale.ROOT)}",
            PAGE_WIDTH - MARGIN - 14f,
            y + 55f,
            subRightPaint
        )

        y += 84f

        // Installment Breakdown Snapshot
        if (summary.installmentBreakdown.isNotEmpty() && y < PAGE_HEIGHT - 220f) {
            val secTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_GREEN
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("TERM INSTALLMENT SCHEDULE", MARGIN, y, secTitlePaint)
            y += 6f

            // Table Header
            val thRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f)
            fillPaint.color = COLOR_GREEN
            canvas.drawRect(thRect, fillPaint)
            val thPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Installment", MARGIN + 8f, y + 13.5f, thPaint)
            canvas.drawText("Due Date", MARGIN + 185f, y + 13.5f, thPaint)
            canvas.drawText("Amount Due", MARGIN + 275f, y + 13.5f, thPaint)
            canvas.drawText("Paid", MARGIN + 365f, y + 13.5f, thPaint)
            canvas.drawText("Status", MARGIN + 445f, y + 13.5f, thPaint)
            y += 20f

            val tdPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_DARK
                textSize = 9f
            }
            summary.installmentBreakdown.take(5).forEachIndexed { idx, alloc ->
                val trRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 19f)
                if (idx % 2 == 1) {
                    fillPaint.color = COLOR_ALT_ROW
                    canvas.drawRect(trRect, fillPaint)
                }
                canvas.drawText(ellipsizeText(alloc.installment.installmentTitle, tdPaint, 170f), MARGIN + 8f, y + 13f, tdPaint)
                canvas.drawText(alloc.installment.dueDate, MARGIN + 185f, y + 13f, tdPaint)
                canvas.drawText(FeeHelper.formatNaira(alloc.installment.amountDue), MARGIN + 275f, y + 13f, tdPaint)
                canvas.drawText(FeeHelper.formatNaira(alloc.paidTowardInstallment), MARGIN + 365f, y + 13f, tdPaint)

                val stColor = when {
                    alloc.isCleared -> COLOR_STATUS_PAID
                    alloc.isOverdue -> COLOR_STATUS_OVERDUE
                    alloc.isDueSoon -> COLOR_STATUS_WARN
                    else -> COLOR_GREEN
                }
                val stLabel = when {
                    alloc.isCleared -> "CLEARED"
                    alloc.isOverdue -> "OVERDUE"
                    alloc.isDueSoon -> "DUE SOON"
                    else -> "UPCOMING"
                }
                val stPaint = TextPaint(tdPaint).apply {
                    color = stColor
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(stLabel, MARGIN + 445f, y + 13f, stPaint)
                y += 19f
            }
        }

        // Verification Footer with QR Code + Signature Line
        val footerTop = PAGE_HEIGHT - 126f
        strokePaint.color = COLOR_BORDER
        strokePaint.strokeWidth = 1f
        canvas.drawLine(MARGIN, footerTop, PAGE_WIDTH - MARGIN, footerTop, strokePaint)

        // Generate Verification QR Bitmap
        val qrPayload = "${StudentIdCardGenerator.encodeStudentQrPayload(student)}|${payment.receiptNumber}|${payment.amountPaid}"
        val qrBitmap = runCatching { StudentIdCardGenerator.generateQrBitmap(qrPayload, 74) }.getOrNull()
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, MARGIN, footerTop + 12f, null)
        }

        val footLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
        }
        val footBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_DARK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val issuedOn = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Official Receipt Verification", MARGIN + 84f, footerTop + 28f, footBoldPaint)
        canvas.drawText("Ref: ${payment.receiptNumber} • ${student.registrationNumber}", MARGIN + 84f, footerTop + 42f, footLabelPaint)
        canvas.drawText("Generated: $issuedOn", MARGIN + 84f, footerTop + 56f, footLabelPaint)
        canvas.drawText("Currency: Nigerian Naira (NGN)", MARGIN + 84f, footerTop + 70f, footLabelPaint)

        // Signature Line
        strokePaint.color = COLOR_TEXT_DARK
        strokePaint.strokeWidth = 1f
        canvas.drawLine(PAGE_WIDTH - MARGIN - 165f, footerTop + 60f, PAGE_WIDTH - MARGIN, footerTop + 60f, strokePaint)
        val sigTextPaint = TextPaint(footLabelPaint).apply {
            textAlign = Paint.Align.CENTER
            color = COLOR_TEXT_DARK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Bursar / Administrator Signature", PAGE_WIDTH - MARGIN - 82.5f, footerTop + 74f, sigTextPaint)
    }

    private fun drawCompactSchoolBanner(
        canvas: Canvas,
        schoolInfo: SchoolInfo,
        reportTitle: String,
        generatedAt: String,
        pageNumber: Int,
        totalPages: Int,
        startY: Float
    ): Float {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bannerRect = RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + 56f)
        fillPaint.color = COLOR_GREEN
        canvas.drawRoundRect(bannerRect, 8f, 8f, fillPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText(schoolInfo.name.ifBlank { "UMURIDDIN ISLAMIYYA PRIMARY SCHOOL" }, MARGIN + 12f, startY + 22f, titlePaint)
        canvas.drawText(reportTitle, MARGIN + 12f, startY + 40f, subPaint)
        canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN - 12f, startY + 22f, rightPaint)
        canvas.drawText(generatedAt, PAGE_WIDTH - MARGIN - 12f, startY + 38f, rightPaint)

        return startY + 68f
    }

    private fun drawLedgerTotalsStrip(
        canvas: Canvas,
        count: Int,
        expected: Long,
        collected: Long,
        balance: Long,
        startY: Float
    ): Float {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SOFT_MINT }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        val rect = RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + 34f)
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Accounts: $count", MARGIN + 10f, startY + 21f, p)
        canvas.drawText("Expected: ${FeeHelper.formatNaira(expected)}", MARGIN + 110f, startY + 21f, p)

        val greenP = TextPaint(p).apply { color = COLOR_STATUS_PAID }
        canvas.drawText("Collected: ${FeeHelper.formatNaira(collected)}", MARGIN + 260f, startY + 21f, greenP)

        val redP = TextPaint(p).apply { color = COLOR_STATUS_OVERDUE }
        canvas.drawText("Balance: ${FeeHelper.formatNaira(balance)}", PAGE_WIDTH - MARGIN - 135f, startY + 21f, redP)

        return startY + 44f
    }

    private fun drawLedgerTable(
        canvas: Canvas,
        rows: List<StudentFeeSummary>,
        offset: Int,
        startY: Float
    ) {
        var y = startY
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.color = COLOR_GREEN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, fillPaint)

        val thPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("#", MARGIN + 6f, y + 14.5f, thPaint)
        canvas.drawText("Student Name", MARGIN + 26f, y + 14.5f, thPaint)
        canvas.drawText("Class", MARGIN + 178f, y + 14.5f, thPaint)
        canvas.drawText("Due", MARGIN + 245f, y + 14.5f, thPaint)
        canvas.drawText("Paid", MARGIN + 312f, y + 14.5f, thPaint)
        canvas.drawText("Balance", MARGIN + 382f, y + 14.5f, thPaint)
        canvas.drawText("Status", MARGIN + 455f, y + 14.5f, thPaint)
        y += 22f

        val tdPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 8.5f
        }
        rows.forEachIndexed { idx, s ->
            if (idx % 2 == 1) {
                fillPaint.color = COLOR_ALT_ROW
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f, fillPaint)
            }
            canvas.drawText("${offset + idx + 1}", MARGIN + 6f, y + 16f, tdPaint)
            canvas.drawText(ellipsizeText(s.student.fullName, tdPaint, 145f), MARGIN + 26f, y + 16f, tdPaint)
            canvas.drawText(ellipsizeText(s.student.className, tdPaint, 62f), MARGIN + 178f, y + 16f, tdPaint)
            canvas.drawText(FeeHelper.formatNaira(s.totalDue), MARGIN + 245f, y + 16f, tdPaint)
            canvas.drawText(FeeHelper.formatNaira(s.totalPaid), MARGIN + 312f, y + 16f, tdPaint)
            canvas.drawText(FeeHelper.formatNaira(s.balance), MARGIN + 382f, y + 16f, tdPaint)

            val stPaint = TextPaint(tdPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = when (s.status) {
                    StudentFeeStatus.PAID -> COLOR_STATUS_PAID
                    StudentFeeStatus.OVERDUE -> COLOR_STATUS_OVERDUE
                    StudentFeeStatus.DUE_SOON, StudentFeeStatus.PARTIAL -> COLOR_STATUS_WARN
                    else -> COLOR_GREEN
                }
            }
            canvas.drawText(s.status.label, MARGIN + 455f, y + 16f, stPaint)
            y += 26f
        }
    }

    private fun loadRoundedLogoBitmap(context: Context, sizePx: Int): Bitmap? {
        return try {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_logo) ?: return null
            val scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
            val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, sizePx, sizePx)
            val path = android.graphics.Path().apply {
                addOval(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(path)
            canvas.drawBitmap(scaled, rect, rect, paint)
            output
        } catch (_: Exception) {
            null
        }
    }

    private fun ellipsizeText(text: String, paint: TextPaint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return if (end <= 1) text.take(1) else text.substring(0, end) + "…"
    }

    private fun buildFallbackReceiptLines(
        student: Student,
        payment: FeePayment,
        summary: StudentFeeSummary,
        schoolInfo: SchoolInfo
    ): List<String> {
        fun nairaAscii(v: Long) = FeeHelper.formatNaira(v).replace("₦", "NGN ")
        val lines = mutableListOf(
            "Location: ${schoolInfo.location}",
            "Receipt No: ${payment.receiptNumber}   |   Payment Date: ${payment.paymentDate}",
            "----------------------------------------------------------------",
            "Student Name: ${student.fullName}",
            "Registration No: ${student.registrationNumber}",
            "Class & Term: ${student.className} - ${payment.termLabel}",
            "Parent / Guardian: ${student.parentGuardianName.ifBlank { "N/A" }} (${student.parentGuardianPhone.ifBlank { "N/A" }})",
            "Payment Method: ${payment.paymentMethod}   |   Recorded By: ${payment.recordedBy}",
            "Amount Paid (This Receipt): ${nairaAscii(payment.amountPaid)}",
            "Total Paid / Term Tuition: ${nairaAscii(summary.totalPaid)} / ${nairaAscii(summary.totalDue)}",
            "Remaining Balance: ${nairaAscii(summary.balance)} (${summary.status.label})"
        )
        if (payment.remarks.isNotBlank()) {
            lines.add("Remarks: ${payment.remarks}")
        }
        if (summary.installmentBreakdown.isNotEmpty()) {
            lines.add("----------------------------------------------------------------")
            lines.add("TERM INSTALLMENT SCHEDULE:")
            summary.installmentBreakdown.forEach { alloc ->
                lines.add(
                    "- ${alloc.installment.installmentTitle} | Due ${alloc.installment.dueDate} | " +
                        "${nairaAscii(alloc.paidTowardInstallment)} / ${nairaAscii(alloc.installment.amountDue)}"
                )
            }
        }
        return lines
    }

    /**
     * Standards-compliant pure-Kotlin %PDF-1.4 document writer with valid xref offsets.
     */
    private fun writeMinimalPdf14File(
        outputFile: File,
        title: String,
        subtitle: String,
        bodyLines: List<String>
    ) {
        fun escapePdfText(raw: String): String {
            val ascii = raw.replace("₦", "NGN ")
                .replace(Regex("[^\\x20-\\x7E]"), " ")
            return ascii.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
        }

        val content = StringBuilder()
        // Top green banner bar
        content.append("0.043 0.325 0.271 rg\n")
        content.append("36 742 523 64 re f\n")
        content.append("0.773 0.627 0.349 rg\n")
        content.append("36 738 523 4 re f\n")

        // Header Text
        content.append("BT /F2 14 Tf 1 1 1 rg 48 784 Td (${escapePdfText(title)}) Tj ET\n")
        content.append("BT /F2 10 Tf 0.95 0.85 0.55 rg 48 760 Td (${escapePdfText(subtitle)}) Tj ET\n")

        // Body Lines
        var y = 706
        content.append("0.09 0.12 0.10 rg\n")
        for (line in bodyLines) {
            if (y < 64) break
            content.append("BT /F1 10 Tf 42 $y Td (${escapePdfText(line)}) Tj ET\n")
            y -= 18
        }

        val streamBytes = content.toString().toByteArray(StandardCharsets.US_ASCII)
        val baos = ByteArrayOutputStream()
        fun writeAscii(s: String) = baos.write(s.toByteArray(StandardCharsets.US_ASCII))

        writeAscii("%PDF-1.4\n")
        val offsets = mutableListOf(0)

        fun startObj(id: Int, body: String) {
            offsets.add(baos.size())
            writeAscii("$id 0 obj\n$body\nendobj\n")
        }

        startObj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        startObj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        startObj(
            3,
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>"
        )
        startObj(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
        startObj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>")

        offsets.add(baos.size())
        writeAscii("6 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n")
        baos.write(streamBytes)
        writeAscii("\nendstream\nendobj\n")

        val xrefOffset = baos.size()
        writeAscii("xref\n0 ${offsets.size}\n")
        writeAscii("0000000000 65535 f \n")
        for (i in 1 until offsets.size) {
            writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        writeAscii("trailer\n<< /Size ${offsets.size} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        FileOutputStream(outputFile).use { it.write(baos.toByteArray()) }
    }
}
