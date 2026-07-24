package io.kelly.remember.audio.recorder

import kotlinx.coroutines.flow.Flow

sealed class AudioRecordState {
    data class Idle(val event: Flow<AudioRecordEvent>) : AudioRecordState()
    data object Preparing : AudioRecordState()
    data object Recording : AudioRecordState()
}

sealed class AudioRecordEvent {
    data class Complete(val duration: Long, val path: String) : AudioRecordEvent()
    data class Failed(val error: Throwable) : AudioRecordEvent()
}