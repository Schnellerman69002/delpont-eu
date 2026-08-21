package eu.delpont.morphee.data

import kotlinx.serialization.Serializable

/** Épisode d'un flux RSS ; downloadedUri est renseigné une fois téléchargé. */
@Serializable
data class StoredEpisode(
    val guid: String,
    val title: String,
    val audioUrl: String,
    val pubDate: String = "",
    val durationText: String = "",
    val downloadedUri: String? = null,
)

/** Abonnement à un podcast (flux RSS) avec ses derniers épisodes connus. */
@Serializable
data class Subscription(
    val id: String,
    val title: String,
    val feedUrl: String,
    val episodes: List<StoredEpisode> = emptyList(),
)
