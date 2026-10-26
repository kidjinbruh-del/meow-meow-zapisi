package ru.meowmeow.notes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Фото лежат в files/photos внутри приложения: без интернета, без внешних разрешений,
 * удаление приложения уносит и снимки.
 */
object PhotoStore {
    private const val DIR = "photos"

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun newFile(context: Context, orderId: Long): File {
        val name = "order_${orderId}_${UUID.randomUUID().toString().take(8)}.jpg"
        return File(dir(context), name)
    }

    fun file(context: Context, fileName: String): File = File(dir(context), fileName)

    fun delete(context: Context, fileName: String) {
        runCatching { file(context, fileName).delete() }
    }

    /** Импорт из галереи: копируем в свою папку, чтобы оригинал можно было удалить. */
    fun copyFromUri(context: Context, orderId: Long, uri: Uri): File? {
        val target = newFile(context, orderId)
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
            if (target.length() > 0L) target else {
                target.delete()
                null
            }
        } catch (t: Throwable) {
            target.delete()
            null
        }
    }

    fun thumbnail(context: Context, fileName: String, size: Int): Bitmap? =
        decode(file(context, fileName), size)

    fun full(context: Context, fileName: String): Bitmap? = decode(file(context, fileName), 1600)

    private fun decode(file: File, reqSize: Int): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return applyExifRotation(bitmap, file)
    }

    private fun applyExifRotation(bitmap: Bitmap, file: File): Bitmap {
        val degrees = runCatching {
            when (
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)

        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
