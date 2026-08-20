package eu.delpont.morphee.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/** Analyse de la bibliothèque audio locale via MediaStore. */
object MediaLibrary {

    fun scan(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.DATA,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
                "${MediaStore.Audio.Media.IS_ALARM} = 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val podcastCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_PODCAST)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val path = cursor.getString(dataCol) ?: ""
                    val isPodcast = cursor.getInt(podcastCol) != 0 ||
                        path.contains("podcast", ignoreCase = true)
                    val artist = cursor.getString(artistCol)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                        ?: ""
                    tracks += Track(
                        uri = uri,
                        title = cursor.getString(titleCol) ?: "Sans titre",
                        artist = artist,
                        durationMs = cursor.getLong(durationCol),
                        isPodcast = isPodcast,
                    )
                }
            }
        return tracks
    }
}
