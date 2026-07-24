package io.kelly.remember.audio.recorder

import android.media.MediaRecorder
import android.os.Build
import io.kelly.KellyInternal
import io.kelly.extension.tryResult
import io.kelly.flow.effectProperty
import io.kelly.flow.stateProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

internal class AudioRecordControllerImpl(
    private val scope: CoroutineScope,
    private val config: MediaRecorder.() -> Unit = {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setMaxDuration(60 * 1000)
        setMaxFileSize(1024 * 1024)
    }
) : AudioRecordController {

    private var _recorder: MediaRecorder? = null

    private var _recordJob: Job? = null

    private val _event = effectProperty<AudioRecordEvent>()

    private val event by _event

    private val _state = stateProperty<AudioRecordState>(AudioRecordState.Idle(event))
    override val state by _state

    private var _file: File? = null

    private var _startTimeMillis: Long = 0L

    override fun startRecord() {
        _recordJob?.cancel()
        cleanupInternal()
        _recordJob = scope.launch {
            tryResult {
                _state.value = AudioRecordState.Preparing
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(KellyInternal.context)
                } else {
                    MediaRecorder()
                }
                _recorder = recorder
                recorder.config()
                val file = File(KellyInternal.context.cacheDir, "${System.currentTimeMillis()}.aac")
                _file = file
                recorder.setOutputFile(file.path)
                recorder.setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        finishRecord(cancel = false)
                    }
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        finishRecord(cancel = false)
                    }
                }
                recorder.setOnErrorListener { _, what, extra ->
                    cleanupInternal()
                    _file?.delete()
                    _event.trySend(AudioRecordEvent.Failed(Exception("what:${what} extra=${extra}")))
                    _state.value = AudioRecordState.Idle(event)
                }
                withContext(Dispatchers.IO) {
                    recorder.prepare()
                }
                recorder.start()
                _startTimeMillis = System.currentTimeMillis()
                _state.value = AudioRecordState.Recording
            }.onFailure {
                cleanupInternal()
                _file?.delete()
                _event.trySend(AudioRecordEvent.Failed(it))
                _state.value = AudioRecordState.Idle(event)
            }
        }
    }

    override fun stopRecord() {
        finishRecord(false)
    }

    override fun cancelRecord() {
        finishRecord(true)
    }

    private fun finishRecord(cancel: Boolean) {
        if (_state.value is AudioRecordState.Idle) return
        _recordJob?.cancel()
        cleanupInternal()
        val wasRecording = _state.value == AudioRecordState.Recording
        val duration = if (wasRecording) System.currentTimeMillis() - _startTimeMillis else 0L
        if (cancel) {
            _file?.delete()
            _event.trySend(AudioRecordEvent.Failed(RuntimeException("Recording cancel")))
        } else if (!wasRecording) {
            _file?.delete()
            _event.trySend(AudioRecordEvent.Failed(RuntimeException(" No recording")))
        } else if (duration < 1000) {
            _file?.delete()
            _event.trySend(AudioRecordEvent.Failed(RuntimeException("Recording duration too short")))
        } else if (_file?.exists() != true) {
            _file?.delete()
            _event.trySend(AudioRecordEvent.Failed(FileNotFoundException("file is not exist")))
        } else {
            _event.trySend(
                AudioRecordEvent.Complete(
                    duration = duration,
                    path = _file!!.absolutePath
                )
            )
        }
        _startTimeMillis = 0L
        _file = null
        _state.value = AudioRecordState.Idle(event)
    }

    private fun cleanupInternal() {
        _recorder?.let { recorder ->
            recorder.setOnErrorListener(null)
            recorder.setOnInfoListener(null)
            tryResult {
                recorder.stop()
            }
            tryResult {
                recorder.release()
            }
        }
        _recorder = null
    }
}