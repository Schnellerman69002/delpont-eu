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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.delpont.morphee.AppViewModel
import eu.delpont.morphee.data.Playlist
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

    var selectionMode by remember { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<String>() }
    var pickPlaylistForSelection by remember { mutableStateOf(false) }

    val filtered = when (filter) {
        LibraryFilter.ALL -> library
        LibraryFilter.MUSIC -> library.filter { !it.isPodcast }
        LibraryFilter.PODCASTS -> library.filter { it.isPodcast }
    }

    fun exitSelection() {
        selectionMode = false
        selectedUris.clear()
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

        if (!selectionMode) {
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
                Row {
                    TextButton(
                        onClick = { selectionMode = true },
                        enabled = filtered.isNotEmpty(),
                    ) {
                        Text("Sélectionner")
                    }
                    TextButton(onClick = viewModel::scanLibrary) {
                        Text("Actualiser")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${selectedUris.size} sélectionnés",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            if (selectedUris.size == filtered.size) {
                                selectedUris.clear()
                            } else {
                                selectedUris.clear()
                                selectedUris.addAll(filtered.map { it.uri })
                            }
                        },
                    ) {
                        Text(if (selectedUris.size == filtered.size) "Aucun" else "Tous")
                    }
                    TextButton(onClick = { exitSelection() }) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { pickPlaylistForSelection = true },
                        enabled = selectedUris.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Ajouter")
                    }
                }
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
                val isSelected = track.uri in selectedUris
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectionMode) {
                                if (isSelected) selectedUris.remove(track.uri)
                                else selectedUris.add(track.uri)
                            } else {
                                viewModel.playTracks(filtered, index)
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedUris.add(track.uri)
                                else selectedUris.remove(track.uri)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
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
                    if (!selectionMode) {
                        IconButton(onClick = { trackForPlaylist = track }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Ajouter à une playlist",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }

    // Ajout d'une piste unique.
    trackForPlaylist?.let { track ->
        PlaylistPickerDialog(
            title = "Ajouter « ${track.title} » à :",
            playlists = playlists,
            onPick = { playlist ->
                viewModel.addToPlaylist(playlist.id, track)
                trackForPlaylist = null
            },
            onDismiss = { trackForPlaylist = null },
        )
    }

    // Ajout de la sélection multiple.
    if (pickPlaylistForSelection) {
        PlaylistPickerDialog(
            title = "Ajouter ${selectedUris.size} pistes à :",
            playlists = playlists,
            onPick = { playlist ->
                val tracks = filtered.filter { it.uri in selectedUris }
                viewModel.addAllToPlaylist(playlist.id, tracks)
                pickPlaylistForSelection = false
                exitSelection()
            },
            onDismiss = { pickPlaylistForSelection = false },
        )
    }
}

@Composable
private fun PlaylistPickerDialog(
    title: String,
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
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
                            .clickable { onPick(playlist) }
                            .padding(vertical = 12.dp),
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}
