package io.kelly.remember.audio.player

import android.media.MediaPlayer
import androidx.compose.runtime.*
import io.kelly.source.Source
import kotlinx.coroutines.flow.StateFlow

@Composable
fun rememberAudioPlayController(
    config: MediaPlayer.() -> Unit = {
        isLooping = false
    }
): AudioPlayController {
    val scope = rememberCoroutineScope()
    val currentConfig by rememberUpdatedState(config)
    val controller = remember(scope) {
        AudioPlayControllerImpl(scope) {
            currentConfig()
        }
    }
    DisposableEffect(controller) {
        onDispose {
            controller.stopPlay()
        }
    }
    return controller
}

@Stable
interface AudioPlayController {
    val state: StateFlow<AudioPlayState>
    fun<T> startPlay(source: Source<T>)
    fun stopPlay()
}


