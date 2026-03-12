package com.swappergallery.ui.viewer

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swappergallery.data.repository.EditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImageInfo(
    val displayName: String = "",
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "",
    val dateModified: Long = 0
)

data class ViewerUiState(
    val imageUri: String = "",
    val hasEditProject: Boolean = false,
    val projectId: Long? = null,
    val imageInfo: ImageInfo? = null
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val editRepository: EditRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun loadImage(uri: String) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
        viewModelScope.launch {
            // Run both queries concurrently but update state atomically
            kotlinx.coroutines.coroutineScope {
                val projectDeferred = async {
                    try { editRepository.getProjectByUri(uri) } catch (_: Exception) { null }
                }
                val infoDeferred = async {
                    withContext(Dispatchers.IO) { queryImageInfo(Uri.parse(uri)) }
                }
                val project = projectDeferred.await()
                val info = infoDeferred.await()
                _uiState.value = _uiState.value.copy(
                    hasEditProject = project != null,
                    projectId = project?.id,
                    imageInfo = info ?: _uiState.value.imageInfo
                )
            }
        }
    }

    private fun queryImageInfo(uri: Uri): ImageInfo? {
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ImageInfo(
                        displayName = cursor.getString(0) ?: "",
                        size = cursor.getLong(1),
                        width = cursor.getInt(2),
                        height = cursor.getInt(3),
                        mimeType = cursor.getString(4) ?: "",
                        dateModified = cursor.getLong(5)
                    )
                } else null
            }
        } catch (_: Throwable) { null }
    }
}
