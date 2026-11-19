package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.LearningLevel
import com.example.data.LearningTopicEntity
import com.example.data.NewsAndVideoRepository
import com.example.data.SavedMediaEntity
import com.example.data.TechNewsArticle
import com.example.data.YouTubeVideoItem
import com.example.domain.TechLearningCatalog
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnHubScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onNavigate: (String) -> Unit,
    initialSection: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeSection by remember(initialSection) { mutableIntStateOf(initialSection) } // 0=Courses, 1=Tech News, 2=YouTube Videos

    // Courses filter state
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedLevel by remember { mutableStateOf("ALL") }
    var editingTopic by remember { mutableStateOf<LearningTopicEntity?>(null) }

    // Tech News live state
    var newsLoading by remember { mutableStateOf(false) }
    var newsError by remember { mutableStateOf<String?>(null) }
    var newsArticles by remember { mutableStateOf<List<TechNewsArticle>>(emptyList()) }
    var newsCategory by remember { mutableStateOf("All") }

    // YouTube Videos live state
    var videoCategory by remember { mutableStateOf(NewsAndVideoRepository.videoCategories.first()) }
    var videoLanguage by remember { mutableStateOf("English") }
    var videoSearchQuery by remember { mutableStateOf("") }
    var videosLoading by remember { mutableStateOf(false) }
    var videosError by remember { mutableStateOf<String?>(null) }
    var liveVideos by remember { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }

    val openUrl: (String) -> Unit = { url ->
        if (url.startsWith("http")) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    val loadNews: () -> Unit = {
        scope.launch {
            newsLoading = true
            newsError = null
            NewsAndVideoRepository.fetchLiveTechNews()
                .onSuccess { newsArticles = it }
                .onFailure { newsError = it.localizedMessage ?: "Unable to fetch live tech news." }
            newsLoading = false
        }
    }

    val loadYouTubeVideos: () -> Unit = {
        if (NewsAndVideoRepository.hasYouTubeApiKey()) {
            scope.launch {
                videosLoading = true
                videosError = null
                NewsAndVideoRepository.searchYouTubeVideos(
                    query = videoSearchQuery,
                    category = videoCategory,
                    language = videoLanguage
                ).onSuccess { liveVideos = it }
                    .onFailure { videosError = it.localizedMessage ?: "YouTube API request failed." }
                videosLoading = false
            }
        }
    }

    LaunchedEffect(activeSection) {
        if (activeSection == 1 && newsArticles.isEmpty() && !newsLoading) loadNews()
        if (activeSection == 2 && NewsAndVideoRepository.hasYouTubeApiKey() && liveVideos.isEmpty()) loadYouTubeVideos()
    }

    val totalTopics = state.learningTopics.size.coerceAtLeast(1)
    val completedTopics = state.learningTopics.count { it.completed }
    val totalStudyMin = state.learningTopics.sumOf { it.studyMinutes }
    val savedUrls = remember(state.savedMedia) { state.savedMedia.map { it.url }.toSet() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("learn_hub_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LifeOsBadgeIcon(type = LifeOsBadgeType.LEARNING, size = 44.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Learn & Career", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "20 Tech Tracks, Live News, Videos, Classes & Interviews",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Career & Schedule Quick Bar
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { onNavigate("timetable") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("learn_open_timetable_button")
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Classes (${state.classSchedules.size})")
                    }
                    FilledTonalButton(
                        onClick = { onNavigate("jobs") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("learn_open_jobs_button")
                    ) {
                        Icon(Icons.Filled.Work, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Jobs (${state.jobApplications.size})")
                    }
                }
            }

            // Separate Section Switcher: Courses | Tech News | YouTube Videos
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeSection == 0,
                        onClick = { activeSection = 0 },
                        leadingIcon = { Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Tech Hub") },
                        modifier = Modifier.testTag("learn_tab_courses")
                    )
                    FilterChip(
                        selected = activeSection == 1,
                        onClick = { activeSection = 1 },
                        leadingIcon = { Icon(Icons.Filled.Newspaper, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Tech News") },
                        modifier = Modifier.testTag("learn_tab_news")
                    )
                    FilterChip(
                        selected = activeSection == 2,
                        onClick = { activeSection = 2 },
                        leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Videos") },
                        modifier = Modifier.testTag("learn_tab_videos")
                    )
                }
            }

            // -------- TAB 0: 20-CATEGORY TECH LEARNING HUB --------
            if (activeSection == 0) {
                item {
                    GlassCard(accentTint = MaterialTheme.colorScheme.primary) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Skill Mastery Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "$completedTopics / $totalTopics done",
                                    fontFamily = JetBrainsMonoFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (completedTopics.toFloat() / totalTopics).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp))
                            )
                            Text(
                                text = "Total logged study time: $totalStudyMin min • 20 structured developer tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "All",
                                onClick = { selectedCategory = "All" },
                                label = { Text("All (20)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedCategory == "Saved",
                                onClick = { selectedCategory = "Saved" },
                                label = { Text("Saved") }
                            )
                        }
                        items(TechLearningCatalog.allCategories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "All Levels", LearningLevel.BEGINNER.name to "Beginner", LearningLevel.INTERMEDIATE.name to "Intermediate", LearningLevel.ADVANCED.name to "Advanced").forEach { (k, label) ->
                            FilterChip(selected = selectedLevel == k, onClick = { selectedLevel = k }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }

                val filteredTopics = state.learningTopics.filter { t ->
                    val matchCat = when (selectedCategory) {
                        "All" -> true
                        "Saved" -> t.saved
                        else -> t.category == selectedCategory
                    }
                    val matchLevel = selectedLevel == "ALL" || t.level == selectedLevel
                    matchCat && matchLevel
                }

                items(filteredTopics, key = { it.id }) { topic ->
                    val meta = remember(topic.title) { TechLearningCatalog.metadataFor(topic.title) }
                    GlassCard(cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleLearningTopicCompleted(topic) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (topic.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = "Toggle topic completion",
                                            tint = if (topic.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "${topic.category} • ${LearningLevel.fromNameOrDefault(topic.level).label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (meta != null) {
                                            Text(meta.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (topic.studyMinutes > 0 || topic.notes.isNotBlank()) {
                                            Text(
                                                text = buildString {
                                                    if (topic.studyMinutes > 0) append("${topic.studyMinutes} min studied")
                                                    if (topic.notes.isNotBlank()) {
                                                        if (isNotEmpty()) append(" • ")
                                                        append("Notes: ${topic.notes.take(60)}")
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.toggleLearningTopicSaved(topic) }) {
                                    Icon(
                                        imageVector = if (topic.saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Bookmark topic",
                                        tint = if (topic.saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editingTopic = topic }) { Text("Log Time / Notes") }
                                if (meta != null) {
                                    TextButton(onClick = { openUrl(meta.officialDocUrl) }) {
                                        Text("Official Docs")
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------- TAB 1: DAILY TECH NEWS (LIVE RSS FEEDS) --------
            if (activeSection == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Technology News", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Live feeds from Hacker News, InfoQ & Ars Technica", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = loadNews) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh live tech news")
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NewsAndVideoRepository.newsCategories) { cat ->
                            FilterChip(selected = newsCategory == cat, onClick = { newsCategory = cat }, label = { Text(cat) })
                        }
                    }
                }

                if (newsLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (newsError != null) {
                    item {
                        GlassCard {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Could not load live tech news", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                                Text(newsError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = loadNews) { Text("Retry") }
                            }
                        }
                    }
                } else {
                    val filteredNews = if (newsCategory == "All") newsArticles else newsArticles.filter { it.category == newsCategory }
                    if (filteredNews.isEmpty()) {
                        item {
                            GlassCard {
                                Text("No live articles in $newsCategory right now. Pull refresh or choose All.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        items(filteredNews, key = { it.id }) { article ->
                            val isSaved = article.url in savedUrls
                            GlassCard(cornerRadius = 18.dp) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${article.source} • ${article.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = article.publishedLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(article.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(article.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(onClick = { openUrl(article.url) }) {
                                            Text("Read Full Article")
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = {
                                            if (isSaved) {
                                                state.savedMedia.firstOrNull { it.url == article.url }?.let { viewModel.deleteMediaBookmark(it.id) }
                                            } else {
                                                viewModel.saveMediaBookmark(
                                                    SavedMediaEntity(
                                                        userId = state.userId,
                                                        kind = "NEWS",
                                                        title = article.title,
                                                        subtitleOrAuthor = article.source,
                                                        url = article.url,
                                                        category = article.category
                                                    )
                                                )
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                                contentDescription = "Bookmark article",
                                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------- TAB 2: YOUTUBE EDUCATIONAL VIDEOS (SEPARATE SECTION) --------
            if (activeSection == 2) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Educational YouTube Videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("English", "Tamil").forEach { lang ->
                                FilterChip(
                                    selected = videoLanguage == lang,
                                    onClick = {
                                        videoLanguage = lang
                                        if (NewsAndVideoRepository.hasYouTubeApiKey()) loadYouTubeVideos()
                                    },
                                    label = { Text(lang) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(NewsAndVideoRepository.videoCategories) { cat ->
                                FilterChip(
                                    selected = videoCategory == cat,
                                    onClick = {
                                        videoCategory = cat
                                        if (NewsAndVideoRepository.hasYouTubeApiKey()) loadYouTubeVideos()
                                    },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }

                if (NewsAndVideoRepository.hasYouTubeApiKey()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = videoSearchQuery,
                                onValueChange = { videoSearchQuery = it },
                                label = { Text("Search $videoLanguage tutorials") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = loadYouTubeVideos) { Text("Search") }
                        }
                    }
                    if (videosLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (videosError != null) {
                        item {
                            GlassCard {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(videosError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    Button(onClick = loadYouTubeVideos) { Text("Retry") }
                                }
                            }
                        }
                    } else {
                        items(liveVideos, key = { it.videoId }) { video ->
                            val isSaved = video.watchUrl in savedUrls
                            GlassCard(cornerRadius = 18.dp) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (video.thumbnailUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = video.thumbnailUrl,
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(14.dp))
                                        )
                                    }
                                    Text(video.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${video.channelTitle} • ${video.publishedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Button(onClick = {
                                            val payload = VideoDetailPayload(
                                                title = video.title,
                                                channel = video.channelTitle,
                                                publishedDate = video.publishedAt,
                                                url = video.watchUrl,
                                                thumbnailUrl = video.thumbnailUrl,
                                                category = video.category,
                                                language = video.language
                                            ).encode()
                                            onNavigate("video_detail/$payload")
                                        }) {
                                            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Preview & Watch")
                                        }
                                        IconButton(onClick = {
                                            if (isSaved) {
                                                state.savedMedia.firstOrNull { it.url == video.watchUrl }?.let { viewModel.deleteMediaBookmark(it.id) }
                                            } else {
                                                viewModel.saveMediaBookmark(
                                                    SavedMediaEntity(
                                                        userId = state.userId,
                                                        kind = "VIDEO",
                                                        title = video.title,
                                                        subtitleOrAuthor = video.channelTitle,
                                                        url = video.watchUrl,
                                                        category = video.category
                                                    )
                                                )
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                                contentDescription = "Save to Watch Later"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Honest transparent state when YOUTUBE_API_KEY isn't configured — zero fake video items!
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Live YouTube Data API Search Ready", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Add YOUTUBE_API_KEY in AI Studio Secrets to stream live YouTube API search results inside this tab. Below are verified YouTube educational course searches in $videoLanguage (no fabricated video entries).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    val matchingPortals = NewsAndVideoRepository.verifiedOfficialPortals.filter {
                        it.language.equals(videoLanguage, ignoreCase = true)
                    }
                    items(matchingPortals, key = { it.url }) { portal ->
                        GlassCard(cornerRadius = 18.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${portal.category} • ${portal.language}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(portal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(portal.channelOrSource, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(onClick = {
                                    val payload = VideoDetailPayload(
                                        title = portal.title,
                                        channel = portal.channelOrSource,
                                        publishedDate = "Verified Search Portal",
                                        url = portal.url,
                                        thumbnailUrl = "",
                                        category = portal.category,
                                        language = portal.language
                                    ).encode()
                                    onNavigate("video_detail/$payload")
                                }) {
                                    Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preview")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Study Time & Notes Dialog for a Tech Topic
    editingTopic?.let { topic ->
        var addMin by remember { mutableStateOf("30") }
        var notes by remember { mutableStateOf(topic.notes) }
        AlertDialog(
            onDismissRequest = { editingTopic = null },
            title = { Text(topic.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total studied so far: ${topic.studyMinutes} min", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = addMin,
                        onValueChange = { addMin = it.filter(Char::isDigit).take(4) },
                        label = { Text("Add study minutes") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Personal study notes") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateLearningTopicStudy(topic, addMin.toIntOrNull() ?: 0, notes)
                    editingTopic = null
                }) { Text("Save Progress") }
            },
            dismissButton = { TextButton(onClick = { editingTopic = null }) { Text("Cancel") } }
        )
    }
}
