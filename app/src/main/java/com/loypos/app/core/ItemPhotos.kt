package com.loypos.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ItemPhotos {
    private const val DIR_NAME = "item_photos"
    private const val MAX_DIM = 640

    private fun photoDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    suspend fun saveFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not open selected photo")
        saveJpegBytes(context, bytes)
    }

    suspend fun writeBytes(context: Context, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val file = File(photoDir(context), "${newId()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }

    private fun saveJpegBytes(context: Context, rawBytes: ByteArray): String {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: throw IllegalStateException("Unsupported image format")
        val maxSide = max(decoded.width, decoded.height)
        val scaled = if (maxSide > MAX_DIM) {
            val scale = MAX_DIM.toFloat() / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        val file = File(photoDir(context), "${newId()}.jpg")
        file.writeBytes(out.toByteArray())
        return file.absolutePath
    }

    fun readBase64(path: String): String? {
        if (path.isEmpty()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeAndSaveBase64(context: Context, b64: String): String? {
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val file = File(photoDir(context), "${newId()}.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFile(path: String) {
        if (path.isEmpty()) return
        try {
            File(path).delete()
        } catch (_: Exception) {
        }
    }
}
