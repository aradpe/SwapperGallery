package com.swappergallery.ui.gallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.swappergallery.data.model.Album
import com.swappergallery.data.model.MediaItem
import com.swappergallery.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GalleryScreen(
    onImageClick: (Uri) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionState = rememberPermissionState(PermissionUtils.imagePermission)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reload photos when permission is granted
    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.loadPhotos()
        }
    }

    // Reload photos every time gallery becomes visible (e.g. after editor save)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (permissionState.status.isGranted) {
                viewModel.loadPhotos()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.selectedAlbumId != null) {
                            uiState.albums.find { it.id == uiState.selectedAlbumId }?.name ?: "Album"
                        } else {
                            "SwapperGallery"
                        }
                    )
                },
                navigationIcon = {
                    if (uiState.selectedAlbumId != null) {
                        IconButton(onClick = { viewModel.clearAlbumSelection() }) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Back to albums"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (!permissionState.status.isGranted) {
            PermissionRequest(
                onRequestPermission = { permissionState.launchPermissionRequest() },
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(modifier = Modifier.padding(padding)) {
                if (uiState.selectedAlbumId == null) {
                    TabRow(
                        selectedTabIndex = uiState.currentTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = uiState.currentTab == GalleryTab.PHOTOS,
                            onClick = { viewModel.selectTab(GalleryTab.PHOTOS) },
                            text = { Text("Photos") },
                            icon = { Icon(Icons.Default.Photo, contentDescription = null) }
                        )
                        Tab(
                            selected = uiState.currentTab == GalleryTab.ALBUMS,
                            onClick = { viewModel.selectTab(GalleryTab.ALBUMS) },
                            text = { Text("Albums") },
                            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
                        )
                        Tab(
                            selected = uiState.currentTab == GalleryTab.EDITED,
                            onClick = { viewModel.selectTab(GalleryTab.EDITED) },
                            text = { Text("Edited") },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.loadError != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.loadError ?: "Error loading photos",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = { viewModel.loadPhotos() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    uiState.currentTab == GalleryTab.ALBUMS && uiState.selectedAlbumId == null -> {
                        AlbumGrid(
                            albums = uiState.albums,
                            onAlbumClick = { viewModel.selectAlbum(it.id) }
                        )
                    }
                    uiState.currentTab == GalleryTab.EDITED -> {
                        val editedPhotos = uiState.photos.filter {
                            uiState.editedUris.contains(it.uri.toString())
                        }
                        PhotoGrid(
                            photos = editedPhotos,
                            editedUris = uiState.editedUris,
                            onPhotoClick = { onImageClick(it.uri) }
                        )
                    }
                    else -> {
                        PhotoGrid(
                            photos = uiState.photos,
                            editedUris = uiState.editedUris,
                            onPhotoClick = { onImageClick(it.uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Permission required to access your photos",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            androidx.compose.material3.Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<MediaItem>,
    editedUris: Set<String>,
    onPhotoClick: (MediaItem) -> Unit
) {
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No photos found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoGridItem(
                photo = photo,
                hasEdits = editedUris.contains(photo.uri.toString()),
                onClick = { onPhotoClick(photo) }
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: MediaItem,
    hasEdits: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .memoryCacheKey("${photo.uri}_${photo.dateModified}")
                .diskCacheKey("${photo.uri}_${photo.dateModified}")
                .build(),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (hasEdits) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Has edits",
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No albums found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumItem(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun AlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.coverUri)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        Text(
            text = "${album.count} photos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
    }
}
