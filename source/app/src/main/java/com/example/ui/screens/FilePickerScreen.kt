package com.example.ui.screens

import android.net.Uri
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.provider.ContactsContract
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.TransferItem
import com.example.ui.MediaTab
import com.example.ui.Screen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SuccessGreen
import com.example.util.StorageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerScreen(
    selectedFiles: List<TransferItem>,
    scannedMedia: List<TransferItem>,
    selectedTab: MediaTab,
    isLoadingMedia: Boolean,
    onTabSelected: (MediaTab) -> Unit,
    onFileToggled: (TransferItem) -> Unit,
    onFilesAdded: (List<Uri>) -> Unit,
    onClearSelected: () -> Unit,
    onRemoveSelected: (TransferItem) -> Unit,
    onBack: () -> Unit,
    onProceedToSend: () -> Unit
) {
    // SAF System File Picker for selecting any file types (documents, archives, APKs)
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onFilesAdded(uris)
        }
    }

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val lookupKey = runCatching {
                context.contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.LOOKUP_KEY), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
            if (!lookupKey.isNullOrBlank()) {
                val vcardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
                onFilesAdded(listOf(vcardUri))
            }
        }
    }
    val totalBytes = selectedFiles.sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Select Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (selectedFiles.isEmpty()) "Tap files or use System File Picker"
                            else "${selectedFiles.size} selected (${StorageHelper.formatBytes(totalBytes)})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (selectedFiles.isNotEmpty()) {
                        IconButton(onClick = onClearSelected) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Selection"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedFiles.size} Files Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = StorageHelper.formatBytes(totalBytes),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onProceedToSend,
                        enabled = selectedFiles.isNotEmpty(),
                        modifier = Modifier
                            .testTag("proceed_send_button")
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Send (${selectedFiles.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // System SAF File Picker Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        safPickerLauncher.launch(arrayOf("*/*"))
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "System File Picker",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Browse Device Files (SAF)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Select any files, APKs, documents, or large archives",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (selectedFiles.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedFiles) { selected ->
                        Card(
                            modifier = Modifier.clickable { onRemoveSelected(selected) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(selected.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(Icons.Default.Close, contentDescription = "Remove selected file", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = { contactPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share a Contact")
            }

            // Category Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MediaTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.name.replace("_", " "),
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Main Media Content
            if (isLoadingMedia) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else if (selectedTab == MediaTab.PHOTOS || selectedTab == MediaTab.VIDEOS) {
                if (scannedMedia.isEmpty()) {
                    EmptyMediaView(
                        icon = if (selectedTab == MediaTab.PHOTOS) Icons.Default.Image else Icons.Default.Videocam,
                        message = "No ${selectedTab.name.lowercase()} found on device",
                        subMessage = "Use 'Browse Device Files' above to pick from storage"
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(scannedMedia) { item ->
                            val isSelected = selectedFiles.any { it.id == item.id || (it.uri != null && it.uri == item.uri) }
                            MediaGridItem(
                                item = item,
                                isSelected = isSelected,
                                onToggle = { onFileToggled(item) }
                            )
                        }
                    }
                }
            } else if (selectedTab == MediaTab.APPS) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(scannedMedia) { item ->
                        val isSelected = selectedFiles.any { it.id == item.id || (it.uri != null && it.uri == item.uri) }
                        AppGridItem(item = item, isSelected = isSelected, onToggle = { onFileToggled(item) })
                    }
                }
            } else {
                // List View for Audio, Documents, All
                if (scannedMedia.isEmpty()) {
                    EmptyMediaView(
                        icon = when (selectedTab) {
                            MediaTab.AUDIO -> Icons.Default.Audiotrack
                            MediaTab.APPS -> Icons.Default.Android
                            MediaTab.DOCUMENTS -> Icons.Default.Description
                            else -> Icons.Default.Folder
                        },
                        message = "Tap 'Browse Device Files' above",
                        subMessage = "Choose ${selectedTab.name.lowercase()} directly via Android File Picker"
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(scannedMedia) { item ->
                            val isSelected = selectedFiles.any { it.id == item.id || (it.uri != null && it.uri == item.uri) }
                            MediaListRow(
                                item = item,
                                isSelected = isSelected,
                                onToggle = { onFileToggled(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppGridItem(item: TransferItem, isSelected: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val appIcon = remember(item.id) {
        runCatching { context.packageManager.getApplicationIcon(item.id.removePrefix("app_")) }.getOrNull()
    }
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                AsyncImage(model = appIcon, contentDescription = item.name, modifier = Modifier.size(48.dp))
            } else {
                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(40.dp))
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
            }
        }
        Text(item.name, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
fun MediaGridItem(
    item: TransferItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    var showPreview by remember(item.id) { mutableStateOf(false) }
    var showPhotoPreview by remember(item.id) { mutableStateOf(false) }
    var previewLandscape by remember(item.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val videoFrame by produceState<Bitmap?>(initialValue = null, key1 = item.uri) {
        if (item.mimeType.startsWith("video/") && item.uri != null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    MediaMetadataRetriever().let { retriever ->
                        try {
                            retriever.setDataSource(context, item.uri)
                            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } finally { retriever.release() }
                    }
                }.getOrNull()
            }
        }
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(110.dp)
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onToggle() },
                    onLongPress = {
                    if (item.mimeType.startsWith("video/") && item.uri != null) showPreview = true
                    else if (item.mimeType.startsWith("image/") && item.uri != null) showPhotoPreview = true
                }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.mimeType.startsWith("video/") && videoFrame != null) {
                Image(bitmap = videoFrame!!.asImageBitmap(), contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (item.mimeType.startsWith("video/")) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.62f)).padding(horizontal = 5.dp, vertical = 3.dp)
                )
            }

            // Selection Indicator Checkbox
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Size badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = StorageHelper.formatBytes(item.size),
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showPhotoPreview && item.uri != null) {
        AlertDialog(
            onDismissRequest = { showPhotoPreview = false },
            title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = { TextButton(onClick = { showPhotoPreview = false }) { Text("Close") } }
        )
    }

    if (showPreview && item.uri != null) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(item.uri)
                            setMediaController(MediaController(context).also { it.setAnchorView(this) })
                            setOnPreparedListener { player -> player.isLooping = false; start() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(if (previewLandscape) 300.dp else 220.dp)
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        previewLandscape = !previewLandscape
                        context.findActivity()?.requestedOrientation = if (previewLandscape)
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }) { Text(if (previewLandscape) "Portrait" else "Rotate") }
                    TextButton(onClick = {
                        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        showPreview = false
                    }) { Text("Close") }
                }
            }
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun MediaListRow(
    item: TransferItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(item.id) {
        if (item.mimeType == "application/vnd.android.package-archive") {
            runCatching { context.packageManager.getApplicationIcon(item.id.removePrefix("app_")) }.getOrNull()
        } else null
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    AsyncImage(model = appIcon, contentDescription = item.name, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(
                        imageVector = if (item.mimeType.startsWith("audio/")) Icons.Default.Audiotrack else Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${StorageHelper.formatBytes(item.size)} • ${item.mimeType.substringAfter('/')}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyMediaView(
    icon: ImageVector,
    message: String,
    subMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subMessage,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
