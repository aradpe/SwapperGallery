package com.swappergallery.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class SaveResult {
    data object Success : SaveResult()
    data class NeedsWriteAccess(val intentSender: IntentSender) : SaveResult()
    data class Failure(val error: Exception) : SaveResult()
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver
) {
    private val backupDir: File
        get() = File(context.filesDir, "backups").also { it.mkdirs() }

    suspend fun createBackup(imageUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.png"
        val backupFile = File(backupDir, fileName)

        val bitmap = loadBitmapFromUri(imageUri)
            ?: throw IllegalStateException("Cannot load image from $imageUri")

        FileOutputStream(backupFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val result = BackupResult(
            fileName = fileName,
            width = bitmap.width,
            height = bitmap.height
        )
        bitmap.recycle() // Free full-res bitmap after saving to disk
        result
    }

    suspend fun loadBackup(fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(backupDir, fileName)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun deleteBackup(fileName: String) = withContext(Dispatchers.IO) {
        File(backupDir, fileName).delete()
    }

    fun hasBackup(fileName: String): Boolean {
        return File(backupDir, fileName).exists()
    }

    suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveBitmapToUri(
        bitmap: Bitmap,
        uri: Uri,
        mimeType: String = "image/jpeg"
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            writeBitmapToUri(bitmap, uri, mimeType)
            updateMediaStoreMetadata(uri)
            SaveResult.Success
        } catch (e: SecurityException) {
            handleSecurityException(e, uri)
        } catch (e: Exception) {
            SaveResult.Failure(e)
        }
    }

    private fun writeBitmapToUri(bitmap: Bitmap, uri: Uri, mimeType: String) {
        contentResolver.openOutputStream(uri, "w")?.use { out ->
            val format = when {
                mimeType.contains("png") -> Bitmap.CompressFormat.PNG
                mimeType.contains("webp") -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                else -> Bitmap.CompressFormat.JPEG
            }
            val quality = if (format == Bitmap.CompressFormat.PNG) 100 else 95
            bitmap.compress(format, quality, out)
        } ?: throw Exception("Could not open output stream for $uri")
    }

    private fun updateMediaStoreMetadata(uri: Uri) {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DATE_MODIFIED,
                System.currentTimeMillis() / 1000
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
        }
        try {
            contentResolver.update(uri, values, null, null)
        } catch (_: Exception) {
            // Best effort — may fail on some devices
        }
    }

    private fun handleSecurityException(e: SecurityException, uri: Uri): SaveResult {
        // On API 29+, writing to files the app didn't create requires user permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Try RecoverableSecurityException first (works on API 29+)
            val recoverable = e as? RecoverableSecurityException
            if (recoverable != null) {
                return SaveResult.NeedsWriteAccess(
                    recoverable.userAction.actionIntent.intentSender
                )
            }
            // Fallback: use createWriteRequest on API 30+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return try {
                    val writeRequest = MediaStore.createWriteRequest(
                        contentResolver, listOf(uri)
                    )
                    SaveResult.NeedsWriteAccess(writeRequest.intentSender)
                } catch (ex: Exception) {
                    SaveResult.Failure(e)
                }
            }
        }
        return SaveResult.Failure(e)
    }

    data class BackupResult(
        val fileName: String,
        val width: Int,
        val height: Int
    )
}
