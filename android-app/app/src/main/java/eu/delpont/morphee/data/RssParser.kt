package eu.delpont.morphee.data

import android.util.Xml
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.xmlpull.v1.XmlPullParser

/** Récupération et analyse minimale d'un flux RSS de podcast. */
object RssParser {

    data class Feed(val title: String, val episodes: List<StoredEpisode>)

    private const val MAX_EPISODES = 100

    fun fetch(feedUrl: String): Feed {
        val connection = URL(feedUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Morphee/1.0 (podcast player)")
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Réponse HTTP $code")
            }
            connection.inputStream.use { input -> return parse(input) }
        } finally {
            connection.disconnect()
        }
    }

    fun parse(input: InputStream): Feed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channelTitle = ""
        val episodes = mutableListOf<StoredEpisode>()
        var inItem = false
        var title = ""
        var audioUrl = ""
        var guid = ""
        var pubDate = ""
        var duration = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && episodes.size < MAX_EPISODES) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (val name = parser.name.lowercase()) {
                        "item" -> {
                            inItem = true
                            title = ""; audioUrl = ""; guid = ""; pubDate = ""; duration = ""
                        }
                        "enclosure" -> if (inItem && audioUrl.isBlank()) {
                            audioUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                        "title" -> if (inItem) {
                            title = parser.nextText().trim()
                        } else if (channelTitle.isBlank()) {
                            channelTitle = parser.nextText().trim()
                        }
                        "guid" -> if (inItem) guid = parser.nextText().trim()
                        "pubdate" -> if (inItem) pubDate = parser.nextText().trim()
                        "itunes:duration", "duration" -> if (inItem && duration.isBlank()) {
                            duration = parser.nextText().trim()
                        }
                        else -> { /* balise ignorée */ }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.lowercase() == "item" && inItem) {
                        inItem = false
                        if (audioUrl.isNotBlank()) {
                            episodes += StoredEpisode(
                                guid = guid.ifBlank { audioUrl },
                                title = title.ifBlank { "Épisode" },
                                audioUrl = audioUrl,
                                pubDate = pubDate,
                                durationText = duration,
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        if (episodes.isEmpty() && channelTitle.isBlank()) {
            throw IllegalStateException("Ce flux ne ressemble pas à un podcast RSS")
        }
        return Feed(channelTitle.ifBlank { "Podcast" }, episodes)
    }
}
