package io.kelly.remember.audio.recorder

import android.media.MediaRecorder
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun rememberAudioRecordController(
    config: MediaRecorder.() -> Unit = {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setMaxDuration(60 * 1000)
        setMaxFileSize(1024 * 1024)
    }
): AudioRecordController {
    val scope = rememberCoroutineScope()
    val currentConfig by rememberUpdatedState(config)
    val controller = remember(scope) {
        AudioRecordControllerImpl(scope) {
            currentConfig()
        }
    }
    DisposableEffect(controller) {
        onDispose {
            controller.cancelRecord()
        }
    }
    return controller
}

@Stable
interface AudioRecordController {
    val state: StateFlow<AudioRecordState>
    fun startRecord()
    fun stopRecord()
    fun cancelRecord()
}

