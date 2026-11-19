package com.example.data

import android.util.Xml
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TechNewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val publishedLabel: String,
    val category: String,
    val url: String
)

data class YouTubeVideoItem(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val watchUrl: String,
    val category: String,
    val language: String
)

data class VerifiedLearningSearchPortal(
    val title: String,
    val channelOrSource: String,
    val category: String,
    val language: String,
    val url: String
)

/**
 * Fetches REAL live technology news from verified public RSS feeds and
 * REAL educational videos via YouTube Data API v3 when YOUTUBE_API_KEY is configured.
 * Never fabricates fake news headlines, fake creators, or placeholder videos.
 */
object NewsAndVideoRepository {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private data class RssSource(
        val name: String,
        val defaultCategory: String,
        val feedUrl: String
    )

    private val rssSources = listOf(
        RssSource("Hacker News", "Software Engineering", "https://hnrss.org/frontpage?count=15"),
        RssSource("InfoQ", "Java & Cloud", "https://feed.infoq.com/"),
        RssSource("Ars Technica", "AI & Tech", "https://feeds.arstechnica.com/arstechnica/technology-lab")
    )

    val newsCategories = listOf(
        "All", "AI & Tech", "Software Engineering", "Java & Cloud", "Cybersecurity", "Mobile & Web", "Startups"
    )

    val videoCategories = listOf(
        "Artificial Intelligence", "Machine Learning", "Java", "Spring Boot",
        "Python", "React", "DSA", "SQL & Databases", "Git & GitHub", "Java Full Stack"
    )

    val verifiedOfficialPortals: List<VerifiedLearningSearchPortal> = listOf(
        VerifiedLearningSearchPortal("Java & Spring Boot Full Tutorials", "YouTube Search (English)", "Spring Boot", "English", "https://www.youtube.com/results?search_query=spring+boot+full+course+java"),
        VerifiedLearningSearchPortal("DSA Interview Preparation", "YouTube Search (English)", "DSA", "English", "https://www.youtube.com/results?search_query=data+structures+and+algorithms+full+course"),
        VerifiedLearningSearchPortal("Python & Machine Learning Courses", "YouTube Search (English)", "Machine Learning", "English", "https://www.youtube.com/results?search_query=machine+learning+python+full+course"),
        VerifiedLearningSearchPortal("React & Modern Web Development", "YouTube Search (English)", "React", "English", "https://www.youtube.com/results?search_query=react+js+full+course+modern"),
        VerifiedLearningSearchPortal("AI & LLM Engineering Guides", "YouTube Search (English)", "Artificial Intelligence", "English", "https://www.youtube.com/results?search_query=generative+ai+llm+engineering+tutorial"),
        VerifiedLearningSearchPortal("Java Full Stack Roadmap in Tamil", "YouTube Search (Tamil)", "Java Full Stack", "Tamil", "https://www.youtube.com/results?search_query=java+full+stack+tutorial+in+tamil"),
        VerifiedLearningSearchPortal("Python & AI Tutorials in Tamil", "YouTube Search (Tamil)", "Python", "Tamil", "https://www.youtube.com/results?search_query=python+full+course+in+tamil"),
        VerifiedLearningSearchPortal("DSA & SQL Tutorials in Tamil", "YouTube Search (Tamil)", "DSA", "Tamil", "https://www.youtube.com/results?search_query=data+structures+algorithms+tamil")
    )

    suspend fun fetchLiveTechNews(): Result<List<TechNewsArticle>> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<TechNewsArticle>()
        var lastError: Exception? = null

        for (src in rssSources) {
            try {
                val request = Request.Builder()
                    .url(src.feedUrl)
                    .header("User-Agent", "LifeOS-Android/3.0")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            collected += parseRssFeed(body, src.name, src.defaultCategory)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }

        val unique = collected
            .distinctBy { it.url.ifBlank { it.title } }
            .take(30)

        if (unique.isEmpty() && lastError != null) {
            Result.failure(lastError)
        } else {
            Result.success(unique)
        }
    }

    fun hasYouTubeApiKey(): Boolean {
        val key = BuildConfig.YOUTUBE_API_KEY.trim()
        return key.isNotBlank() && key != "YOUTUBE_API_KEY_DEFAULT_VALUE"
    }

    suspend fun searchYouTubeVideos(
        query: String,
        category: String,
        language: String = "English"
    ): Result<List<YouTubeVideoItem>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
        if (!hasYouTubeApiKey()) {
            return@withContext Result.failure(
                IllegalStateException("YOUTUBE_API_KEY is not configured in AI Studio Secrets.")
            )
        }
        try {
            val langSuffix = if (language.equals("Tamil", ignoreCase = true)) " tamil tutorial" else " tutorial course"
            val q = URLEncoder.encode("${query.ifBlank { category }}$langSuffix", "UTF-8")
            val relevanceLang = if (language.equals("Tamil", ignoreCase = true)) "ta" else "en"
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=12&relevanceLanguage=$relevanceLang&q=$q&key=$apiKey"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("YouTube API returned HTTP ${resp.code}.")
                    )
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val items = json.optJSONArray("items")
                val results = mutableListOf<YouTubeVideoItem>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val obj = items.optJSONObject(i) ?: continue
                        val vid = obj.optJSONObject("id")?.optString("videoId").orEmpty()
                        val snippet = obj.optJSONObject("snippet") ?: continue
                        if (vid.isBlank()) continue
                        val thumb = snippet.optJSONObject("thumbnails")
                            ?.optJSONObject("medium")
                            ?.optString("url")
                            .orEmpty()
                        results += YouTubeVideoItem(
                            videoId = vid,
                            title = decodeBasicEntities(snippet.optString("title")),
                            channelTitle = decodeBasicEntities(snippet.optString("channelTitle")),
                            publishedAt = snippet.optString("publishedAt").take(10),
                            thumbnailUrl = thumb,
                            watchUrl = "https://www.youtube.com/watch?v=$vid",
                            category = category,
                            language = language
                        )
                    }
                }
                Result.success(results)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRssFeed(xml: String, sourceName: String, defaultCategory: String): List<TechNewsArticle> {
        val articles = mutableListOf<TechNewsArticle>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var inItem = false
            var title = ""
            var link = ""
            var description = ""
            var pubDate = ""
            var currentTag = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && articles.size < 12) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.orEmpty().lowercase()
                        if (currentTag == "item" || currentTag == "entry") {
                            inItem = true
                            title = ""
                            link = ""
                            description = ""
                            pubDate = ""
                        } else if (inItem && currentTag == "link") {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) link = href
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            val text = parser.text.orEmpty().trim()
                            if (text.isNotEmpty()) {
                                when (currentTag) {
                                    "title" -> title += text
                                    "link" -> if (link.isBlank()) link = text
                                    "description", "summary" -> description += text
                                    "pubdate", "published", "updated" -> if (pubDate.isBlank()) pubDate = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name.orEmpty().lowercase()
                        if (endTag == "item" || endTag == "entry") {
                            inItem = false
                            val cleanTitle = decodeBasicEntities(stripHtml(title)).trim()
                            val cleanDesc = decodeBasicEntities(stripHtml(description)).trim().take(220)
                            if (cleanTitle.isNotBlank() && link.startsWith("http")) {
                                articles += TechNewsArticle(
                                    id = "news_${link.hashCode()}",
                                    title = cleanTitle,
                                    summary = cleanDesc.ifBlank { "Open article on $sourceName to read the full technical report." },
                                    source = sourceName,
                                    publishedLabel = pubDate.take(16).ifBlank { "Recent" },
                                    category = inferCategory(cleanTitle, defaultCategory),
                                    url = link
                                )
                            }
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            // Return whatever valid entries were parsed
        }
        return articles
    }

    private fun inferCategory(title: String, fallback: String): String {
        val lower = title.lowercase()
        return when {
            listOf("ai", "llm", "gemini", "openai", "claude", "neural", "model", "ml").any { it in lower } -> "AI & Tech"
            listOf("security", "vulnerability", "cve", "breach", "hack", "zero-day").any { it in lower } -> "Cybersecurity"
            listOf("java", "spring", "kubernetes", "docker", "cloud", "aws", "gcp").any { it in lower } -> "Java & Cloud"
            listOf("android", "ios", "react", "web", "browser", "kotlin", "swift").any { it in lower } -> "Mobile & Web"
            listOf("startup", "funding", "ipo", "yc", "founder").any { it in lower } -> "Startups"
            else -> fallback
        }
    }

    private fun stripHtml(raw: String): String = raw.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ")

    private fun decodeBasicEntities(raw: String): String = raw
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}
