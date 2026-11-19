package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.SavedMediaEntity
import com.example.domain.LifeOsAiAssistant
import com.example.domain.MotivationQuote
import com.example.domain.MotivationQuotesProvider
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalLifeOsGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotivationGoalScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLifeOsGlass.current

    var activeDateIso by remember(state.selectedDateIso) { mutableStateOf(state.selectedDateIso) }
    var extraOffset by remember(activeDateIso) { mutableIntStateOf(0) }
    var selectedTheme by remember { mutableStateOf("All") }
    var committedToday by remember(activeDateIso) { mutableStateOf(false) }
    var aiQuoteText by remember { mutableStateOf<String?>(null) }
    var aiQuoteLoading by remember { mutableStateOf(false) }
    var aiQuoteError by remember { mutableStateOf<String?>(null) }

    val activeQuote = remember(activeDateIso, extraOffset) {
        MotivationQuotesProvider.quoteForDate(activeDateIso, extraOffset)
    }
    val favoriteQuotes = remember(state.savedMedia) {
        state.savedMedia.filter { it.kind == "QUOTE" }
    }
    val activeFavEntity = remember(favoriteQuotes, activeQuote) {
        favoriteQuotes.firstOrNull { it.title == activeQuote.text }
    }
    val recentDaily = remember(state.selectedDateIso) {
        MotivationQuotesProvider.recentDailyQuotes(state.selectedDateIso, days = 7)
    }
    val themedLibrary = remember(selectedTheme) {
        MotivationQuotesProvider.filterByTheme(selectedTheme)
    }

    val shareQuote: (MotivationQuote) -> Unit = { q ->
        val shareText = "\"${q.text}\"\n— ${q.author} (${q.authorRole})\n\nShared via LIFE OS • Motivation Goal"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share Motivation Quote"))
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("motivation_goal_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                AppleSubScreenHeader(
                    title = "Motivation Goal",
                    subtitle = "Daily rotating quotes • Tech leaders • AI originals",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.MOTIVATION
                )
            }

            // Daily Quote Liquid Glass Hero Card
            item(key = "daily_quote_hero") {
                LiquidGlassCard(
                    accentTint = scheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("motivation_daily_quote_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ApplePillBadge(
                                text = if (extraOffset == 0) {
                                    "DAILY QUOTE • ${DateUtils.formatShortDay(activeDateIso).uppercase()}"
                                } else {
                                    "BONUS QUOTE #${extraOffset + 1}"
                                },
                                tint = scheme.primary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ApplePillBadge(text = activeQuote.theme, tint = scheme.tertiary)
                                IconButton(
                                    onClick = {
                                        if (activeFavEntity != null) {
                                            viewModel.deleteMediaBookmark(activeFavEntity.id)
                                        } else {
                                            viewModel.saveMediaBookmark(
                                                SavedMediaEntity(
                                                    userId = state.userId,
                                                    kind = "QUOTE",
                                                    title = activeQuote.text,
                                                    subtitleOrAuthor = "${activeQuote.author} • ${activeQuote.authorRole}",
                                                    category = activeQuote.theme
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("motivation_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (activeFavEntity != null) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Save favorite quote",
                                        tint = if (activeFavEntity != null) scheme.error else scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    activeDateIso = DateUtils.offsetDateIso(activeDateIso, -1)
                                    extraOffset = 0
                                },
                                modifier = Modifier.testTag("motivation_prev_day_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day's quote")
                            }
                            Text(
                                text = DateUtils.formatPrettyDate(activeDateIso),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    activeDateIso = DateUtils.offsetDateIso(activeDateIso, 1)
                                    extraOffset = 0
                                },
                                modifier = Modifier.testTag("motivation_next_day_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day's quote")
                            }
                        }

                        AnimatedContent(
                            targetState = activeQuote,
                            label = "motivation_quote_transition"
                        ) { quote ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.FormatQuote,
                                    contentDescription = null,
                                    tint = scheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = "\"${quote.text}\"",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                    modifier = Modifier.testTag("motivation_quote_text")
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "— ${quote.author}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = scheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("motivation_quote_author")
                                    )
                                    Text(
                                        text = quote.authorRole,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.6.dp, color = glass.separator)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { extraOffset += 1 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("motivation_next_quote_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Next Quote")
                            }
                            FilledTonalButton(
                                onClick = { shareQuote(activeQuote) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("motivation_share_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    committedToday = !committedToday
                                    if (committedToday) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Locked in for today's Motivation Goal!")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("motivation_commit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (committedToday) scheme.tertiary else scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (committedToday) "Committed for Today" else "Commit to Today's Mindset")
                            }
                            if (extraOffset > 0) {
                                FilledTonalButton(
                                    onClick = { extraOffset = 0 },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("motivation_reset_today_button")
                                ) {
                                    Text("Today's")
                                }
                            }
                        }
                    }
                }
            }

            // Clearly Labeled AI-Generated Original Quote Card
            item(key = "ai_original_quote") {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentTint = scheme.secondary
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ApplePillBadge(text = "AI-GENERATED ORIGINAL", tint = scheme.secondary)
                            FilledTonalButton(
                                enabled = !aiQuoteLoading,
                                onClick = {
                                    aiQuoteLoading = true
                                    aiQuoteError = null
                                    scope.launch {
                                        LifeOsAiAssistant.generateOriginalMotivationalQuote(
                                            theme = if (selectedTheme == "All") "Focus & Growth" else selectedTheme
                                        ).onSuccess { text ->
                                            aiQuoteText = text
                                        }.onFailure { err ->
                                            aiQuoteError = err.localizedMessage ?: "Unable to generate AI quote right now."
                                        }
                                        aiQuoteLoading = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("generate_ai_quote_button")
                            ) {
                                if (aiQuoteLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Craft Quote")
                                }
                            }
                        }
                        if (!aiQuoteText.isNullOrBlank()) {
                            Text(
                                text = aiQuoteText.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Original reflection crafted by LIFE OS AI — never attributed to a real person.",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        } else if (!aiQuoteError.isNullOrBlank()) {
                            Text(aiQuoteError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = scheme.error)
                        } else {
                            Text(
                                text = "Tap Craft Quote to generate an original AI mindset prompt tailored to your theme.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (favoriteQuotes.isNotEmpty()) {
                item(key = "fav_header") {
                    AppleSectionHeader(
                        title = "Saved Favorites (${favoriteQuotes.size})",
                        subtitle = "Your personal motivation collection"
                    )
                }
                items(favoriteQuotes, key = { "fav_${it.id}" }) { fav ->
                    LiquidGlassCard(cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("\"${fav.title}\"", style = MaterialTheme.typography.bodyMedium)
                                Text("— ${fav.subtitleOrAuthor}", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteMediaBookmark(fav.id) }) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Remove favorite", tint = scheme.error)
                            }
                        }
                    }
                }
            }

            item(key = "daily_history_title") {
                AppleSectionHeader(
                    title = "Daily Changing Quote Schedule",
                    subtitle = "Automatically refreshes with a new English quote and author every day"
                )
            }

            items(recentDaily, key = { "day_${it.first}" }) { (iso, quote) ->
                val isToday = iso == state.selectedDateIso
                LiquidGlassCard(
                    cornerRadius = 20.dp,
                    accentTint = if (isToday) scheme.primary else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ApplePillBadge(
                                text = if (isToday) "Today • $iso" else "${DateUtils.formatShortDay(iso)} • $iso",
                                tint = scheme.primary
                            )
                            Text(quote.theme, style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
                        }
                        Text("\"${quote.text}\"", style = MaterialTheme.typography.bodyMedium)
                        Text("— ${quote.author}", style = MaterialTheme.typography.labelMedium, color = scheme.secondary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item(key = "theme_filter_header") {
                AppleSectionHeader(
                    title = "Explore Quotes by Theme (${themedLibrary.size})",
                    subtitle = "Verified quotes from builders, leaders & thinkers"
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MotivationQuotesProvider.allThemes.forEach { theme ->
                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme },
                            label = { Text(theme) }
                        )
                    }
                }
            }

            items(themedLibrary.take(15), key = { "lib_${it.id}" }) { quote ->
                LiquidGlassCard(cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\"${quote.text}\"", style = MaterialTheme.typography.bodyMedium)
                            Text("— ${quote.author} • ${quote.authorRole}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { shareQuote(quote) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share quote by ${quote.author}", tint = scheme.primary)
                        }
                    }
                }
            }
        }
    }
}
