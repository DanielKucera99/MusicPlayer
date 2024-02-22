package com.example.musicplayer.files

import android.media.MediaPlayer
import android.util.Log
import com.example.musicplayer.activities.PlayMode


class AudioPlayer (
    private val audioFilePaths: MutableList<String>,
    private var selectedAudioFilePath: String? = audioFilePaths.firstOrNull(),
    private var onNewAudioStarted: (String) -> Unit,
    private var onProgressUpdate: (Int) -> Unit,
    private var playMode: PlayMode
) : MediaPlayer.OnCompletionListener {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused: Boolean = false
    private var wasSeekBarMovedDuringPause: Boolean = false // New variable to track seek bar movement during pause
    private var pausePosition: Int = 0
    private var currentAudioIndex: Int = 0
    private var shuffledIndexes: MutableList<Int> = mutableListOf()
    companion object {
        private var instance: AudioPlayer? = null

        fun getInstance(): AudioPlayer {
            if (instance == null) {
                throw IllegalStateException("AudioPlayer instance has not been initialized")
            }
            return instance!!
        }

        fun setInstance(audioPlayer: AudioPlayer) {
            instance = audioPlayer
        }
    }
    private fun playCurrentAudio() {
        isPaused = false
        val audioFilePath = audioFilePaths[currentAudioIndex]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioFilePath)
            prepare()
            start()
            setOnCompletionListener(this@AudioPlayer)
        }
        onNewAudioStarted(audioFilePaths[currentAudioIndex])

        mediaPlayer?.let {
            onProgressUpdate(it.currentPosition)
        }
    }


    override fun onCompletion(mediaPlayer: MediaPlayer) {
        when (playMode) {
            PlayMode.FORWARD -> {
                currentAudioIndex = (currentAudioIndex + 1) % audioFilePaths.size
            }
            PlayMode.SHUFFLE -> {
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }

                val nextIndex = shuffledIndexes.removeAt(0)
                currentAudioIndex = nextIndex
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }

            }
            PlayMode.LOOP -> {
            }
        }
        playCurrentAudio()
        onNewAudioStarted(audioFilePaths[currentAudioIndex])
    }

     fun playNextAudioBasedOnMode() {
        when (playMode) {
            PlayMode.FORWARD -> {
                currentAudioIndex = (currentAudioIndex + 1) % audioFilePaths.size
            }
            PlayMode.SHUFFLE -> {
              if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }

                val nextIndex = shuffledIndexes.removeAt(0)
                currentAudioIndex = nextIndex
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }

            }
            PlayMode.LOOP -> {
            }
        }
        playCurrentAudio()
    }

     fun playPreviousAudioBasedOnMode() {
        when (playMode) {
            PlayMode.FORWARD -> {
                currentAudioIndex = if (currentAudioIndex == 0) {
                    audioFilePaths.size - 1
                } else {
                    currentAudioIndex - 1
                }
            }
            PlayMode.SHUFFLE -> {
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }

                val nextIndex = shuffledIndexes.removeAt(0)
                currentAudioIndex = nextIndex
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }
            }
            PlayMode.LOOP -> {

            }
        }
        playCurrentAudio()
    }
    fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
                pausePosition = it.currentPosition
                wasSeekBarMovedDuringPause = false
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let {
            if (isPaused) {
                if (wasSeekBarMovedDuringPause) {
                    onProgressUpdate(it.currentPosition)
                    wasSeekBarMovedDuringPause = false
                }
                it.start()
                isPaused = false

            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun playAudio(audioFilePath: String) {
        mediaPlayer?.release()

        val index = audioFilePaths.indexOf(audioFilePath)
        if (index != -1) {
            currentAudioIndex = index
            playCurrentAudio()
        } else {
            Log.e("AudioPlayer", "Audio file not found: $audioFilePath")
        }
    }

    fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener) {
        mediaPlayer?.setOnCompletionListener(listener)
    }
    fun setPlayMode(playMode: PlayMode){
        this.playMode = playMode
    }

    private fun shuffleAudioFilePaths() {
        shuffledIndexes = (0 until audioFilePaths.size).shuffled().toMutableList()
    }

    fun getCurrentlyPlayingFile(): String {
        return audioFilePaths[currentAudioIndex]
    }

    fun setProgressUpdateCallback(callback: ((Int) -> Unit)?) {
        onProgressUpdate = callback ?: { /* default behavior if callback is null */ }
    }
    fun setOnNewAudioStartedCallback(callback: ((String) -> Unit)?) {
        onNewAudioStarted = callback ?: { /* default behavior if callback is null */ }
    }


    fun getPlayMode(): PlayMode {
        return playMode
    }

    fun getAudioFiles(): MutableList<String> {
        return audioFilePaths
    }

    fun getSelectedAudioFilePath(): String? {
        return selectedAudioFilePath
    }

    fun getAudioCurrentState(): Boolean {
        return isPaused
    }

}

