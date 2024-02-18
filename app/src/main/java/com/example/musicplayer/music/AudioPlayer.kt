package com.example.musicplayer.music

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException



class AudioPlayer(
    private val audioFilePaths: MutableList<String>,
    private var selectedAudioFilePath: String? = audioFilePaths.firstOrNull(),
    private val onNewAudioStarted: (String) -> Unit,
    private val onProgressUpdate: (Int) -> Unit,
    private var playMode: PlayMode

) : MediaPlayer.OnCompletionListener {
    private var mediaPlayer: MediaPlayer? = null
    private var isPaused: Boolean = false
    private var wasSeekBarMovedDuringPause: Boolean = false // New variable to track seek bar movement during pause
    private var pausePosition: Int = 0
    private var currentAudioIndex: Int = 0
    private var shuffledIndexes: MutableList<Int> = mutableListOf()

    private fun playCurrentAudio() {
        val audioFilePath = audioFilePaths[currentAudioIndex]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioFilePath)
            prepare()
            start()
            setOnCompletionListener(this@AudioPlayer)
        }
        // Notify the callback function that a new audio has started
        onNewAudioStarted(audioFilePaths[currentAudioIndex])

        mediaPlayer?.let {
            onProgressUpdate(it.currentPosition)
        }
    }


    override fun onCompletion(mediaPlayer: MediaPlayer) {
        // Move to the next audio file or restart playback from the beginning
        currentAudioIndex = (currentAudioIndex + 1) % audioFilePaths.size
        playCurrentAudio()
        // Call the callback to update the current song button
        onNewAudioStarted(audioFilePaths[currentAudioIndex])
    }

     fun playNextAudioBasedOnMode() {
        when (playMode) {
            PlayMode.FORWARD -> {
                // Increment the current index to play the next audio file
                currentAudioIndex = (currentAudioIndex + 1) % audioFilePaths.size
            }
            PlayMode.SHUFFLE -> {
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }

                // Get the next index from the shuffled list
                val nextIndex = shuffledIndexes.removeAt(0)
                currentAudioIndex = nextIndex
                if (shuffledIndexes.isEmpty()) {
                    shuffleAudioFilePaths()
                }
            }
            PlayMode.LOOP -> {
                // Keep the current index to loop the current audio file
            }
        }
        playCurrentAudio()
    }

    // Function to play the previous audio file based on the play mode
     fun playPreviousAudioBasedOnMode() {
        when (playMode) {
            PlayMode.FORWARD -> {
                // Decrement the current index to play the previous audio file
                currentAudioIndex = if (currentAudioIndex == 0) {
                    audioFilePaths.size - 1 // If at the first index, move to the last index
                } else {
                    currentAudioIndex - 1
                }
            }
            PlayMode.SHUFFLE -> {
                // Pick a random index to play the previous audio file
                currentAudioIndex = (currentAudioIndex + (0 until audioFilePaths.size).random()) % audioFilePaths.size
            }
            PlayMode.LOOP -> {
                // Keep the current index to loop the current audio file
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
                wasSeekBarMovedDuringPause = false // Reset the flag when audio is paused
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let {
            if (isPaused) {
                if (wasSeekBarMovedDuringPause) {
                    onProgressUpdate(it.currentPosition)
                    wasSeekBarMovedDuringPause = false // Reset the flag after updating the seek bar
                }
                it.start()
                isPaused = false
                // Update the seek bar if it was moved during the pause state

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

    fun playNextAudio() {
        // Increment the current index to play the next audio file
        currentAudioIndex = (currentAudioIndex + 1) % audioFilePaths.size
        val nextAudioFilePath = audioFilePaths[currentAudioIndex]
        selectedAudioFilePath = nextAudioFilePath // Update selectedAudioFilePath
        playAudio(nextAudioFilePath)

        // Notify the activity of the new audio file
        onNewAudioStarted(nextAudioFilePath)
    }

    fun playFirstAudio() {
        // Start playing the first audio file in the list
        currentAudioIndex = 0
        val firstAudioFilePath = audioFilePaths[currentAudioIndex]
        playAudio(firstAudioFilePath)
    }

    fun playAudio(audioFilePath: String) {
        // Release the current MediaPlayer instance if it's already playing
        mediaPlayer?.release()

        // Find the index of the provided audioFilePath in the list
        val index = audioFilePaths.indexOf(audioFilePath)
        if (index != -1) {
            currentAudioIndex = index
            playCurrentAudio()
        } else {
            // Audio file path not found in the list
            Log.e("AudioPlayer", "Audio file not found: $audioFilePath")
        }
    }

    fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener) {
        mediaPlayer?.setOnCompletionListener(listener)
    }

    fun playPreviousAudio() {
        // Decrement the current index to play the previous audio file
        currentAudioIndex = if (currentAudioIndex == 0) {
            audioFilePaths.size - 1 // If at the first index, move to the last index
        } else {
            currentAudioIndex - 1
        }
        val previousAudioFilePath = audioFilePaths[currentAudioIndex]
        selectedAudioFilePath = previousAudioFilePath // Update selectedAudioFilePath
        playAudio(previousAudioFilePath)
    }

    // Function to set the flag indicating seek bar movement during pause
    fun setSeekBarMovedDuringPause() {
        wasSeekBarMovedDuringPause = true
    }

    fun setPlayMode(playmode: PlayMode){
        playMode = playmode
    }

    private fun shuffleAudioFilePaths() {
        // Create a list of indexes representing the order of audio files
        shuffledIndexes = (0 until audioFilePaths.size).shuffled().toMutableList()
    }

}

