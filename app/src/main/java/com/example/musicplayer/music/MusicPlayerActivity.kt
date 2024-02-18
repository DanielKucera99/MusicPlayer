package com.example.musicplayer.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musicplayer.R
import com.example.musicplayer.ui.theme.SeekBarUpdater

enum class PlayMode {
    FORWARD, SHUFFLE, LOOP
}

class MusicPlayerActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    private val READ_MEDIA_AUDIO_REQUEST = 123 // You can use any value here
    private val seekBarUpdater = SeekBarUpdater(updateCallback = this::updateSeekBar)
    private lateinit var seekBar: SeekBar
    private lateinit var stopPlayButton: ImageButton
    private lateinit var currentSongButton: Button
    private lateinit var audioPlayer: AudioPlayer
    private var isPlaying: Boolean = false
    private var currentAudioFilePath: String? = null
    private var selectedAudioFilePath: String? = null
    private lateinit var backwardButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var modeButton: ImageButton
    private var playMode: PlayMode = PlayMode.FORWARD
    private lateinit var audioFilePaths: MutableList<String>
    private lateinit var sidebarLayout: LinearLayout



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        sidebarLayout = findViewById(R.id.sidebar)
        seekBar = findViewById(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(this)
        stopPlayButton = findViewById(R.id.stopPlayButton)
        audioPlayer = AudioPlayer(
            mutableListOf(),
            selectedAudioFilePath,
            { newAudioFilePath ->
                updateCurrentSong(newAudioFilePath)
            },
            { progress ->
                // Update seek bar's progress
                seekBar.progress = progress
            },playMode)
        backwardButton = findViewById(R.id.backwardButton)
        forwardButton = findViewById(R.id.forwardButton)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPosition = audioPlayer.getDuration() * progress / 100
                    audioPlayer.seekTo(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed for this implementation
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed for this implementation
            }
        })
        modeButton = findViewById(R.id.modeButton)
        modeButton.setImageResource(getPlayModeIcon())

        // Set click listener to toggle play mode and update icon
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
                    currentAudioFilePath = selectedAudioFilePath
                    updateCurrentSong(selectedAudioFilePath ?: "")
                    seekBarUpdater.startUpdatingSeekBar()
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

        // Initialize current song button
        currentSongButton = findViewById(R.id.currentSong)
        currentSongButton.setOnClickListener {
            if(currentAudioFilePath != null) {
                val intent = Intent(this, AudioDetailsActivity::class.java)
                // Pass any necessary data to the AudioDetailsActivity using intent extras
                intent.putExtra("AUDIO_FILE_PATH", currentAudioFilePath)
                // Similarly, pass other details like artist, album, etc., to the intent extras
                startActivity(intent)
            }
        }
        // Assuming you have a Button with id "button" in your layout
        val songsLayout = findViewById<LinearLayout>(R.id.songs)

        val alphabets = ('A'..'Z').toList()


        alphabets.forEach { alphabet ->
            val button = Button(this)
            button.text = alphabet.toString()
            button.setOnClickListener {
                // Scroll to the position where the first song starting with this alphabet is located
                scrollToAlphabet(alphabet)
            }
            // Add button to the sidebar LinearLayout
            sidebarLayout.addView(button)
        }
        // Check and request permissions
        if (checkPermission()) {
            // Permissions already granted, proceed with retrieving audio files
            Log.d("test", "Permission checked")
            retrieveAudioFiles(songsLayout)
        } else {
            // Permissions not granted, request the permission
            requestPermission()
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission(): Boolean {
        // Check if the permission is granted
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission() {
        // Request the permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
            READ_MEDIA_AUDIO_REQUEST
        )
    }

    private fun retrieveAudioFiles(songs: LinearLayout) {
        val audioFileScanner = AudioFileScanner(this)
        val audioFilesList = audioFileScanner.getAudioFiles()
        audioFilePaths = audioFilesList.toMutableList() // Convert to list of paths
        audioPlayer = AudioPlayer(
            audioFilePaths,
            selectedAudioFilePath,
            ::onNewAudioStarted,
            ::onProgressUpdate,
            playMode
        )


        if (audioFilesList.isNotEmpty()) {
            for (audioFilePath in audioFilesList) {
                val audioTitle = extractAudioTitle(audioFilePath)
                val button = Button(this)
                button.text = audioTitle
                songs.addView(button)

                button.setOnClickListener {
                    // Check if a different audio file is selected
                    if (selectedAudioFilePath != audioFilePath) {
                        // Reset play state and start playing the new audio file
                        audioPlayer.playAudio(audioFilePath)
                        selectedAudioFilePath = audioFilePath
                        updateCurrentSong(audioFilePath)
                        seekBarUpdater.startUpdatingSeekBar()
                        isPlaying = true
                        stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
                    } else {
                        // If the same audio file is selected, toggle playback state
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
                }
            }
        } else {
            // Handle case when no audio files are found
            val button = Button(this)
            button.text = "No Audio Files Found"
            songs.addView(button)
        }
    }
    private fun extractAudioTitle(audioFilePath: String): String {
        // You can implement this function to extract the title from the audio file
        // For simplicity, let's return the file name without extension
        return audioFilePath.substringAfterLast("/")
            .substringBeforeLast(".") // Remove file extension
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_MEDIA_AUDIO_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("test", "Permission granted!")
                // Permission granted, retrieve audio files
                retrieveAudioFiles(findViewById(R.id.songs))
            } else {
                // Permission denied, handle accordingly
                // For example, show a message indicating the permission is necessary to proceed
            }
        }
    }

    private fun updateSeekBar() {
        val duration =
            audioPlayer.getDuration() // Implement getDuration() in your AudioPlayer class
        val currentPosition =
            audioPlayer.getCurrentPosition() // Implement getCurrentPosition() in your AudioPlayer class

        val progress = (currentPosition.toFloat() / duration.toFloat() * 100).toInt()

        seekBar.progress = progress
    }

    private fun updateCurrentSong(audioFilePath: String) {
        currentSongButton.text = extractAudioTitle(audioFilePath)
        currentAudioFilePath = audioFilePath
    }

    private fun onNewAudioStarted(newAudioFilePath: String) {
        updateCurrentSong(newAudioFilePath)
        selectedAudioFilePath =
            newAudioFilePath // Update selectedAudioFilePath when a new audio is started
    }

    private fun onProgressUpdate(progress: Int) {
        // Update seek bar's progress
        seekBar.progress = progress
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        // Check if the progress change is initiated by the user
        if (fromUser) {
            // Check if the audio is currently paused
            if (!isPlaying) {
                // If the audio is paused, inform the AudioPlayer that the seek bar was moved during pause
                audioPlayer.setSeekBarMovedDuringPause()
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

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
    fun changePlayMode(newMode: PlayMode) {
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
    fun getPlayModeIcon(): Int {
        return when (playMode) {
            PlayMode.FORWARD -> R.drawable.outline_arrow_forward_black_24dp
            PlayMode.SHUFFLE -> R.drawable.baseline_shuffle_black_24dp
            PlayMode.LOOP -> R.drawable.baseline_repeat_black_24dp
        }
    }
    fun scrollToAlphabet(alphabet: Char) {
        val songsLayout: LinearLayout = findViewById(R.id.songs)
        val scrollView: ScrollView = findViewById(R.id.scrollView)
        // Iterate through each child view of the songs layout
        for (i in 0 until songsLayout.childCount) {
            val songButton = songsLayout.getChildAt(i) as Button
            val songTitle = songButton.text.toString()

            // Check if the song title starts with the specified alphabet
            if (songTitle.isNotEmpty() && songTitle[0].toUpperCase() == alphabet) {
                // Calculate the Y position of the song button relative to the ScrollView
                val scrollY = songButton.top - scrollView.top

                // Scroll the ScrollView to the position of the song button
                scrollView.scrollTo(0, scrollY)

                // Exit the loop since we found the first song starting with the alphabet
                break
            }
        }
    }

}
