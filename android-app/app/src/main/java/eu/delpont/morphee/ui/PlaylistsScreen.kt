package eu.delpont.morphee.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.delpont.morphee.AppViewModel

@Composable
fun PlaylistsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nouvelle playlist") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Button(
                    onClick = {
                        viewModel.createPlaylist(newName)
                        newName = ""
                    },
                    enabled = newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Créer")
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Text(
                    "Créez une playlist, puis ajoutez-y des fichiers depuis " +
                        "l'onglet Bibliothèque.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedId = if (expandedId == playlist.id) null else playlist.id
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                playlist.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${playlist.tracks.size} pistes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.playPlaylist(playlist) },
                            enabled = playlist.tracks.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Lire la playlist",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Supprimer la playlist",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (expandedId == playlist.id) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        playlist.tracks.forEachIndexed { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.playPlaylist(playlist, index) },
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.moveInPlaylist(playlist.id, index, index - 1)
                                    },
                                    enabled = index > 0,
                                ) {
                                    Icon(
                                        Icons.Filled.KeyboardArrowUp,
                                        contentDescription = "Monter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.moveInPlaylist(playlist.id, index, index + 1)
                                    },
                                    enabled = index < playlist.tracks.size - 1,
                                ) {
                                    Icon(
                                        Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Descendre",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.removeFromPlaylist(playlist.id, index)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Retirer",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        if (playlist.tracks.isEmpty()) {
                            Text(
                                "Playlist vide — ajoutez des pistes depuis la Bibliothèque.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
