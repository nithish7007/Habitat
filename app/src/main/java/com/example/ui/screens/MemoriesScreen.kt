package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DateUtils
import com.example.data.MemoryEntity
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val MEMORY_CATEGORIES = listOf("All", "Personal", "Milestone", "Travel", "Family", "Career", "Gratitude")

/**
 * Dedicated Private Memories module — strictly separate from Notes.
 * Supports titles, rich descriptions, date & time, zero-permission Photo Picker photos,
 * tags, categories, favorites, search, calendar-date timeline grouping, and private user storage.
 */
@Composable
fun MemoriesScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<MemoryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MemoryEntity?>(null) }

    val filtered = remember(state.memories, searchQuery, selectedCategory, favoritesOnly) {
        state.memories.filter { m ->
            val catOk = selectedCategory == "All" || m.category.equals(selectedCategory, ignoreCase = true)
            val favOk = !favoritesOnly || m.favorite
            val q = searchQuery.trim()
            val queryOk = q.isBlank() ||
                m.title.contains(q, ignoreCase = true) ||
                m.description.contains(q, ignoreCase = true) ||
                m.tagsCsv.contains(q, ignoreCase = true) ||
                m.memoryDateIso.contains(q, ignoreCase = true)
            catOk && favOk && queryOk
        }.sortedWith(compareByDescending<MemoryEntity> { it.memoryDateIso }.thenByDescending { it.createdAtMs })
    }

    val groupedByDate = remember(filtered) {
        filtered.groupBy { it.memoryDateIso }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMemory = null
                    showEditor = true
                },
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 84.dp)
                    .testTag("add_memory_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Memory")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("memories_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Memories",
                    subtitle = "Private life moments • Separate from Notes",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.MEMORIES
                )
            }

            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search memories, tags, or YYYY-MM-DD...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memories_search_field")
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = favoritesOnly,
                                    onClick = { favoritesOnly = !favoritesOnly },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (favoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("Favorites") }
                                )
                            }
                            items(MEMORY_CATEGORIES) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }
            }

            if (groupedByDate.isEmpty()) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = scheme.primary.copy(alpha = 0.65f),
                                modifier = Modifier.size(38.dp)
                            )
                            Text("Capture Your Meaningful Moments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Store private memories with photos, dates, categories, and tags — kept completely separate from your study & code notes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    editingMemory = null
                                    showEditor = true
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Add First Memory")
                            }
                        }
                    }
                }
            } else {
                groupedByDate.forEach { (dateIso, dayMemories) ->
                    item(key = "hdr_$dateIso") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = dateIso,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary
                            )
                        }
                    }

                    items(dayMemories, key = { it.id }) { memory ->
                        val photoFile = remember(memory.photoLocalPath) {
                            memory.photoLocalPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }
                        }
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (photoFile != null) {
                                    AsyncImage(
                                        model = photoFile,
                                        contentDescription = memory.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ApplePillBadge(text = memory.category, tint = scheme.secondary)
                                        if (memory.memoryTimeText.isNotBlank()) {
                                            Text(
                                                text = memory.memoryTimeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = scheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            viewModel.saveMemory(memory.copy(favorite = !memory.favorite))
                                        }) {
                                            Icon(
                                                imageVector = if (memory.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = "Favorite memory",
                                                tint = if (memory.favorite) scheme.error else scheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            editingMemory = memory
                                            showEditor = true
                                        }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit memory")
                                        }
                                        IconButton(onClick = { pendingDelete = memory }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete memory", tint = scheme.error)
                                        }
                                    }
                                }
                                Text(
                                    text = memory.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (memory.description.isNotBlank()) {
                                    Text(
                                        text = memory.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurface.copy(alpha = 0.88f)
                                    )
                                }
                                val tags = memory.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                if (tags.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        tags.take(4).forEach { tag ->
                                            ApplePillBadge(text = "#$tag", tint = scheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        MemoryEditorDialog(
            initial = editingMemory,
            userId = state.userId,
            viewModel = viewModel,
            onDismiss = { showEditor = false },
            onSaved = { showEditor = false }
        )
    }

    pendingDelete?.let { mem ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Memory?") },
            text = { Text("Remove \"${mem.title}\" permanently from your private memories?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMemory(mem)
                    pendingDelete = null
                }) { Text("Delete", color = scheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    initial: MemoryEntity?,
    userId: String,
    viewModel: LifeOsViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val memoryId = remember(initial) { initial?.id ?: UUID.randomUUID().toString() }
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var dateIso by remember(initial) { mutableStateOf(initial?.memoryDateIso ?: DateUtils.todayIso()) }
    var timeText by remember(initial) {
        mutableStateOf(
            initial?.memoryTimeText?.ifBlank {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            } ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
    }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Personal") }
    var tagsCsv by remember(initial) { mutableStateOf(initial?.tagsCsv.orEmpty()) }
    var photoPath by remember(initial) { mutableStateOf(initial?.photoLocalPath.orEmpty()) }
    var favorite by remember(initial) { mutableStateOf(initial?.favorite ?: false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.saveMemoryPhoto(memoryId, uri).onSuccess { path ->
                    photoPath = path
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Private Memory" else "Edit Memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Memory Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What happened?") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = { dateIso = it.take(10) },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it.take(10) },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MEMORY_CATEGORIES.drop(1)) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = tagsCsv,
                    onValueChange = { tagsCsv = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoPath.isBlank()) "Attach Photo" else "Change Photo")
                    }
                    FilterChip(
                        selected = favorite,
                        onClick = { favorite = !favorite },
                        label = { Text("Favorite") }
                    )
                }
                if (photoPath.isNotBlank() && File(photoPath).exists()) {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = "Selected memory photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty(),
                onClick = {
                    viewModel.saveMemory(
                        MemoryEntity(
                            id = memoryId,
                            userId = userId,
                            title = title.trim(),
                            description = description.trim(),
                            memoryDateIso = dateIso.ifBlank { DateUtils.todayIso() },
                            memoryTimeText = timeText.trim(),
                            category = category,
                            tagsCsv = tagsCsv.trim(),
                            photoLocalPath = photoPath,
                            favorite = favorite,
                            createdAtMs = initial?.createdAtMs ?: System.currentTimeMillis()
                        )
                    )
                    onSaved()
                }
            ) {
                Text("Save Memory")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
