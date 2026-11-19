package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.TaskEntity
import com.example.domain.AiChatMessage
import com.example.domain.LifeOsAiAssistant
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.ReactiveMinimalMascot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

private val quickAssistantPrompts = listOf(
    "Show my tasks today",
    "How much water did I drink?",
    "Show my current savings & balance",
    "What is my current streak?",
    "Show tomorrow's classes",
    "Summarize my weekly productivity",
    "Add a task to review Spring Boot"
)

@Composable
fun AiAssistantScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                id = "welcome",
                isUser = false,
                text = "Hi ${state.displayName.substringBefore(" ")}! I'm your LIFE OS AI Assistant powered by Gemini. Ask about your real tasks, water intake, balance, savings, streak, classes, interviews, or weekly summary."
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val sendPrompt: (String) -> Unit = { raw ->
        val prompt = raw.trim()
        if (prompt.isNotBlank() && !isSending) {
            inputText = ""
            errorMessage = null
            isSending = true
            val historySnapshot = messages.toList()
            messages += AiChatMessage(id = UUID.randomUUID().toString(), isUser = true, text = prompt)
            val replyId = UUID.randomUUID().toString()
            messages += AiChatMessage(id = replyId, isUser = false, text = "", isStreaming = true)

            scope.launch {
                try {
                    var finalRaw = ""
                    LifeOsAiAssistant.streamAssistantReply(
                        userPrompt = prompt,
                        history = historySnapshot,
                        state = state
                    ).collect { partial ->
                        finalRaw = partial
                        val (clean, _) = LifeOsAiAssistant.extractProposedTask(partial)
                        val idx = messages.indexOfFirst { it.id == replyId }
                        if (idx >= 0) {
                            messages[idx] = messages[idx].copy(text = clean, isStreaming = true)
                        }
                    }
                    val (clean, proposedTask) = LifeOsAiAssistant.extractProposedTask(finalRaw)
                    val idx = messages.indexOfFirst { it.id == replyId }
                    if (idx >= 0) {
                        messages[idx] = messages[idx].copy(
                            text = clean,
                            isStreaming = false,
                            suggestedTaskTitle = proposedTask
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val idx = messages.indexOfFirst { it.id == replyId }
                    if (idx >= 0) messages.removeAt(idx)
                    errorMessage = e.localizedMessage ?: "Gemini request failed. Please check network connectivity and retry."
                } finally {
                    isSending = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("ai_assistant_screen")
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                AppleSubScreenHeader(
                    title = "LIFE OS AI Assistant",
                    subtitle = "Powered by Firebase AI Logic",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.AI
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReactiveMinimalMascot(mood = state.streakReport.mascotMood, size = 46.dp)
                    Column {
                        Text(
                            text = "Connected to your live LIFE OS data",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Answers strictly from your real Room & Firestore records",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Command Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickAssistantPrompts) { chipText ->
                    SuggestionChip(
                        onClick = { sendPrompt(chipText) },
                        enabled = !isSending,
                        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(chipText, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { errorMessage = null }) { Text("Dismiss") }
                    }
                }
            }

            // Chat History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (msg.isUser) {
                            Surface(
                                shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            GlassCard(
                                cornerRadius = 20.dp,
                                modifier = Modifier.widthIn(max = 330.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (msg.text.isBlank() && msg.isStreaming) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                                    }

                                    // Safe confirmation card before creating any task
                                    msg.suggestedTaskTitle?.let { proposedTitle ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Proposed Task: \"$proposedTitle\"",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.saveTask(
                                                                TaskEntity(
                                                                    userId = state.userId,
                                                                    title = proposedTitle,
                                                                    dueDateIso = DateUtils.todayIso(),
                                                                    category = "AI Assistant"
                                                                )
                                                            )
                                                            val idx = messages.indexOfFirst { it.id == msg.id }
                                                            if (idx >= 0) messages[idx] = messages[idx].copy(suggestedTaskTitle = null)
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Task added: $proposedTitle")
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Confirm Add")
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val idx = messages.indexOfFirst { it.id == msg.id }
                                                            if (idx >= 0) messages[idx] = messages[idx].copy(suggestedTaskTitle = null)
                                                        }
                                                    ) { Text("Dismiss") }
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

            // Input Composer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about tasks, money, streak, classes...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )
                FilledIconButton(
                    onClick = { sendPrompt(inputText) },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("ai_chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    }
}
