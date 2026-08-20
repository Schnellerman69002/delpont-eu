package eu.delpont.morphee.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.Serializable

/** Un fichier audio de la bibliothèque locale (MediaStore). */
data class Track(
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val isPodcast: Boolean,
)

/** Piste persistée (playlists et file d'attente sauvegardée). */
@Serializable
data class StoredTrack(
    val uri: String,
    val title: String,
    val artist: String = "",
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<StoredTrack> = emptyList(),
)

/** File d'attente sauvegardée pour reprendre la lecture là où elle s'était arrêtée. */
@Serializable
data class StoredQueue(
    val tracks: List<StoredTrack>,
    val index: Int,
    val positionMs: Long,
)

fun Track.toStored() = StoredTrack(uri = uri, title = title, artist = artist)

fun StoredTrack.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(uri)
    .setUri(uri)
    .setRequestMetadata(
        MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(uri)).build()
    )
    .setMediaMetadata(
        MediaMetadata.Builder().setTitle(title).setArtist(artist).build()
    )
    .build()

fun Track.toMediaItem(): MediaItem = toStored().toMediaItem()
