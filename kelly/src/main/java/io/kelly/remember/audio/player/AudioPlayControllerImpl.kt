package io.kelly.remember.audio.player

import android.media.MediaPlayer
import io.kelly.extension.suspendResult
import io.kelly.extension.tryResult
import io.kelly.flow.effectProperty
import io.kelly.flow.stateProperty
import io.kelly.source.Source
import io.kelly.source.fetch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AudioPlayControllerImpl(
    private val scope: CoroutineScope,
    private val config: MediaPlayer.() -> Unit = {
        isLooping = false
    }
) : AudioPlayController {

    private var _player: MediaPlayer? = null

    private var _playJob: Job? = null


    private val _event = effectProperty<AudioPlayEvent>()

    private val event by _event

    private val _state = stateProperty<AudioPlayState>(AudioPlayState.Idle(event))

    override val state by _state

    override fun <T> startPlay(source: Source<T>) {
        _playJob?.cancel()
        cleanupInternal()
        _playJob = scope.launch {
            tryResult {
                _state.value = AudioPlayState.Preparing
                val player = MediaPlayer()
                _player = player
                player.config()
                val file = suspendResult {
                    source.fetch()
                }.getOrThrow()
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    _event.trySend(AudioPlayEvent.Complete)
                    stopPlay()
                }
                player.setOnErrorListener { _, what, extra ->
                    cleanupInternal()
                    _event.trySend(AudioPlayEvent.Failed(Exception("what:${what} extra=${extra}")))
                    _state.value = AudioPlayState.Idle(event)
                    true
                }
                withContext(Dispatchers.IO) {
                    player.prepare()
                }
                player.start()
                _state.value = AudioPlayState.Playing
            }.onFailure {
                cleanupInternal()
                _event.trySend(AudioPlayEvent.Failed(it))
                _state.value = AudioPlayState.Idle(event)
            }
        }
    }

    override fun stopPlay() {
        if (_state.value is AudioPlayState.Idle) return
        _playJob?.cancel()
        cleanupInternal()
        _state.value = AudioPlayState.Idle(event)
    }

    private fun cleanupInternal() {
        _player?.let { player ->
            player.setOnErrorListener(null)
            player.setOnCompletionListener(null)
            tryResult {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            tryResult {
                player.release()
            }
        }
        _player = null
    }
}