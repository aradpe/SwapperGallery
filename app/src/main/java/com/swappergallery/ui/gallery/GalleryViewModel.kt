package com.swappergallery.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swappergallery.data.model.Album
import com.swappergallery.data.model.MediaItem
import com.swappergallery.data.repository.EditRepository
import com.swappergallery.data.repository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val photos: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val editedUris: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val selectedAlbumId: Long? = null,
    val currentTab: GalleryTab = GalleryTab.PHOTOS
)

enum class GalleryTab { PHOTOS, ALBUMS, EDITED }

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val editRepository: EditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val photos = galleryRepository.getAllPhotos()
                val albums = galleryRepository.getAlbums(preloadedPhotos = photos)
                val editedUris = editRepository.getAllEditedUris()
                _uiState.value = _uiState.value.copy(
                    photos = photos,
                    albums = albums,
                    editedUris = editedUris,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = "Could not load photos: ${e.message}"
                )
            }
        }
    }

    fun selectTab(tab: GalleryTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectAlbum(albumId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedAlbumId = albumId,
                isLoading = true
            )
            val photos = galleryRepository.getPhotosForAlbum(albumId)
            _uiState.value = _uiState.value.copy(
                photos = photos,
                isLoading = false
            )
        }
    }

    fun clearAlbumSelection() {
        _uiState.value = _uiState.value.copy(selectedAlbumId = null)
        loadPhotos()
    }
}
