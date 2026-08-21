package eu.delpont.morphee.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
fun PodcastsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val busy by viewModel.podcastBusy.collectAsStateWithLifecycle()
    val message by viewModel.podcastMessage.collectAsStateWithLifecycle()
    val downloading by viewModel.downloadingGuids.collectAsStateWithLifecycle()
    var feedUrl by remember { mutableStateOf("") }
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
                    value = feedUrl,
                    onValueChange = { feedUrl = it },
                    label = { Text("URL du flux RSS") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Button(
                    onClick = {
                        viewModel.subscribePodcast(feedUrl)
                        feedUrl = ""
                    },
                    enabled = feedUrl.isNotBlank() && !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("S'abonner")
                }
            }
        }

        if (busy) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        message?.let { text ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::clearPodcastMessage) { Text("OK") }
                }
            }
        }

        if (subscriptions.isEmpty() && !busy) {
            item {
                Text(
                    "Collez l'URL du flux RSS d'un podcast (disponible sur le site " +
                        "de chaque podcast) puis appuyez sur S'abonner. Les épisodes " +
                        "téléchargés apparaissent dans la Bibliothèque et peuvent " +
                        "être ajoutés aux playlists.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        itemsIndexed(subscriptions, key = { _, s -> s.id }) { _, subscription ->
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
                                expandedId =
                                    if (expandedId == subscription.id) null else subscription.id
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                subscription.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val downloadedCount =
                                subscription.episodes.count { it.downloadedUri != null }
                            Text(
                                "${subscription.episodes.size} épisodes · " +
                                    "$downloadedCount téléchargés",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshPodcast(subscription.id) },
                            enabled = !busy,
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Actualiser le flux",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { viewModel.unsubscribePodcast(subscription.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Se désabonner",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (expandedId == subscription.id) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        subscription.episodes.forEach { episode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        episode.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    val subtitle = buildString {
                                        if (episode.pubDate.isNotBlank()) {
                                            append(episode.pubDate.take(16))
                                        }
                                        if (episode.durationText.isNotBlank()) {
                                            if (isNotEmpty()) append(" · ")
                                            append(episode.durationText)
                                        }
                                        if (episode.downloadedUri != null) {
                                            if (isNotEmpty()) append(" · ")
                                            append("Téléchargé")
                                        }
                                    }
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                when {
                                    episode.guid in downloading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    episode.downloadedUri != null -> {
                                        IconButton(
                                            onClick = { viewModel.playEpisode(episode) },
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Lire l'épisode",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteEpisodeDownload(
                                                    subscription, episode,
                                                )
                                            },
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Supprimer le fichier",
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                    else -> {
                                        IconButton(
                                            onClick = {
                                                viewModel.downloadEpisode(subscription, episode)
                                            },
                                        ) {
                                            Icon(
                                                Icons.Filled.Download,
                                                contentDescription = "Télécharger l'épisode",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
