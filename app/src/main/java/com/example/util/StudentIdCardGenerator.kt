package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.SchoolInfo
import com.example.data.model.Student
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedQrPayload(
    val studentId: Long?,
    val registrationNumber: String?,
    val rawText: String
)

object StudentIdCardGenerator {

    private const val QR_PREFIX = "UIPS_STUDENT|"

    /**
     * Builds the canonical QR payload containing the unique student ID and registration number.
     */
    fun encodeStudentQrPayload(student: Student): String {
        val regNo = student.registrationNumber.trim()
        return "${QR_PREFIX}${student.id}|$regNo"
    }

    /**
     * Parses a scanned or typed QR payload / student ID / registration number.
     */
    fun parseStudentQrPayload(payload: String): ParsedQrPayload {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) {
            return ParsedQrPayload(studentId = null, registrationNumber = null, rawText = "")
        }

        if (trimmed.startsWith(QR_PREFIX, ignoreCase = true)) {
            val parts = trimmed.split("|")
            val parsedId = parts.getOrNull(1)?.trim()?.toLongOrNull()?.takeIf { it > 0L }
            val parsedReg = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
            return ParsedQrPayload(
                studentId = parsedId,
                registrationNumber = parsedReg,
                rawText = trimmed
            )
        }

        // Support bare numeric student database ID or bare registration number
        val numericId = trimmed.removePrefix("#").trim().toLongOrNull()?.takeIf { it > 0L }
        return if (numericId != null) {
            ParsedQrPayload(studentId = numericId, registrationNumber = null, rawText = trimmed)
        } else {
            ParsedQrPayload(studentId = null, registrationNumber = trimmed, rawText = trimmed)
        }
    }

    /**
     * Generates a crisp ISO/IEC 18004 QR code Bitmap for the given content.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = 0xFF022B22.toInt(),
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val safeContent = content.ifBlank { "UIPS_STUDENT|0|UNREGISTERED" }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(safeContent, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /**
     * Decodes a QR code string from an Android Bitmap (e.g. from Photo Picker or saved ID card).
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val scaled = if (bitmap.width > 1200 || bitmap.height > 1200) {
            val ratio = minOf(1200f / bitmap.width, 1200f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val reader = QRCodeReader()

        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: Exception) {
            try {
                reader.reset()
                reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints).text
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Decodes a QR code directly from a CameraX YUV_420_888 Y-plane buffer.
     */
    fun decodeQrFromYuvBytes(yData: ByteArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || yData.size < width * height) return null
        val source = PlanarYUVLuminanceSource(
            yData,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val reader = QRCodeReader()
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders a high-resolution (1080 x 660) Digital Student ID Card PNG Bitmap.
     */
    fun renderIdCardBitmap(
        context: Context,
        student: Student,
        schoolInfo: SchoolInfo
    ): Bitmap {
        val width = 1080
        val height = 660
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer light-cream card surface
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBFDFC.toInt()
        }
        val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(cardRect, 28f, 28f, bgPaint)

        // Subtle watermark border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0xFF0B5345.toInt()
        }
        canvas.drawRoundRect(RectF(3f, 3f, width - 3f, height - 3f), 26f, 26f, borderPaint)

        // Header banner gradient
        val headerHeight = 148f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), headerHeight,
                0xFF0B5345.toInt(), 0xFF022B22.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        val headerPath = Path().apply {
            addRoundRect(
                RectF(0f, 0f, width.toFloat(), headerHeight),
                floatArrayOf(28f, 28f, 28f, 28f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(headerPath, headerPaint)

        // Gold accent stripe below header
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC5A059.toInt()
        }
        canvas.drawRect(0f, headerHeight, width.toFloat(), headerHeight + 10f, goldPaint)

        // School logo in header
        val logoRadius = 46f
        val logoCx = 76f
        val logoCy = headerHeight / 2f
        val logoCircleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(logoCx, logoCy, logoRadius + 4f, goldPaint)
        canvas.drawCircle(logoCx, logoCy, logoRadius, logoCircleBg)

        val schoolLogoBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_logo)
        }.getOrNull()
        if (schoolLogoBitmap != null) {
            canvas.save()
            val clipPath = Path().apply {
                addCircle(logoCx, logoCy, logoRadius - 2f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val dst = RectF(
                logoCx - logoRadius,
                logoCy - logoRadius,
                logoCx + logoRadius,
                logoCy + logoRadius
            )
            canvas.drawBitmap(schoolLogoBitmap, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        }

        // School title and subtitle
        val schoolTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(schoolInfo.name, 142f, 64f, schoolTitlePaint)

        val schoolSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD4EDE5.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(schoolInfo.location, 142f, 94f, schoolSubPaint)

        val cardTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC5A059.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("OFFICIAL DIGITAL STUDENT IDENTITY CARD", 142f, 126f, cardTagPaint)

        // Left Section: Student Photo or Initials
        val photoCx = 148f
        val photoCy = 326f
        val photoRadius = 96f
        val isFemale = student.gender.equals("Female", ignoreCase = true)
        val avatarRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isFemale) 0xFF9C27B0.toInt() else 0xFF0B5345.toInt()
        }
        val avatarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isFemale) 0xFFE8D7F1.toInt() else 0xFFD4EDE5.toInt()
        }
        canvas.drawCircle(photoCx, photoCy, photoRadius + 5f, avatarRingPaint)
        canvas.drawCircle(photoCx, photoCy, photoRadius, avatarBgPaint)

        val studentPhoto = loadStudentBitmapSafely(context, student.photoUri)
        if (studentPhoto != null) {
            canvas.save()
            val clipPath = Path().apply {
                addCircle(photoCx, photoCy, photoRadius - 2f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val dst = RectF(
                photoCx - photoRadius,
                photoCy - photoRadius,
                photoCx + photoRadius,
                photoCy + photoRadius
            )
            canvas.drawBitmap(studentPhoto, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val initials = student.fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { "ST" }
            val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isFemale) 0xFF6A1B9A.toInt() else 0xFF022B22.toInt()
                textSize = 68f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val bounds = Rect()
            initialsPaint.getTextBounds(initials, 0, initials.length, bounds)
            canvas.drawText(initials, photoCx, photoCy + bounds.height() / 2f, initialsPaint)
        }

        // Status pill beneath photo
        val statusRect = RectF(66f, 444f, 230f, 484f)
        val statusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (student.admissionStatus.equals("Active", ignoreCase = true)) {
                0xFFD4EDE5.toInt()
            } else {
                0xFFFFE5D0.toInt()
            }
        }
        canvas.drawRoundRect(statusRect, 20f, 20f, statusBgPaint)
        val statusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (student.admissionStatus.equals("Active", ignoreCase = true)) {
                0xFF022B22.toInt()
            } else {
                0xFF7A3E00.toInt()
            }
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.admissionStatus.uppercase(Locale.ROOT), photoCx, 471f, statusTextPaint)

        // Center Section: Student Details
        val detailX = 284f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5D6D66.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF022B22.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A2A22.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("STUDENT FULL NAME", detailX, 214f, labelPaint)
        val displayName = truncateText(student.fullName, namePaint, 430f)
        canvas.drawText(displayName, detailX, 254f, namePaint)

        canvas.drawText("CLASS ENROLLED", detailX, 304f, labelPaint)
        val classBadgeRect = RectF(detailX, 316f, detailX + 210f, 358f)
        val classBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B5345.toInt() }
        canvas.drawRoundRect(classBadgeRect, 12f, 12f, classBadgePaint)
        val classBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.className, classBadgeRect.centerX(), 345f, classBadgeTextPaint)

        canvas.drawText("UNIQUE STUDENT ID / REG NO", detailX, 404f, labelPaint)
        val regBoxRect = RectF(detailX, 416f, detailX + 410f, 464f)
        val regBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5EFE0.toInt() }
        val regBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xFFC5A059.toInt()
        }
        canvas.drawRoundRect(regBoxRect, 12f, 12f, regBoxPaint)
        canvas.drawRoundRect(regBoxRect, 12f, 12f, regBorderPaint)

        val regTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF281E04.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val idSuffix = if (student.id > 0L) "  (#${student.id})" else ""
        canvas.drawText("${student.registrationNumber}$idSuffix", detailX + 16f, 448f, regTextPaint)

        canvas.drawText("GENDER & ADMISSION DATE", detailX, 508f, labelPaint)
        canvas.drawText("${student.gender}  •  Admitted: ${student.dateOfAdmission}", detailX, 540f, valuePaint)

        // Right Section: Crisp QR Code Box
        val qrSize = 264
        val qrPayload = encodeStudentQrPayload(student)
        val qrBitmap = generateQrBitmap(qrPayload, qrSize)

        val qrBoxRect = RectF(738f, 200f, 1034f, 542f)
        val qrContainerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0xFFC5A059.toInt()
        }
        canvas.drawRoundRect(qrBoxRect, 18f, 18f, qrContainerPaint)
        canvas.drawRoundRect(qrBoxRect, 18f, 18f, qrBorderPaint)

        canvas.drawBitmap(qrBitmap, 754f, 216f, null)

        val qrCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B5345.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCAN QR FOR RECORD", qrBoxRect.centerX(), 508f, qrCaptionPaint)
        val qrSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5D6D66.toInt()
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.registrationNumber, qrBoxRect.centerX(), 528f, qrSubPaint)

        // Footer Strip
        val footerTop = 588f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B5345.toInt() }
        val footerPath = Path().apply {
            addRoundRect(
                RectF(0f, footerTop, width.toFloat(), height.toFloat()),
                floatArrayOf(0f, 0f, 0f, 0f, 28f, 28f, 28f, 28f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(footerPath, footerPaint)

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("\"${schoolInfo.motto}\"", 36f, 631f, footerTextPaint)

        val footerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC5A059.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Tel: ${schoolInfo.phoneNumber}", width - 36f, 631f, footerRightPaint)

        return bitmap
    }

    /**
     * Saves a high-res PNG of the student's digital ID card into cacheDir for sharing via FileProvider.
     */
    fun saveIdCardPngToCache(
        context: Context,
        student: Student,
        schoolInfo: SchoolInfo
    ): File? {
        return try {
            val bitmap = renderIdCardBitmap(context, student, schoolInfo)
            val safeReg = student.registrationNumber.replace('/', '_').replace(' ', '_')
            val file = File(context.cacheDir, "Student_ID_${safeReg}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launches Android share sheet for the generated Digital ID Card PNG via FileProvider.
     */
    fun shareIdCardImage(context: Context, file: File, student: Student): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_SUBJECT, "Digital ID Card - ${student.fullName} (${student.registrationNumber})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Official Student Digital ID Card\n" +
                        "Name: ${student.fullName}\n" +
                        "Class: ${student.className}\n" +
                        "Student ID: ${student.registrationNumber}"
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Digital ID Card"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Opens Android PrintManager to print a crisp front-and-back Student ID Card with embedded QR code.
     */
    fun printStudentIdCard(context: Context, student: Student, schoolInfo: SchoolInfo) {
        val htmlContent = buildPrintableIdCardHtml(context, student, schoolInfo)
        val jobName = "ID_Card_${student.registrationNumber.replace('/', '_')}"
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("id_card", "print", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    fun buildPrintableIdCardHtml(
        context: Context,
        student: Student,
        schoolInfo: SchoolInfo
    ): String {
        val qrBitmap = generateQrBitmap(encodeStudentQrPayload(student), 320)
        val qrBase64 = bitmapToBase64Png(qrBitmap)

        val logoBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_school_logo)
        }.getOrNull()
        val logoTag = if (logoBitmap != null) {
            "<img src=\"data:image/png;base64,${bitmapToBase64Png(logoBitmap)}\" class=\"logo\" alt=\"School Logo\"/>"
        } else {
            ""
        }

        val studentPhoto = loadStudentBitmapSafely(context, student.photoUri)
        val initials = student.fullName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "ST" }
        val photoHtml = if (studentPhoto != null) {
            "<img src=\"data:image/png;base64,${bitmapToBase64Png(studentPhoto)}\" class=\"avatar-img\" alt=\"Student Photo\"/>"
        } else {
            "<div class=\"avatar-initials\">$initials</div>"
        }

        val issuedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val uniqueIdSuffix = if (student.id > 0L) " • ID #${student.id}" else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Helvetica Neue', Arial, sans-serif;
                    background: #f5f7f6;
                    margin: 24px;
                    color: #1a2a22;
                }
                .page-title {
                    text-align: center;
                    font-size: 14px;
                    color: #556;
                    margin-bottom: 18px;
                    letter-spacing: 0.5px;
                }
                .cards-wrapper {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 24px;
                }
                .id-card {
                    width: 540px;
                    border: 2px solid #0B5345;
                    border-radius: 16px;
                    overflow: hidden;
                    background: #ffffff;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                    page-break-inside: avoid;
                }
                .card-header {
                    background: linear-gradient(135deg, #0B5345, #022B22);
                    color: #ffffff;
                    padding: 12px 16px;
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    border-bottom: 4px solid #C5A059;
                }
                .logo {
                    width: 48px;
                    height: 48px;
                    border-radius: 50%;
                    background: #ffffff;
                    border: 2px solid #C5A059;
                    object-fit: cover;
                }
                .school-name {
                    font-size: 14px;
                    font-weight: 800;
                    letter-spacing: 0.3px;
                }
                .school-sub {
                    font-size: 10px;
                    color: #D4EDE5;
                    margin-top: 2px;
                }
                .card-tag {
                    font-size: 10px;
                    color: #C5A059;
                    font-weight: 700;
                    margin-top: 3px;
                    letter-spacing: 0.8px;
                }
                .card-body {
                    padding: 16px;
                    display: flex;
                    align-items: center;
                    gap: 16px;
                }
                .avatar-col {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    width: 105px;
                }
                .avatar-img, .avatar-initials {
                    width: 88px;
                    height: 88px;
                    border-radius: 50%;
                    border: 3px solid #0B5345;
                    object-fit: cover;
                }
                .avatar-initials {
                    background: #D4EDE5;
                    color: #022B22;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 28px;
                    font-weight: 800;
                }
                .status-pill {
                    margin-top: 8px;
                    background: #D4EDE5;
                    color: #022B22;
                    font-size: 10px;
                    font-weight: 700;
                    padding: 3px 10px;
                    border-radius: 10px;
                    text-transform: uppercase;
                }
                .info-col {
                    flex: 1;
                }
                .lbl {
                    font-size: 9px;
                    font-weight: 700;
                    color: #5D6D66;
                    text-transform: uppercase;
                    letter-spacing: 0.6px;
                    margin-top: 6px;
                }
                .student-name {
                    font-size: 16px;
                    font-weight: 800;
                    color: #022B22;
                    margin-top: 2px;
                }
                .class-badge {
                    display: inline-block;
                    background: #0B5345;
                    color: #ffffff;
                    font-size: 11px;
                    font-weight: 700;
                    padding: 3px 10px;
                    border-radius: 6px;
                    margin-top: 3px;
                }
                .reg-box {
                    display: inline-block;
                    background: #F5EFE0;
                    border: 1px solid #C5A059;
                    color: #281E04;
                    font-family: monospace;
                    font-size: 12px;
                    font-weight: 700;
                    padding: 4px 8px;
                    border-radius: 6px;
                    margin-top: 3px;
                }
                .meta-val {
                    font-size: 11px;
                    font-weight: 600;
                    color: #1A2A22;
                    margin-top: 2px;
                }
                .qr-col {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    border: 1.5px solid #C5A059;
                    border-radius: 10px;
                    padding: 8px;
                    background: #ffffff;
                }
                .qr-img {
                    width: 114px;
                    height: 114px;
                }
                .qr-caption {
                    font-size: 9px;
                    font-weight: 800;
                    color: #0B5345;
                    margin-top: 4px;
                }
                .card-footer {
                    background: #0B5345;
                    color: #ffffff;
                    padding: 7px 16px;
                    display: flex;
                    justify-content: space-between;
                    font-size: 10px;
                }
                .back-body {
                    padding: 16px;
                    font-size: 11px;
                    line-height: 1.5;
                }
                .back-row {
                    margin-bottom: 6px;
                    border-bottom: 1px solid #eef2f0;
                    padding-bottom: 4px;
                }
                .cut-guide {
                    font-size: 11px;
                    color: #888;
                    border-top: 1px dashed #aaa;
                    width: 540px;
                    text-align: center;
                    padding-top: 6px;
                }
            </style>
        </head>
        <body>
            <div class="page-title">PRINTABLE STUDENT DIGITAL ID CARD &bull; CUT ALONG BORDER</div>
            <div class="cards-wrapper">
                <!-- FRONT OF ID CARD -->
                <div class="id-card">
                    <div class="card-header">
                        $logoTag
                        <div>
                            <div class="school-name">${escapeHtml(schoolInfo.name)}</div>
                            <div class="school-sub">${escapeHtml(schoolInfo.location)}</div>
                            <div class="card-tag">OFFICIAL DIGITAL STUDENT IDENTITY CARD</div>
                        </div>
                    </div>
                    <div class="card-body">
                        <div class="avatar-col">
                            $photoHtml
                            <div class="status-pill">${escapeHtml(student.admissionStatus)}</div>
                        </div>
                        <div class="info-col">
                            <div class="lbl">Student Name</div>
                            <div class="student-name">${escapeHtml(student.fullName)}</div>

                            <div class="lbl">Class</div>
                            <div class="class-badge">${escapeHtml(student.className)}</div>

                            <div class="lbl">Unique Student ID / Reg No</div>
                            <div class="reg-box">${escapeHtml(student.registrationNumber)}$uniqueIdSuffix</div>

                            <div class="lbl">Gender &bull; Date of Admission</div>
                            <div class="meta-val">${escapeHtml(student.gender)} &bull; ${escapeHtml(student.dateOfAdmission)}</div>
                        </div>
                        <div class="qr-col">
                            <img src="data:image/png;base64,$qrBase64" class="qr-img" alt="Student QR Code"/>
                            <div class="qr-caption">SCAN FOR RECORD</div>
                        </div>
                    </div>
                    <div class="card-footer">
                        <span><em>&ldquo;${escapeHtml(schoolInfo.motto)}&rdquo;</em></span>
                        <span style="color:#C5A059; font-weight:700;">Issued: $issuedDate</span>
                    </div>
                </div>

                <div class="cut-guide">&#9986; Fold or Cut Here for Card Back &#9986;</div>

                <!-- BACK OF ID CARD -->
                <div class="id-card">
                    <div class="card-header">
                        <div>
                            <div class="school-name">STUDENT VERIFICATION &amp; EMERGENCY CONTACT</div>
                            <div class="school-sub">${escapeHtml(schoolInfo.name)}</div>
                        </div>
                    </div>
                    <div class="back-body">
                        <div class="back-row"><strong>Parent / Guardian:</strong> ${escapeHtml(student.parentGuardianName.ifBlank { "On File" })}</div>
                        <div class="back-row"><strong>Emergency Phone:</strong> ${escapeHtml(student.parentGuardianPhone.ifBlank { schoolInfo.phoneNumber })}</div>
                        <div class="back-row"><strong>Date of Birth:</strong> ${escapeHtml(student.dateOfBirth)} (${student.age} Years)</div>
                        <div class="back-row"><strong>School Contact:</strong> ${escapeHtml(schoolInfo.phoneNumber)} &bull; ${escapeHtml(schoolInfo.email)}</div>
                        <p style="font-size: 10px; color: #556; margin-top: 8px;">
                            This card remains the property of ${escapeHtml(schoolInfo.name)}. If found, please return to the School Administration Office at ${escapeHtml(schoolInfo.location)} or scan the QR code for instant record lookup.
                        </p>
                    </div>
                    <div class="card-footer">
                        <span>Authorized School Administration</span>
                        <span style="color:#C5A059; font-weight:700;">${escapeHtml(student.registrationNumber)}</span>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun loadStudentBitmapSafely(context: Context, photoUri: String?): Bitmap? {
        if (photoUri.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(photoUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun bitmapToBase64Png(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 3 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "..."
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
