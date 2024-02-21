package com.example.musicplayer.music

import android.widget.ImageButton
import com.example.musicplayer.R

class AudioPlayerControls(private val audioPlayer: AudioPlayer){

    private var playMode = audioPlayer.getPlayMode()
    private var audioFilePaths = audioPlayer.getAudioFiles()
    private var isPlaying: Boolean = false
    fun play() {
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
    }
   fun initializeControls( stopPlayButton: ImageButton,
                                backwardButton: ImageButton,
                                forwardButton: ImageButton,
                                modeButton: ImageButton,
                                updateCurrentSong: (String) -> Unit) {

       modeButton.setImageResource(getPlayModeIcon())
      modeButton.setOnClickListener {
          togglePlayMode()
          playMode = when (playMode) {
              PlayMode.FORWARD -> PlayMode.SHUFFLE
              PlayMode.SHUFFLE -> PlayMode.LOOP
              PlayMode.LOOP -> PlayMode.FORWARD
          }
          audioPlayer.setPlayMode(playMode)
          modeButton.setImageResource(getPlayModeIcon())
      }
      backwardButton.setOnClickListener {
          if (isPlaying) {
              audioPlayer.pauseAudio()
              isPlaying = false
              stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
          }
          audioPlayer.playPreviousAudioBasedOnMode()
          audioPlayer.getSelectedAudioFilePath()?.let { it1 -> updateCurrentSong(it1) }
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
          audioPlayer.getSelectedAudioFilePath()?.let { it1 -> updateCurrentSong(it1) }

          isPlaying = true
          stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
      }

      stopPlayButton.setOnClickListener {

          if (isPlaying) {
              audioPlayer.pauseAudio()
              isPlaying = false
              stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
          } else {
                  audioPlayer.resumeAudio()
                  isPlaying = true
                  stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
              }
          }
      audioPlayer.setOnCompletionListener {
          audioPlayer.playNextAudioBasedOnMode()
          audioPlayer.getSelectedAudioFilePath()?.let { it1 -> updateCurrentSong(it1) }
      }
   }
    private fun togglePlayMode() {
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
        when (playMode) {
            PlayMode.FORWARD -> {
                audioFilePaths.sort()
            }

            PlayMode.SHUFFLE -> {
                audioFilePaths.shuffle()
            }

            PlayMode.LOOP -> {
            }
        }
    }

    fun getPlayModeIcon(): Int {
        return when (audioPlayer.getPlayMode()) {
            PlayMode.FORWARD -> R.drawable.outline_arrow_forward_black_24dp
            PlayMode.SHUFFLE -> R.drawable.baseline_shuffle_black_24dp
            PlayMode.LOOP -> R.drawable.baseline_repeat_black_24dp
        }
    }

}