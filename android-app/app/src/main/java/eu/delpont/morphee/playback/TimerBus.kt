package eu.delpont.morphee.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface TimerCommand {
    data class Start(val minutes: Int) : TimerCommand
    data object Cancel : TimerCommand
    data class Extend(val minutes: Int) : TimerCommand
}

/**
 * Pont entre l'interface et le service de lecture pour le timer de sommeil.
 * L'application tourne dans un seul processus : un singleton suffit.
 */
object TimerBus {
    /** Temps restant en millisecondes, ou null si le timer est inactif. */
    val remainingMs = MutableStateFlow<Long?>(null)

    val commands = MutableSharedFlow<TimerCommand>(extraBufferCapacity = 16)

    fun send(command: TimerCommand) {
        commands.tryEmit(command)
    }
}
