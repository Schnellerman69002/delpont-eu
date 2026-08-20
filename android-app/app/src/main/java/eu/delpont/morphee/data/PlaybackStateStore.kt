package eu.delpont.morphee.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Sauvegarde de la file d'attente et de la position de lecture,
 * pour reprendre exactement là où la playlist s'était arrêtée
 * (y compris après un appui sur Play depuis la télécommande Bluetooth,
 * application fermée).
 */
class PlaybackStateStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "queue.json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(queue: StoredQueue) {
        mutex.withLock {
            runCatching { file.writeText(json.encodeToString(queue)) }
        }
    }

    suspend fun load(): StoredQueue? = mutex.withLock {
        runCatching {
            if (file.exists()) json.decodeFromString<StoredQueue>(file.readText()) else null
        }.getOrNull()
    }

    companion object {
        // Un seul verrou pour toutes les instances (service + interface).
        private val mutex = Mutex()
    }
}
