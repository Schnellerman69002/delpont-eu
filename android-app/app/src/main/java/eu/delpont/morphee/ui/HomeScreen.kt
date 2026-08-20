package eu.delpont.morphee.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.delpont.morphee.AppViewModel

@Composable
fun HomeScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val nowTitle by viewModel.nowTitle.collectAsStateWithLifecycle()
    val nowArtist by viewModel.nowArtist.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val hasQueue by viewModel.hasQueue.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val timerRemaining by viewModel.timerRemainingMs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ----- Piste en cours -----
        Text(
            text = when {
                nowTitle.isNotBlank() -> nowTitle
                hasQueue -> "Prêt à reprendre"
                else -> "Aucune lecture en cours"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        if (nowArtist.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = nowArtist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))

        // ----- Progression -----
        LinearProgressIndicator(
            progress = {
                if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.bodyLarge)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(24.dp))

        // ----- Contrôles -----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            IconButton(onClick = viewModel::previous, modifier = Modifier.size(72.dp)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Piste précédente",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            FilledIconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier.size(112.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Lecture",
                    modifier = Modifier.size(72.dp),
                )
            }
            IconButton(onClick = viewModel::next, modifier = Modifier.size(72.dp)) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Piste suivante",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ----- Timer de sommeil -----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Timer de sommeil",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                val remaining = timerRemaining
                if (remaining != null) {
                    Text(
                        formatDuration(remaining),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { viewModel.extendTimer(5) }) {
                            Text("+5 min")
                        }
                        OutlinedButton(onClick = viewModel::cancelTimer) {
                            Text("Annuler")
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = { viewModel.setTimerMinutes(settings.timerMinutes - 5) },
                        ) {
                            Text("−5", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "${settings.timerMinutes} min",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(
                            onClick = { viewModel.setTimerMinutes(settings.timerMinutes + 5) },
                        ) {
                            Text("+5", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isPlaying) {
                        OutlinedButton(onClick = viewModel::startTimerNow) {
                            Text("Lancer le timer")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Automatique à chaque lecture",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.autoTimer,
                        onCheckedChange = viewModel::setAutoTimer,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Fondu : ${settings.fadeSeconds} s",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.setFadeSeconds(settings.fadeSeconds - 5) }) {
                        Text("−5")
                    }
                    TextButton(onClick = { viewModel.setFadeSeconds(settings.fadeSeconds + 5) }) {
                        Text("+5")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Le bouton Play de la télécommande Bluetooth reprend la lecture " +
                "là où elle s'était arrêtée et relance le timer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
