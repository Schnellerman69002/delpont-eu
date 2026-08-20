package eu.delpont.morphee.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.delpont.morphee.AppViewModel
import eu.delpont.morphee.data.Track

private enum class LibraryFilter(val label: String) {
    ALL("Tout"),
    MUSIC("Musique"),
    PODCASTS("Podcasts"),
}

@Composable
fun LibraryScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }

    val filtered = when (filter) {
        LibraryFilter.ALL -> library
        LibraryFilter.MUSIC -> library.filter { !it.isPodcast }
        LibraryFilter.PODCASTS -> library.filter { it.isPodcast }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${filtered.size} fichiers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = viewModel::scanLibrary) {
                Text("Actualiser")
            }
        }

        if (filtered.isEmpty()) {
            Text(
                "Aucun fichier audio trouvé. Copiez vos musiques et podcasts " +
                    "sur le téléphone (dossiers Music, Podcasts…), puis appuyez " +
                    "sur Actualiser.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        LazyColumn {
            itemsIndexed(filtered, key = { _, track -> track.uri }) { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playTracks(filtered, index) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        val subtitle = buildString {
                            if (track.artist.isNotBlank()) append(track.artist).append(" · ")
                            append(formatDuration(track.durationMs))
                            if (track.isPodcast) append(" · Podcast")
                        }
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { trackForPlaylist = track }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Ajouter à une playlist",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }

    trackForPlaylist?.let { track ->
        Dialog(onDismissRequest = { trackForPlaylist = null }) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "Ajouter « ${track.title} » à :",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (playlists.isEmpty()) {
                        Text(
                            "Aucune playlist. Créez-en une dans l'onglet Playlists.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    playlists.forEach { playlist ->
                        Text(
                            playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addToPlaylist(playlist.id, track)
                                    trackForPlaylist = null
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                    TextButton(
                        onClick = { trackForPlaylist = null },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}
