package eu.delpont.morphee.data

import android.content.Context
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Abonnements podcasts persistés dans un fichier JSON privé de l'application. */
class PodcastRepository(context: Context) {

    private val file = File(context.applicationContext.filesDir, "podcasts.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions

    suspend fun load() {
        mutex.withLock {
            _subscriptions.value = runCatching {
                if (file.exists()) json.decodeFromString<List<Subscription>>(file.readText())
                else emptyList()
            }.getOrDefault(emptyList())
        }
    }

    /** Récupère le flux et ajoute l'abonnement. Lève une exception si le flux est illisible. */
    suspend fun subscribe(feedUrl: String): Subscription {
        val feed = RssParser.fetch(feedUrl.trim())
        val subscription = Subscription(
            id = UUID.randomUUID().toString(),
            title = feed.title,
            feedUrl = feedUrl.trim(),
            episodes = feed.episodes,
        )
        update { list ->
            if (list.any { it.feedUrl == subscription.feedUrl }) list else list + subscription
        }
        return subscription
    }

    /** Recharge le flux en conservant l'état « téléchargé » des épisodes connus. */
    suspend fun refresh(subscriptionId: String) {
        val subscription = _subscriptions.value.firstOrNull { it.id == subscriptionId } ?: return
        val feed = RssParser.fetch(subscription.feedUrl)
        update { list ->
            list.map { sub ->
                if (sub.id == subscriptionId) {
                    val downloadedByGuid = sub.episodes
                        .filter { it.downloadedUri != null }
                        .associateBy { it.guid }
                    sub.copy(
                        title = feed.title,
                        episodes = feed.episodes.map { episode ->
                            downloadedByGuid[episode.guid]
                                ?.let { episode.copy(downloadedUri = it.downloadedUri) }
                                ?: episode
                        },
                    )
                } else sub
            }
        }
    }

    suspend fun unsubscribe(subscriptionId: String) {
        update { list -> list.filterNot { it.id == subscriptionId } }
    }

    suspend fun setDownloadedUri(subscriptionId: String, guid: String, downloadedUri: String?) {
        update { list ->
            list.map { sub ->
                if (sub.id == subscriptionId) {
                    sub.copy(
                        episodes = sub.episodes.map { episode ->
                            if (episode.guid == guid) episode.copy(downloadedUri = downloadedUri)
                            else episode
                        },
                    )
                } else sub
            }
        }
    }

    private suspend fun update(transform: (List<Subscription>) -> List<Subscription>) {
        mutex.withLock {
            val updated = transform(_subscriptions.value)
            _subscriptions.value = updated
            runCatching { file.writeText(json.encodeToString(updated)) }
        }
    }
}
