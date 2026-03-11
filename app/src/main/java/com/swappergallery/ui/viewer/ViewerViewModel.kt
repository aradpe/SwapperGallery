package com.swappergallery.ui.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swappergallery.data.repository.EditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val imageUri: String = "",
    val hasEditProject: Boolean = false,
    val projectId: Long? = null
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val editRepository: EditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun loadImage(uri: String) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
        viewModelScope.launch {
            try {
                val project = editRepository.getProjectByUri(uri)
                _uiState.value = _uiState.value.copy(
                    hasEditProject = project != null,
                    projectId = project?.id
                )
            } catch (_: Exception) {
                // Database might not be ready yet, ignore
            }
        }
    }
}
