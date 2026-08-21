package eu.delpont.morphee.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Téléchargement d'un épisode dans le dossier public Podcasts/ du téléphone,
 * via MediaStore : le fichier apparaît ensuite dans la Bibliothèque de
 * l'application (et dans tout autre lecteur).
 */
object EpisodeDownloader {

    fun download(context: Context, podcastTitle: String, episode: StoredEpisode): String {
        val connection = URL(episode.audioUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Morphee/1.0 (podcast player)")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Réponse HTTP $code")
            val mime = connection.contentType?.substringBefore(';')?.trim()
                ?.takeIf { it.startsWith("audio/") } ?: "audio/mpeg"
            val extension = when (mime) {
                "audio/mp4", "audio/x-m4a", "audio/aac" -> "m4a"
                "audio/ogg" -> "ogg"
                "audio/opus" -> "opus"
                else -> "mp3"
            }
            val fileName = sanitize(episode.title).take(60).ifBlank { "episode" } + ".$extension"
            val folder = sanitize(podcastTitle).take(40).ifBlank { "Podcast" }

            return connection.inputStream.use { input ->
                if (Build.VERSION.SDK_INT >= 29) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.MIME_TYPE, mime)
                        put(
                            MediaStore.Audio.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PODCASTS + "/" + folder,
                        )
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Création du fichier impossible")
                    try {
                        resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                            ?: throw IllegalStateException("Écriture du fichier impossible")
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        throw e
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri.toString()
                } else {
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PODCASTS,
                        ),
                        folder,
                    )
                    dir.mkdirs()
                    val file = File(dir, fileName)
                    file.outputStream().use { output -> input.copyTo(output) }
                    MediaScannerConnection.scanFile(
                        context, arrayOf(file.absolutePath), arrayOf(mime), null,
                    )
                    Uri.fromFile(file).toString()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun delete(context: Context, downloadedUri: String) {
        runCatching {
            val uri = Uri.parse(downloadedUri)
            if (uri.scheme == "file") {
                uri.path?.let { File(it).delete() }
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\n\\r]"), " ").trim()
}
