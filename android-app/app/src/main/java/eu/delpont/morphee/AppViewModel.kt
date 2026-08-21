package eu.delpont.morphee

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import eu.delpont.morphee.data.MediaLibrary
import eu.delpont.morphee.data.PlaybackStateStore
import eu.delpont.morphee.data.Playlist
import eu.delpont.morphee.data.PlaylistRepository
import eu.delpont.morphee.data.SettingsRepository
import eu.delpont.morphee.data.Track
import eu.delpont.morphee.data.toMediaItem
import eu.delpont.morphee.data.toStored
import eu.delpont.morphee.playback.PlaybackService
import eu.delpont.morphee.playback.TimerBus
import eu.delpont.morphee.playback.TimerCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val playlistRepo = PlaylistRepository(app)
    private val settingsRepo = SettingsRepository.get(app)
    private val stateStore = PlaybackStateStore(app)

    private var controller: MediaController? = null

    val playlists: StateFlow<List<Playlist>> = playlistRepo.playlists

    val settings: StateFlow<SettingsRepository.AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.AppSettings())

    val timerRemainingMs: StateFlow<Long?> = TimerBus.remainingMs

    private val _library = MutableStateFlow<List<Track>>(emptyList())
    val library: StateFlow<List<Track>> = _library

    private val _nowTitle = MutableStateFlow("")
    val nowTitle: StateFlow<String> = _nowTitle

    private val _nowArtist = MutableStateFlow("")
    val nowArtist: StateFlow<String> = _nowArtist

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _hasQueue = MutableStateFlow(false)
    val hasQueue: StateFlow<Boolean> = _hasQueue

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    init {
        viewModelScope.launch(Dispatchers.IO) { playlistRepo.load() }

        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener(
            {
                runCatching { onControllerReady(future.get()) }
            },
            ContextCompat.getMainExecutor(app),
        )

        viewModelScope.launch {
            while (isActive) {
                delay(500)
                controller?.let { c ->
                    _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = if (c.duration == C.TIME_UNSET) 0L else c.duration
                }
            }
        }
    }

    private fun onControllerReady(c: MediaController) {
        controller = c
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateNowPlaying(c)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNowPlaying(c)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                _hasQueue.value = c.mediaItemCount > 0
            }
        })
        _isPlaying.value = c.isPlaying
        _hasQueue.value = c.mediaItemCount > 0
        updateNowPlaying(c)
        if (c.mediaItemCount == 0) restoreSavedQueue(c)
    }

    private fun updateNowPlaying(c: MediaController) {
        val metadata = c.currentMediaItem?.mediaMetadata
        _nowTitle.value = metadata?.title?.toString() ?: ""
        _nowArtist.value = metadata?.artist?.toString() ?: ""
    }

    /** Recharge la dernière file (en pause) pour que « Play » reprenne où on s'était arrêté. */
    private fun restoreSavedQueue(c: MediaController) {
        viewModelScope.launch {
            val queue = withContext(Dispatchers.IO) { stateStore.load() } ?: return@launch
            if (queue.tracks.isEmpty() || c.mediaItemCount > 0) return@launch
            c.setMediaItems(
                queue.tracks.map { it.toMediaItem() },
                queue.index.coerceIn(0, queue.tracks.size - 1),
                queue.positionMs,
            )
            c.prepare()
        }
    }

    // ------------------------------------------------------------------
    // Bibliothèque
    // ------------------------------------------------------------------

    fun scanLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            _library.value = MediaLibrary.scan(getApplication())
        }
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    fun playPlaylist(playlist: Playlist, startIndex: Int = 0) {
        val c = controller ?: return
        if (playlist.tracks.isEmpty()) return
        c.setMediaItems(playlist.tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else if (c.mediaItemCount > 0) {
            c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun stopNow() {
        controller?.pause()
    }

    // ------------------------------------------------------------------
    // Playlists
    // ------------------------------------------------------------------

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { playlistRepo.create(name) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) { playlistRepo.delete(playlistId) }
    }

    fun addToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch(Dispatchers.IO) { playlistRepo.addTrack(playlistId, track.toStored()) }
    }

    fun addAllToPlaylist(playlistId: String, tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepo.addTracks(playlistId, tracks.map { it.toStored() })
        }
    }

    fun removeFromPlaylist(playlistId: String, index: Int) {
        viewModelScope.launch(Dispatchers.IO) { playlistRepo.removeTrack(playlistId, index) }
    }

    fun moveInPlaylist(playlistId: String, from: Int, to: Int) {
        viewModelScope.launch(Dispatchers.IO) { playlistRepo.moveTrack(playlistId, from, to) }
    }

    // ------------------------------------------------------------------
    // Timer
    // ------------------------------------------------------------------

    fun setTimerMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepo.setTimerMinutes(minutes) }
    }

    fun setAutoTimer(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoTimer(enabled) }
    }

    fun setFadeSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepo.setFadeSeconds(seconds) }
    }

    fun startTimerNow() {
        TimerBus.send(TimerCommand.Start(settings.value.timerMinutes))
    }

    fun extendTimer(minutes: Int) {
        TimerBus.send(TimerCommand.Extend(minutes))
    }

    fun cancelTimer() {
        TimerBus.send(TimerCommand.Cancel)
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
