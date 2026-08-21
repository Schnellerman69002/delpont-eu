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

/** Playlists persistées dans un fichier JSON privé de l'application. */
class PlaylistRepository(context: Context) {

    private val file = File(context.applicationContext.filesDir, "playlists.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    suspend fun load() {
        mutex.withLock {
            _playlists.value = runCatching {
                if (file.exists()) json.decodeFromString<List<Playlist>>(file.readText())
                else emptyList()
            }.getOrDefault(emptyList())
        }
    }

    suspend fun create(name: String): Playlist {
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = name.trim())
        update { it + playlist }
        return playlist
    }

    suspend fun delete(playlistId: String) {
        update { list -> list.filterNot { it.id == playlistId } }
    }

    suspend fun rename(playlistId: String, name: String) {
        update { list ->
            list.map { if (it.id == playlistId) it.copy(name = name.trim()) else it }
        }
    }

    suspend fun addTrack(playlistId: String, track: StoredTrack) {
        addTracks(playlistId, listOf(track))
    }

    /** Ajoute plusieurs pistes d'un coup, en ignorant celles déjà présentes. */
    suspend fun addTracks(playlistId: String, tracks: List<StoredTrack>) {
        update { list ->
            list.map {
                if (it.id == playlistId) {
                    val existing = it.tracks.map { t -> t.uri }.toSet()
                    it.copy(tracks = it.tracks + tracks.filter { t -> t.uri !in existing })
                } else it
            }
        }
    }

    suspend fun removeTrack(playlistId: String, index: Int) {
        update { list ->
            list.map {
                if (it.id == playlistId && index in it.tracks.indices) {
                    it.copy(tracks = it.tracks.toMutableList().apply { removeAt(index) })
                } else it
            }
        }
    }

    suspend fun moveTrack(playlistId: String, from: Int, to: Int) {
        update { list ->
            list.map {
                if (it.id == playlistId && from in it.tracks.indices && to in it.tracks.indices) {
                    it.copy(
                        tracks = it.tracks.toMutableList().apply { add(to, removeAt(from)) }
                    )
                } else it
            }
        }
    }

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        mutex.withLock {
            val updated = transform(_playlists.value)
            _playlists.value = updated
            runCatching { file.writeText(json.encodeToString(updated)) }
        }
    }
}
