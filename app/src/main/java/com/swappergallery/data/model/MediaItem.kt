package com.swappergallery.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val bucketId: Long = 0,
    val bucketName: String = ""
)

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri?,
    val count: Int
)
