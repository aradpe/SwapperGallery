package com.swappergallery.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

object FileUtils {
    fun getMimeType(contentResolver: ContentResolver, uri: Uri): String {
        return contentResolver.getType(uri) ?: "image/jpeg"
    }

    fun getDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }
}
