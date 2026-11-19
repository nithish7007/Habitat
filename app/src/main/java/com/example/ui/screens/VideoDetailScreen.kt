package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.SavedMediaEntity
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.ApplePillBadge
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.components.LiquidGlassCard

data class VideoDetailPayload(
    val title: String,
    val channel: String,
    val publishedDate: String,
    val url: String,
    val thumbnailUrl: String,
    val category: String,
    val language: String
) {
    fun encode(): String {
        val obj = org.json.JSONObject().apply {
            put("t", title)
            put("c", channel)
            put("p", publishedDate)
            put("u", url)
            put("th", thumbnailUrl)
            put("cat", category)
            put("l", language)
        }
        return android.util.Base64.encodeToString(
            obj.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
    }

    companion object {
        fun decode(raw: String): VideoDetailPayload {
            if (raw.contains("||")) {
                val parts = raw.split("||").map { Uri.decode(it) }
                return VideoDetailPayload(
                    title = parts.getOrNull(0).orEmpty().ifBlank { "Educational Video" },
                    channel = parts.getOrNull(1).orEmpty().ifBlank { "Official Tech Channel" },
                    publishedDate = parts.getOrNull(2).orEmpty(),
                    url = parts.getOrNull(3).orEmpty(),
                    thumbnailUrl = parts.getOrNull(4).orEmpty(),
                    category = parts.getOrNull(5).orEmpty().ifBlank { "Technology" },
                    language = parts.getOrNull(6).orEmpty().ifBlank { "English" }
                )
            }
            return runCatching {
                val decoded = String(
                    android.util.Base64.decode(raw, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP),
                    Charsets.UTF_8
                )
                val obj = org.json.JSONObject(decoded)
                VideoDetailPayload(
                    title = obj.optString("t").ifBlank { "Educational Video" },
                    channel = obj.optString("c").ifBlank { "Official Tech Channel" },
                    publishedDate = obj.optString("p"),
                    url = obj.optString("u"),
                    thumbnailUrl = obj.optString("th"),
                    category = obj.optString("cat").ifBlank { "Technology" },
                    language = obj.optString("l").ifBlank { "English" }
                )
            }.getOrElse {
                VideoDetailPayload(
                    title = "Educational Video",
                    channel = "Official Tech Channel",
                    publishedDate = "",
                    url = "",
                    thumbnailUrl = "",
                    category = "Technology",
                    language = "English"
                )
            }
        }
    }
}

/**
 * In-app Educational Video Details screen.
 * Reached from Learn -> Videos or by tapping a YouTube Video notification.
 * NEVER auto-launches YouTube — only opens YouTube when user explicitly taps "Watch on YouTube".
 */
@Composable
fun VideoDetailScreen(
    encodedPayload: String,
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val video = remember(encodedPayload) { VideoDetailPayload.decode(encodedPayload) }
    val savedEntry = remember(state.savedMedia, video.url) {
        state.savedMedia.firstOrNull { it.kind == "VIDEO" && it.url == video.url }
    }
    val isSaved = savedEntry != null
    val isWatched = savedEntry?.watchedOrRead == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AppleSubScreenHeader(
                title = "Video Details",
                subtitle = video.channel,
                onBack = onBack,
                badgeType = LifeOsBadgeType.VIDEO
            )
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (video.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(206.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = scheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text("Official Educational Portal", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ApplePillBadge(text = video.category, tint = scheme.primary)
                        ApplePillBadge(text = video.language, tint = scheme.secondary)
                        if (isWatched) {
                            ApplePillBadge(text = "Watched", tint = scheme.tertiary)
                        }
                    }

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = buildString {
                            append("Channel: ${video.channel}")
                            if (video.publishedDate.isNotBlank()) append(" • Published ${video.publishedDate}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )

                    Text(
                        text = "LIFE OS opens this preview screen first so you stay in control. Tap Watch on YouTube below whenever you're ready to open the video.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (video.url.isNotBlank()) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
                                }
                            }
                        },
                        enabled = video.url.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("watch_on_youtube_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Watch on YouTube")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (savedEntry != null) {
                                    viewModel.deleteMediaBookmark(savedEntry.id)
                                } else {
                                    viewModel.saveMediaBookmark(
                                        SavedMediaEntity(
                                            userId = state.userId,
                                            kind = "VIDEO",
                                            title = video.title,
                                            subtitleOrAuthor = video.channel,
                                            url = video.url,
                                            category = video.category,
                                            watchedOrRead = false
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSaved) "Saved" else "Save for Later")
                        }

                        OutlinedButton(
                            onClick = {
                                val next = savedEntry?.copy(watchedOrRead = !isWatched) ?: SavedMediaEntity(
                                    userId = state.userId,
                                    kind = "VIDEO",
                                    title = video.title,
                                    subtitleOrAuthor = video.channel,
                                    url = video.url,
                                    category = video.category,
                                    watchedOrRead = true
                                )
                                viewModel.saveMediaBookmark(next)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isWatched) "Watched" else "Mark Watched")
                        }
                    }
                }
            }
        }
    }
}
