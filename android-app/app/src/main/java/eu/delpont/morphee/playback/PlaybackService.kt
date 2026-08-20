package eu.delpont.morphee.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import eu.delpont.morphee.MainActivity
import eu.delpont.morphee.data.PlaybackStateStore
import eu.delpont.morphee.data.SettingsRepository
import eu.delpont.morphee.data.StoredQueue
import eu.delpont.morphee.data.StoredTrack
import eu.delpont.morphee.data.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Service de lecture basé sur Media3 :
 * - MediaSession → boutons de la télécommande Bluetooth (play/pause, suivant,
 *   précédent) et volume absolu gérés par le système ;
 * - reprise de la lecture (file + position) même après fermeture de
 *   l'application, via onPlaybackResumption ;
 * - timer de sommeil : armé automatiquement à chaque lancement de lecture,
 *   fondu du volume puis pause à l'expiration, position sauvegardée.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private lateinit var stateStore: PlaybackStateStore
    private lateinit var settings: SettingsRepository

    private var timerJob: Job? = null
    private var remainingMs = 0L
    private var fadingOut = false

    override fun onCreate() {
        super.onCreate()
        stateStore = PlaybackStateStore(this)
        settings = SettingsRepository.get(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    armTimerFromSettings()
                } else {
                    if (!fadingOut) cancelTimer()
                    saveState()
                }
            }
        })

        // Sauvegarde périodique de la position pendant la lecture.
        serviceScope.launch {
            while (isActive) {
                delay(5_000)
                if (mediaSession?.player?.isPlaying == true) saveState()
            }
        }

        // Commandes du timer envoyées par l'interface.
        serviceScope.launch {
            TimerBus.commands.collect { command ->
                when (command) {
                    is TimerCommand.Start -> startTimer(command.minutes * 60_000L)
                    is TimerCommand.Extend -> extendTimer(command.minutes * 60_000L)
                    TimerCommand.Cancel -> cancelTimer()
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            buildStoredQueue(session.player)?.let { queue ->
                runBlocking { stateStore.save(queue) }
            }
            session.player.release()
            session.release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Timer de sommeil
    // ------------------------------------------------------------------

    private fun armTimerFromSettings() {
        serviceScope.launch {
            val current = settings.settings.first()
            if (current.autoTimer) startTimer(current.timerMinutes * 60_000L)
        }
    }

    private fun startTimer(durationMs: Long) {
        remainingMs = durationMs
        if (timerJob?.isActive == true) {
            TimerBus.remainingMs.value = remainingMs
            return
        }
        timerJob = serviceScope.launch {
            try {
                while (remainingMs > 0) {
                    TimerBus.remainingMs.value = remainingMs
                    delay(1_000)
                    remainingMs -= 1_000
                }
                TimerBus.remainingMs.value = 0
                fadeOutAndPause()
            } finally {
                TimerBus.remainingMs.value = null
            }
        }
    }

    private fun extendTimer(extraMs: Long) {
        if (timerJob?.isActive == true) {
            remainingMs += extraMs
            TimerBus.remainingMs.value = remainingMs
        } else {
            startTimer(extraMs)
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        TimerBus.remainingMs.value = null
    }

    private suspend fun fadeOutAndPause() {
        val player = mediaSession?.player ?: return
        if (!player.isPlaying) return
        fadingOut = true
        val startVolume = player.volume
        try {
            val fadeSeconds = settings.settings.first().fadeSeconds
            if (fadeSeconds > 0) {
                val steps = 30
                val stepMs = (fadeSeconds * 1_000L / steps).coerceAtLeast(50L)
                for (i in steps - 1 downTo 0) {
                    player.volume = startVolume * i / steps
                    delay(stepMs)
                }
            }
            player.pause()
        } finally {
            player.volume = startVolume
            fadingOut = false
        }
        saveState()
    }

    // ------------------------------------------------------------------
    // Sauvegarde de l'état de lecture
    // ------------------------------------------------------------------

    private fun buildStoredQueue(player: Player): StoredQueue? {
        val count = player.mediaItemCount
        if (count == 0) return null
        val tracks = (0 until count).map { i ->
            val item = player.getMediaItemAt(i)
            StoredTrack(
                uri = item.mediaId,
                title = item.mediaMetadata.title?.toString() ?: "",
                artist = item.mediaMetadata.artist?.toString() ?: "",
            )
        }
        return StoredQueue(
            tracks = tracks,
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
        )
    }

    private fun saveState() {
        val player = mediaSession?.player ?: return
        val queue = buildStoredQueue(player) ?: return
        serviceScope.launch(Dispatchers.IO) { stateStore.save(queue) }
    }

    // ------------------------------------------------------------------
    // Callback de session
    // ------------------------------------------------------------------

    private inner class SessionCallback : MediaSession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    val uri = item.requestMetadata.mediaUri ?: Uri.parse(item.mediaId)
                    item.buildUpon().setUri(uri).build()
                }
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }

        /**
         * Appelé quand le système veut reprendre la lecture (bouton Play de la
         * télécommande Bluetooth alors que l'application était fermée) :
         * on recharge la file sauvegardée et on reprend à la position exacte.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                val queue = withContext(Dispatchers.IO) { stateStore.load() }
                if (queue == null || queue.tracks.isEmpty()) {
                    future.setException(IllegalStateException("Aucune lecture à reprendre"))
                } else {
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            queue.tracks.map { it.toMediaItem() },
                            queue.index.coerceIn(0, queue.tracks.size - 1),
                            queue.positionMs,
                        )
                    )
                }
            }
            return future
        }
    }
}
