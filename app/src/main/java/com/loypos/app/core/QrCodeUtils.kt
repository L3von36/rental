package com.loypos.app.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

sealed class ParsedQrCode {
    data class UnitTag(val itemId: String?, val unitCode: String) : ParsedQrCode()
    data class EquipmentTag(val itemId: String?, val skuOrCode: String) : ParsedQrCode()
    data class RawCode(val code: String) : ParsedQrCode()
}

object QrCodeUtils {
    private const val UNIT_PREFIX = "LOYPOS:UNIT:"
    private const val EQ_PREFIX = "LOYPOS:EQ:"

    fun buildUnitPayload(itemId: String, unitCode: String): String =
        "$UNIT_PREFIX$itemId:${unitCode.trim()}"

    fun buildEquipmentPayload(itemId: String, skuOrCode: String): String =
        "$EQ_PREFIX$itemId:${skuOrCode.trim()}"

    fun buildDefaultUnitCode(sku: String, itemId: String, indexOneBased: Int): String {
        val base = sku.trim().takeIf { it.isNotEmpty() }
            ?: "EQ-${itemId.take(6).uppercase()}"
        val suffix = indexOneBased.toString().padStart(2, '0')
        return "$base#U$suffix"
    }

    fun parsePayload(raw: String): ParsedQrCode {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith(UNIT_PREFIX, ignoreCase = true) -> {
                val rest = trimmed.substring(UNIT_PREFIX.length)
                val firstColon = rest.indexOf(':')
                if (firstColon > 0) {
                    ParsedQrCode.UnitTag(
                        itemId = rest.substring(0, firstColon).trim().takeIf { it.isNotEmpty() },
                        unitCode = rest.substring(firstColon + 1).trim(),
                    )
                } else {
                    ParsedQrCode.UnitTag(itemId = null, unitCode = rest.trim())
                }
            }
            trimmed.startsWith(EQ_PREFIX, ignoreCase = true) -> {
                val rest = trimmed.substring(EQ_PREFIX.length)
                val firstColon = rest.indexOf(':')
                if (firstColon > 0) {
                    ParsedQrCode.EquipmentTag(
                        itemId = rest.substring(0, firstColon).trim().takeIf { it.isNotEmpty() },
                        skuOrCode = rest.substring(firstColon + 1).trim(),
                    )
                } else {
                    ParsedQrCode.EquipmentTag(itemId = null, skuOrCode = rest.trim())
                }
            }
            else -> ParsedQrCode.RawCode(trimmed)
        }
    }

    fun encodeQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            val dark = Color.rgb(17, 24, 39)
            val white = Color.WHITE
            for (y in 0 until h) {
                val rowOffset = y * w
                for (x in 0 until w) {
                    pixels[rowOffset + x] = if (matrix.get(x, y)) dark else white
                }
            }
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }.getOrNull()
    }

    fun encodeQrImageBitmap(content: String, sizePx: Int = 512): ImageBitmap? =
        encodeQrBitmap(content, sizePx)?.asImageBitmap()

    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return runCatching {
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        }.getOrNull()
    }

    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return null
                decodeQrFromBitmap(bmp)
            }
        }.getOrNull()
    }

    fun createStickerPngFile(
        context: Context,
        storeName: String,
        itemName: String,
        codeLabel: String,
        subtitle: String,
        qrPayload: String,
    ): File {
        val width = 720
        val height = 860
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 213, 219)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(24f, 24f, width - 24f, height - 24f, 28f, 28f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 163, 74)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(storeName.uppercase(), width / 2f, 82f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cleanTitle = if (itemName.length > 28) itemName.take(26) + "..." else itemName
        canvas.drawText(cleanTitle, width / 2f, 134f, titlePaint)

        val qrBmp = encodeQrBitmap(qrPayload, 500)
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, (width - 500) / 2f, 170f, null)
        }

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 40f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(codeLabel, width / 2f, 736f, codePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 114, 128)
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitle, width / 2f, 786f, subPaint)

        val dir = File(context.cacheDir, "qr_labels").apply { mkdirs() }
        val safeName = codeLabel.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "qr-$safeName.png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }

    fun sharePngFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
