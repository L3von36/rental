package com.loypos.desktop.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

sealed class ParsedDesktopQrCode {
    data class UnitTag(val itemId: String?, val unitCode: String) : ParsedDesktopQrCode()
    data class EquipmentTag(val itemId: String?, val skuOrCode: String) : ParsedDesktopQrCode()
    data class RawCode(val code: String) : ParsedDesktopQrCode()
}

object DesktopQrCodeUtils {
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

    fun parsePayload(raw: String): ParsedDesktopQrCode {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith(UNIT_PREFIX, ignoreCase = true) -> {
                val rest = trimmed.substring(UNIT_PREFIX.length)
                val firstColon = rest.indexOf(':')
                if (firstColon > 0) {
                    ParsedDesktopQrCode.UnitTag(
                        itemId = rest.substring(0, firstColon).trim().takeIf { it.isNotEmpty() },
                        unitCode = rest.substring(firstColon + 1).trim(),
                    )
                } else {
                    ParsedDesktopQrCode.UnitTag(itemId = null, unitCode = rest.trim())
                }
            }
            trimmed.startsWith(EQ_PREFIX, ignoreCase = true) -> {
                val rest = trimmed.substring(EQ_PREFIX.length)
                val firstColon = rest.indexOf(':')
                if (firstColon > 0) {
                    ParsedDesktopQrCode.EquipmentTag(
                        itemId = rest.substring(0, firstColon).trim().takeIf { it.isNotEmpty() },
                        skuOrCode = rest.substring(firstColon + 1).trim(),
                    )
                } else {
                    ParsedDesktopQrCode.EquipmentTag(itemId = null, skuOrCode = rest.trim())
                }
            }
            else -> ParsedDesktopQrCode.RawCode(trimmed)
        }
    }

    fun encodeQrBufferedImage(content: String, sizePx: Int = 420): BufferedImage? {
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
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val dark = Color(17, 24, 39).rgb
            val light = Color.WHITE.rgb
            for (y in 0 until h) {
                for (x in 0 until w) {
                    img.setRGB(x, y, if (matrix.get(x, y)) dark else light)
                }
            }
            img
        }.getOrNull()
    }

    fun encodeQrImageBitmap(content: String, sizePx: Int = 420): ImageBitmap? =
        encodeQrBufferedImage(content, sizePx)?.toComposeImageBitmap()

    fun decodeQrFromFile(imageFile: File): String? {
        return runCatching {
            val img = ImageIO.read(imageFile) ?: return null
            val w = img.width
            val h = img.height
            val pixels = IntArray(w * h)
            img.getRGB(0, 0, w, h, pixels, 0, w)
            val source = RGBLuminanceSource(w, h, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        }.getOrNull()
    }

    fun saveStickerPng(
        targetFile: File,
        storeName: String,
        itemName: String,
        codeLabel: String,
        subtitle: String,
        qrPayload: String,
    ): File {
        val width = 640
        val height = 760
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        g.color = Color(209, 213, 219)
        g.drawRoundRect(16, 16, width - 32, height - 32, 24, 24)

        g.color = Color(22, 163, 74)
        g.font = Font("SansSerif", Font.BOLD, 22)
        val headerFm = g.fontMetrics
        val upperStore = storeName.uppercase()
        g.drawString(upperStore, (width - headerFm.stringWidth(upperStore)) / 2, 64)

        g.color = Color(17, 24, 39)
        g.font = Font("SansSerif", Font.BOLD, 28)
        val titleFm = g.fontMetrics
        val cleanTitle = if (itemName.length > 30) itemName.take(28) + "..." else itemName
        g.drawString(cleanTitle, (width - titleFm.stringWidth(cleanTitle)) / 2, 106)

        val qrImg = encodeQrBufferedImage(qrPayload, 460)
        if (qrImg != null) {
            g.drawImage(qrImg, (width - 460) / 2, 132, null)
        }

        g.color = Color(17, 24, 39)
        g.font = Font("Monospaced", Font.BOLD, 30)
        val codeFm = g.fontMetrics
        g.drawString(codeLabel, (width - codeFm.stringWidth(codeLabel)) / 2, 646)

        g.color = Color(107, 114, 128)
        g.font = Font("SansSerif", Font.PLAIN, 18)
        val subFm = g.fontMetrics
        g.drawString(subtitle, (width - subFm.stringWidth(subtitle)) / 2, 688)

        g.dispose()
        targetFile.parentFile?.mkdirs()
        ImageIO.write(img, "png", targetFile)
        return targetFile
    }
}
