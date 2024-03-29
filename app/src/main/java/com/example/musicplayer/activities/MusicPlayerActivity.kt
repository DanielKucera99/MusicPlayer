package com.example.musicplayer.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.example.musicplayer.controls.AudioPlayerControls
import com.example.musicplayer.controls.SeekBarManager
import com.example.musicplayer.files.AudioFileScanner
import com.example.musicplayer.files.AudioPlayer
import java.io.File

enum class PlayMode {
    FORWARD, SHUFFLE, LOOP
}

class MusicPlayerActivity : AppCompatActivity(){

    private val READ_MEDIA_AUDIO_REQUEST = 123

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
    private var audioFilePaths: MutableList<String> = mutableListOf()
    private var oldAudioFilePaths: MutableList<String> = mutableListOf()
    private lateinit var sidebarLayout: LinearLayout
    private lateinit var seekBarManager: SeekBarManager
    private lateinit var audioPlayerControls: AudioPlayerControls
    private lateinit var songs: LinearLayout
    private var isOnResume : Boolean = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        songs = findViewById(R.id.songs)
        sidebarLayout = findViewById(R.id.sidebar)
        seekBar = findViewById(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPosition = audioPlayer.getDuration() * progress / 100
                    audioPlayer.seekTo(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        stopPlayButton = findViewById(R.id.stopPlayButton)
        backwardButton = findViewById(R.id.backwardButton)
        forwardButton = findViewById(R.id.forwardButton)
        modeButton = findViewById(R.id.modeButton)
        currentSongButton = findViewById(R.id.currentSong)
        currentSongButton.setOnClickListener {
            if(currentAudioFilePath != null) {
                val intent = Intent(this, AudioDetailsActivity::class.java)
                intent.putExtra("AUDIO_FILE_PATH", currentAudioFilePath)
                startActivity(intent)
            }
        }

        if (checkPermission()) {
            retrieveAudioFiles()
            pausePlay(audioFilePaths)
        } else {
            requestPermission()
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
            READ_MEDIA_AUDIO_REQUEST
        )
    }

    private fun retrieveAudioFiles() {
        val audioFileScanner = AudioFileScanner(this)
        val audioFilesList = audioFileScanner.getAudioFiles()
        oldAudioFilePaths.clear()
            for (audioFilePath in audioFilePaths)
            {
                oldAudioFilePaths.add(audioFilePath)
            }
        audioFilePaths = audioFilesList.toMutableList()
        audioFilePaths.sortBy { filePath ->
            val file = File(filePath)
            file.name
        }

        val startingLetters = audioFilePaths.mapNotNull { filePath ->
            val file = File(filePath).name
            file.firstOrNull()?.uppercase()?.singleOrNull()
        }.distinct().sorted()
        startingLetters.forEach { letter ->
            val alphabetButton = Button(this)
            alphabetButton.text = letter.toString()
            alphabetButton.setOnClickListener {
                scrollToAlphabet(letter)
            }
            sidebarLayout.addView(alphabetButton)
        }
        audioPlayer = AudioPlayer(
            audioFilePaths,
            selectedAudioFilePath,
            ::onNewAudioStarted,
            ::onProgressUpdate,
            playMode
        )
        seekBarManager = SeekBarManager(seekBar,audioPlayer)
        seekBarManager.startUpdatingSeekBar()

        audioPlayerControls = AudioPlayerControls(audioPlayer)

        AudioPlayer.setInstance(audioPlayer)
        audioPlayerControls.initializeControls(
            stopPlayButton,
            backwardButton,
            forwardButton,
            modeButton,
            ::updateCurrentSong
        )

    }
    private fun extractAudioTitle(audioFilePath: String): String {
        return audioFilePath.substringAfterLast("/")
            .substringBeforeLast(".")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_MEDIA_AUDIO_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                retrieveAudioFiles()
            }
        }
    }

    private fun updateCurrentSong(audioFilePath: String) {
        currentSongButton.text = extractAudioTitle(audioFilePath)
        currentAudioFilePath = audioFilePath
    }

    private fun onNewAudioStarted(newAudioFilePath: String) {
        updateCurrentSong(newAudioFilePath)
        selectedAudioFilePath =
            newAudioFilePath
    }

    private fun onProgressUpdate(progress: Int) {
        seekBar.progress = progress
    }

    private fun scrollToAlphabet(alphabet: Char) {
        val songsLayout: LinearLayout = findViewById(R.id.songs)
        val scrollView: ScrollView = findViewById(R.id.scrollView)
        for (i in 0 until songsLayout.childCount) {
            val songButtonToScrollTo = songsLayout.getChildAt(i) as Button
            val songTitle = songButtonToScrollTo.text.toString()

            if (songTitle.isNotEmpty() && songTitle[0].uppercaseChar() == alphabet) {
                val scrollY = songButtonToScrollTo.top - scrollView.top

                scrollView.scrollTo(0, scrollY)

                break
            }
        }
    }

    override fun onPause() {
        super.onPause()
        seekBarManager.stopUpdatingSeekBar()
    }
    override fun onResume() {
        super.onResume()
        seekBarManager.startUpdatingSeekBar()

        audioPlayer.setOnNewAudioStartedCallback(::onNewAudioStarted)
        if(isOnResume) {
            currentSongButton.text = extractAudioTitle(audioPlayer.getCurrentlyPlayingFile())
        } else {
            currentSongButton.text = "No audio file"
        }
        selectedAudioFilePath = audioPlayer.getCurrentlyPlayingFile()
        playMode = audioPlayerControls.getAudioPlayer().getPlayMode()
        audioPlayerControls.setPlayMode(playMode)
        audioPlayer.setPlayMode(playMode)
        modeButton.setImageResource(audioPlayerControls.getPlayModeIcon())
        if(audioPlayer.getAudioCurrentState()) {
            stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
            audioPlayerControls.pause()
        } else {
            stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
            audioPlayerControls.play()
        }
        isOnResume = true
        pausePlay(audioPlayer.getAudioFiles())

    }

    private fun pausePlay(audioFilesList: MutableList<String>){
        if (audioFilesList.isNotEmpty()) {
            for (audioFilePath in audioFilesList) {
                val audioTitle = extractAudioTitle(audioFilePath)
                val button = Button(this)
                button.text = audioTitle
                if(!isOnResume) {
                    songs.addView(button)
                }
                    button.setOnClickListener {
                    if (!audioPlayer.getAudioCurrentState()){
                        if (selectedAudioFilePath != audioFilePath) {
                            audioPlayer.playAudio(audioFilePath)
                            selectedAudioFilePath = audioFilePath
                            updateCurrentSong(audioFilePath)
                            isPlaying = true
                            audioPlayerControls.play()
                            stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
                        } else {
                            if (isPlaying) {
                                audioPlayer.pauseAudio()
                                isPlaying = false
                                audioPlayerControls.pause()
                                stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
                            } else {
                                audioPlayer.resumeAudio()
                                isPlaying = true
                                audioPlayerControls.play()
                                stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
                            }
                        }
                    } else {
                        if (selectedAudioFilePath != audioFilePath) {
                            audioPlayer.playAudio(audioFilePath)
                            selectedAudioFilePath = audioFilePath
                            updateCurrentSong(audioFilePath)
                            isPlaying = true
                            audioPlayerControls.play()

                            stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
                        }  else {
                            audioPlayer.resumeAudio()
                            isPlaying = true
                            audioPlayerControls.play()
                            stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
                        }
                    }
                }
            }
        } else {
            val button = Button(this)
            button.text = "No Audio Files Found"
            songs.addView(button)
        }
    }
}


