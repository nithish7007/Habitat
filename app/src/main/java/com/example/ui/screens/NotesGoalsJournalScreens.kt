package com.example.ui.screens

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DateUtils
import com.example.data.GoalEntity
import com.example.data.GoalStatus
import com.example.data.JournalEntryEntity
import com.example.data.NoteEntity
import com.example.data.RoutineEntity
import com.example.data.RoutineType
import com.example.data.TaskPriority
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private val NOTE_FOLDERS = listOf(
    "All",
    "Quick Notes",
    "Personal",
    "Study",
    "Java",
    "SQL",
    "Interview",
    "Project Ideas",
    "Daily Thoughts"
)

@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    AppleSubScreenHeader(
        title = title,
        onBack = onBack,
        trailing = trailing
    )
}

@Composable
fun NotesScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var search by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var showArchived by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(state.notes, search, selectedFolder, showArchived, favoritesOnly) {
        state.notes.filter { n ->
            n.archived == showArchived &&
                (!favoritesOnly || n.favorite) &&
                (selectedFolder == "All" || n.folder.equals(selectedFolder, ignoreCase = true)) &&
                (search.isBlank() ||
                    n.title.contains(search, true) ||
                    n.content.contains(search, true) ||
                    n.labelsCsv.contains(search, true) ||
                    n.folder.contains(search, true))
        }.sortedWith(compareByDescending<NoteEntity> { it.pinned }.thenByDescending { it.updatedAtMs })
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("notes_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Notes & Code",
                    subtitle = "Folders • Code blocks • Autosave • Cloud sync",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.NOTES,
                    trailing = {
                        FilterChip(
                            selected = showArchived,
                            onClick = { showArchived = !showArchived },
                            label = { Text(if (showArchived) "Archived" else "Active") }
                        )
                    }
                )
            }
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Search notes, code snippets, folders, or tags...") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = favoritesOnly,
                                    onClick = { favoritesOnly = !favoritesOnly },
                                    label = { Text("Favorites") }
                                )
                            }
                            items(NOTE_FOLDERS) { folder ->
                                FilterChip(
                                    selected = selectedFolder == folder,
                                    onClick = { selectedFolder = folder },
                                    label = { Text(folder) }
                                )
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No notes in $selectedFolder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Tap + to write Quick Notes, Java/SQL code snippets, Interview prep, or Project Ideas with autosave.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { note ->
                    LiquidGlassCard(
                        cornerRadius = 22.dp,
                        onClick = { editingNote = note; showDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ApplePillBadge(text = note.folder.ifBlank { "Quick Notes" }, tint = scheme.primary)
                                    if (note.isCodeNote) {
                                        ApplePillBadge(text = "CODE", tint = scheme.secondary)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { viewModel.saveNote(note.copy(favorite = !note.favorite)) }) {
                                        Icon(
                                            imageVector = if (note.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "Favorite note",
                                            tint = if (note.favorite) scheme.error else scheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.saveNote(note.copy(pinned = !note.pinned)) }) {
                                        Icon(
                                            Icons.Filled.PushPin,
                                            contentDescription = "Pin note",
                                            tint = if (note.pinned) scheme.primary else scheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.saveNote(note.copy(archived = !note.archived)) }) {
                                        Icon(Icons.Filled.Archive, contentDescription = "Archive note")
                                    }
                                    IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = scheme.error)
                                    }
                                }
                            }
                            Text(
                                text = note.title.ifBlank { "Untitled Note" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (note.content.isNotBlank()) {
                                if (note.isCodeNote) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF11131A))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = note.content,
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFE2E8F0),
                                            maxLines = 8
                                        )
                                    }
                                } else {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 4
                                    )
                                }
                            }
                            if (note.attachmentUri.isNotBlank()) {
                                AsyncImage(
                                    model = note.attachmentUri,
                                    contentDescription = "Note attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                            if (note.labelsCsv.isNotBlank()) {
                                Text(
                                    text = "Tags: ${note.labelsCsv}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingNote = null; showDialog = true },
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 104.dp)
                .testTag("add_note_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Note")
        }
    }

    if (showDialog) {
        val noteId = remember(editingNote) { editingNote?.id ?: UUID.randomUUID().toString() }
        var title by remember(editingNote) { mutableStateOf(editingNote?.title ?: "") }
        var content by remember(editingNote) { mutableStateOf(editingNote?.content ?: "") }
        var folder by remember(editingNote) { mutableStateOf(editingNote?.folder ?: (if (selectedFolder == "All") "Quick Notes" else selectedFolder)) }
        var labels by remember(editingNote) { mutableStateOf(editingNote?.labelsCsv ?: "") }
        var isCodeNote by remember(editingNote) { mutableStateOf(editingNote?.isCodeNote ?: false) }
        var attachmentUri by remember(editingNote) { mutableStateOf(editingNote?.attachmentUri ?: "") }
        var autosaveStatus by remember { mutableStateOf("Autosave ready") }

        // Debounced autosave whenever user edits an existing or non-empty draft
        LaunchedEffect(title, content, folder, labels, isCodeNote, attachmentUri) {
            if (title.isBlank() && content.isBlank()) return@LaunchedEffect
            autosaveStatus = "Saving..."
            delay(900L)
            viewModel.saveNote(
                (editingNote ?: NoteEntity(id = noteId)).copy(
                    id = noteId,
                    title = title.trim(),
                    content = content,
                    folder = folder,
                    labelsCsv = labels.trim(),
                    isCodeNote = isCodeNote,
                    attachmentUri = attachmentUri
                )
            )
            autosaveStatus = "Autosaved"
        }

        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) attachmentUri = uri.toString()
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (editingNote == null) "New Note" else "Edit Note")
                    Text(autosaveStatus, style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(NOTE_FOLDERS.drop(1)) { f ->
                            FilterChip(
                                selected = folder == f,
                                onClick = { folder = f },
                                label = { Text(f) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = scheme.primary)
                            Text("Code Formatting (Monospace)", style = MaterialTheme.typography.labelMedium)
                        }
                        Switch(checked = isCodeNote, onCheckedChange = { isCodeNote = it })
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(if (isCodeNote) "Code Snippet" else "Content") },
                        textStyle = if (isCodeNote) MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily) else MaterialTheme.typography.bodyMedium,
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = labels,
                        onValueChange = { labels = it },
                        label = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (attachmentUri.isBlank()) "Attach Photo (Zero Permission)" else "Change Attached Photo")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveNote(
                        (editingNote ?: NoteEntity(id = noteId)).copy(
                            id = noteId,
                            title = title.trim(),
                            content = content,
                            folder = folder,
                            labelsCsv = labels.trim(),
                            isCodeNote = isCodeNote,
                            attachmentUri = attachmentUri
                        )
                    )
                    showDialog = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val dateIso = state.selectedDateIso

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Routine Builder",
                    subtitle = "Structured morning, study & sleep flows",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.ROUTINES
                )
            }
            if (state.routines.isEmpty()) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Build structured Morning, Study, Exercise, or Sleep routines. Tap + to create your first routine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(state.routines, key = { it.id }) { routine ->
                    val steps = routine.stepsCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    val log = state.routineLogs.firstOrNull { it.routineId == routine.id && it.dateIso == dateIso }
                    val doneSet = log?.completedStepsCsv?.split("|")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(routine.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${routine.routineType} • ${doneSet.size}/${steps.size} steps today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete routine", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            steps.forEach { step ->
                                val done = step in doneSet
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleRoutineStep(routine, step, dateIso) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (done) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(step, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Add Routine") }
    }

    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(RoutineType.MORNING.name) }
        var stepsText by remember { mutableStateOf("Hydrate 500ml\nReview top 3 tasks\n15m deep focus") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Routine Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoutineType.entries.forEach { rt ->
                            FilterChip(selected = type == rt.name, onClick = { type = rt.name }, label = { Text(rt.name) })
                        }
                    }
                    OutlinedTextField(value = stepsText, onValueChange = { stepsText = it }, label = { Text("Steps (one per line)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val csv = stepsText.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("|")
                        if (title.isNotBlank() && csv.isNotBlank()) {
                            viewModel.saveRoutine(RoutineEntity(title = title.trim(), routineType = type, stepsCsv = csv))
                            showDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Goal Center",
                    subtitle = "7 to 180-day milestones",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.GOALS
                )
            }
            if (state.goals.isEmpty()) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Create 7, 30, 60, 90, or 180-day goals with interactive milestones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(state.goals, key = { it.id }) { goal ->
                    val milestones = goal.milestonesCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    val doneSet = goal.completedMilestonesCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }.toSet()

                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${goal.horizon.replace("_", " ")} • ${goal.status} • Due ${goal.targetDateIso}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ApplePillBadge(text = "${goal.progressPercent}%", tint = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { viewModel.deleteGoal(goal.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { goal.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                                strokeCap = StrokeCap.Round
                            )
                            milestones.forEach { ms ->
                                val done = ms in doneSet
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleGoalMilestone(goal, ms) }.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(if (done) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, contentDescription = null, tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(ms, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Add Goal") }
    }

    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var horizon by remember { mutableStateOf("30_DAYS") }
        var milestones by remember { mutableStateOf("Define scope\nComplete 50%\nFinal review") }
        val horizons = listOf("7_DAYS", "30_DAYS", "60_DAYS", "90_DAYS", "180_DAYS")
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        horizons.forEach { h -> FilterChip(selected = horizon == h, onClick = { horizon = h }, label = { Text(h.replace("_", " ")) }) }
                    }
                    OutlinedTextField(value = milestones, onValueChange = { milestones = it }, label = { Text("Milestones (one per line)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = horizon.substringBefore("_").toIntOrNull() ?: 30
                        val csv = milestones.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("|")
                        if (title.isNotBlank()) {
                            viewModel.saveGoal(
                                GoalEntity(
                                    title = title.trim(),
                                    horizon = horizon,
                                    priority = TaskPriority.HIGH.name,
                                    status = GoalStatus.ACTIVE.name,
                                    targetDateIso = DateUtils.offsetDateIso(DateUtils.todayIso(), days),
                                    milestonesCsv = csv
                                )
                            )
                            showDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text("Save Goal") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateIso = state.selectedDateIso
    val existing = state.journalEntries.firstOrNull { it.dateIso == dateIso }

    var highlights by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.highlights ?: "") }
    var learned by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.learned ?: "") }
    var wentWell by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.wentWell ?: "") }
    var wentWrong by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.wentWrong ?: "") }
    var tomorrowPlan by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.tomorrowPlan ?: "") }
    var gratitude by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.gratitude ?: "") }
    var mood by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.mood ?: "Focused") }
    var privateEntry by remember(dateIso, existing?.updatedAtMs) { mutableStateOf(existing?.privateEntry ?: false) }

    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val doc = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = doc.startPage(pageInfo)
                    val paint = Paint().apply { textSize = 14f }
                    var y = 60f
                    listOf(
                        "LIFE OS Daily Journal — $dateIso",
                        "Mood: $mood",
                        "Highlights: $highlights",
                        "What I Learned: $learned",
                        "What Went Well: $wentWell",
                        "What Went Wrong: $wentWrong",
                        "Tomorrow's Plan: $tomorrowPlan",
                        "Gratitude: $gratitude"
                    ).forEach { line ->
                        page.canvas.drawText(line.take(90), 40f, y, paint)
                        y += 32f
                    }
                    doc.finishPage(page)
                    context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                    doc.close()
                }.onSuccess {
                    snackbarHostState.showSnackbar("Journal exported as PDF")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Daily Journal",
                    subtitle = dateIso,
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.JOURNAL,
                    trailing = {
                        IconButton(onClick = { pdfExportLauncher.launch("lifeos_journal_$dateIso.pdf") }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export Journal PDF")
                        }
                    }
                )
            }
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(if (privateEntry) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = null)
                                Text("Private Entry", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Switch(checked = privateEntry, onCheckedChange = { privateEntry = it })
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Focused", "Grateful", "Energized", "Calm", "Stretched").forEach { m ->
                                FilterChip(selected = mood == m, onClick = { mood = m }, label = { Text(m) })
                            }
                        }
                        OutlinedTextField(value = highlights, onValueChange = { highlights = it }, label = { Text("Today's Highlights") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = learned, onValueChange = { learned = it }, label = { Text("What I Learned") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = wentWell, onValueChange = { wentWell = it }, label = { Text("What Went Well") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = wentWrong, onValueChange = { wentWrong = it }, label = { Text("What Went Wrong") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tomorrowPlan, onValueChange = { tomorrowPlan = it }, label = { Text("Tomorrow's Plan") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = gratitude, onValueChange = { gratitude = it }, label = { Text("Gratitude") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                viewModel.saveJournalEntry(
                                    JournalEntryEntity(
                                        id = existing?.id ?: "journal_${dateIso.replace("-", "")}",
                                        dateIso = dateIso,
                                        highlights = highlights,
                                        learned = learned,
                                        wentWell = wentWell,
                                        wentWrong = wentWrong,
                                        tomorrowPlan = tomorrowPlan,
                                        gratitude = gratitude,
                                        mood = mood,
                                        privateEntry = privateEntry
                                    )
                                )
                                scope.launch { snackbarHostState.showSnackbar("Journal entry saved") }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save Journal Entry")
                        }
                    }
                }
            }
        }
    }
}
