package io.kelly.remember.audio.player

import kotlinx.coroutines.flow.Flow

sealed class AudioPlayState {
    data class Idle(val event: Flow<AudioPlayEvent>) : AudioPlayState()
    data object Preparing : AudioPlayState()
    data object Playing : AudioPlayState()
}

sealed class AudioPlayEvent {
    data object Complete : AudioPlayEvent()
    data class Failed(val error: Throwable) : AudioPlayEvent()
}