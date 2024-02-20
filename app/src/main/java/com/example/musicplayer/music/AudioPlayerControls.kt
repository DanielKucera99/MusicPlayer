package com.example.musicplayer.music

import android.media.MediaPlayer
import android.widget.ImageButton
import com.example.musicplayer.R

class AudioPlayerControls(private val audioPlayer: AudioPlayer,
                          private var isPlaying: Boolean){

    private var playMode = audioPlayer.getPlayMode()
    private var audioFilePaths = audioPlayer.getAudioFiles()
    private var currentAudioFilePath = audioPlayer.getCurrentlyPlayingFile()
    private var selectedAudioFilePath: String? = audioFilePaths.firstOrNull()
   fun initializeControls( stopPlayButton: ImageButton,
                                backwardButton: ImageButton,
                                forwardButton: ImageButton,
                                modeButton: ImageButton,
                                updateCurrentSong: (String) -> Unit) {
      modeButton.setOnClickListener {
          togglePlayMode()
          when (playMode) {
              PlayMode.FORWARD -> playMode = PlayMode.SHUFFLE
              PlayMode.SHUFFLE -> playMode = PlayMode.LOOP
              PlayMode.LOOP -> playMode = PlayMode.FORWARD
          }
          audioPlayer.setPlayMode(playMode)
          // Set the new icon based on the updated play mode
          modeButton.setImageResource(getPlayModeIcon())
      }
      backwardButton.setOnClickListener {
          if (isPlaying) {
              audioPlayer.pauseAudio()
              isPlaying = false
              stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
          }
          audioPlayer.playPreviousAudioBasedOnMode()
          updateCurrentSong(selectedAudioFilePath ?: "")
          isPlaying = true
          stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
      }

      forwardButton.setOnClickListener {
          if (isPlaying) {
              audioPlayer.pauseAudio()
              isPlaying = false
              stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
          }
          audioPlayer.playNextAudioBasedOnMode()
          updateCurrentSong(selectedAudioFilePath ?: "")
          isPlaying = true
          stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
      }

      stopPlayButton.setOnClickListener {

          if (isPlaying) {
              audioPlayer.pauseAudio()
              isPlaying = false
              stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
          } else {
              // Check if a different audio file is being played
              if (currentAudioFilePath != null && currentAudioFilePath != selectedAudioFilePath) {
                  // Reset play state and start playing the new audio file
                  audioPlayer.playAudio(selectedAudioFilePath ?: "")
                  currentAudioFilePath = selectedAudioFilePath.toString()

                  updateCurrentSong(selectedAudioFilePath ?: "")
              } else {
                  // Resume playback
                  audioPlayer.resumeAudio()
                  isPlaying = true
                  stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
              }
          }
      }
      audioPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener {
          // Play the next audio file when the current one finishes
          audioPlayer.playNextAudioBasedOnMode()
          // Update UI or perform any other necessary actions
          updateCurrentSong(selectedAudioFilePath ?: "")
      })
  }
    private fun togglePlayMode() {
        // Toggle between play modes: FORWARD -> SHUFFLE -> LOOP -> FORWARD
        changePlayMode(
            when (playMode) {
                PlayMode.FORWARD -> PlayMode.SHUFFLE
                PlayMode.SHUFFLE -> PlayMode.LOOP
                PlayMode.LOOP -> PlayMode.FORWARD
            }
        )
    }
    private fun changePlayMode(newMode: PlayMode) {
        playMode = newMode
        // Depending on the mode, you can shuffle the audio files or reset the index, etc.
        // Implement this logic based on your requirements
        when (playMode) {
            PlayMode.FORWARD -> {
                // Sort audio files by name
                audioFilePaths.sort()
            }

            PlayMode.SHUFFLE -> {
                // Shuffle audio files
                audioFilePaths.shuffle()
            }

            PlayMode.LOOP -> {
                // No need to modify audio files order for loop mode
            }
        }
    }
    private fun updatePlayModeButton(modeButton: ImageButton) {
        modeButton.setImageResource(getPlayModeIcon())
    }

    private fun handlePreviousButton(
        stopPlayButton: ImageButton,
        selectedAudioFilePath: String?,
        updateCurrentSong: (String) -> Unit,
        isPlaying: Boolean
    ) {
        if (isPlaying) {
            audioPlayer.pauseAudio()
            stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
        }
        audioPlayer.playPreviousAudioBasedOnMode()
        updateCurrentSong(selectedAudioFilePath ?: "")
    }

    private fun handleNextButton(
        stopPlayButton: ImageButton,
        selectedAudioFilePath: String?,
        updateCurrentSong: (String) -> Unit,
        isPlaying: Boolean
    ) {
        if (isPlaying) {
            audioPlayer.pauseAudio()
            stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
        }
        audioPlayer.playNextAudioBasedOnMode()
        updateCurrentSong(selectedAudioFilePath ?: "")
    }

    private fun handlePlayStopButton(
        stopPlayButton: ImageButton,
        currentAudioFilePath: String?,
        selectedAudioFilePath: String?,
        updateCurrentSong: (String) -> Unit,
        isPlaying: Boolean
    ) {
        if (isPlaying) {
            audioPlayer.pauseAudio()
            stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
        } else {
            if (currentAudioFilePath != null && currentAudioFilePath != selectedAudioFilePath) {
                // Reset play state and start playing the new audio file
                audioPlayer.playAudio(selectedAudioFilePath ?: "")
                updateCurrentSong(selectedAudioFilePath ?: "")
            } else {
                audioPlayer.resumeAudio()
                stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
            }
        }
    }

    private fun getPlayButtonIcon(isPlaying: Boolean): Int {
        return if (isPlaying) {
            R.drawable.baseline_pause_circle_black_24dp
        } else {
            R.drawable.baseline_play_circle_filled_black_24dp
        }
    }

    private fun getPlayModeIcon(): Int {
        return when (audioPlayer.getPlayMode()) {
            PlayMode.FORWARD -> R.drawable.outline_arrow_forward_black_24dp
            PlayMode.SHUFFLE -> R.drawable.baseline_shuffle_black_24dp
            PlayMode.LOOP -> R.drawable.baseline_repeat_black_24dp
        }
    }
    private fun extractAudioTitle(audioFilePath: String): String {
        // You can implement this function to extract the title from the audio file
        // For simplicity, let's return the file name without extension
        return audioFilePath.substringAfterLast("/")
            .substringBeforeLast(".") // Remove file extension
    }
}