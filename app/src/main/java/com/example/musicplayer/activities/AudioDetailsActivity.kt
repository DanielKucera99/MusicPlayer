package com.example.musicplayer.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.R
import com.example.musicplayer.controls.AudioPlayerControls
import com.example.musicplayer.controls.SeekBarManager
import com.example.musicplayer.files.AudioPlayer
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.Locale

class AudioDetailsActivity : AppCompatActivity() {

    private lateinit var editButton: ImageButton
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarManager: SeekBarManager
    private var audioFilePath: String? = null
    private lateinit var backwardButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var modeButton: ImageButton
    private lateinit var stopPlayButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var audioPlayerControls: AudioPlayerControls
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_details)
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
        audioPlayer = AudioPlayer.getInstance()
        seekBar = findViewById(R.id.seekBar)
        seekBarManager = SeekBarManager(seekBar,audioPlayer)
        audioPlayer.setProgressUpdateCallback(::onProgressUpdate)
        audioPlayer.setOnNewAudioStartedCallback(::onNewAudioStarted)
        audioPlayerControls = AudioPlayerControls(audioPlayer)
        val audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")
        if(!audioPlayer.getAudioCurrentState()){

            audioPlayerControls.play()
        }
        else {

            audioPlayerControls.pause()
        }

        editButton = findViewById(R.id.editDetails)
        editButton.setOnClickListener{

            if(audioFilePath != null) {
                val intent = Intent(this, AudioDetailsEditActivity::class.java)
                intent.putExtra("AUDIO_FILE_PATH", audioFilePath)
                startActivity(intent)
                audioPlayer.stopAudio()
            }
        }
    }

    private fun formatDuration(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }
    private fun extractAudioMetadata(audioFilePath: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            val audioFile = File(audioFilePath)
            val audioFileTag = AudioFileIO.read(audioFile).tag
            if (audioFileTag != null) {
                metadata["title"] = audioFileTag.getFirst(FieldKey.TITLE)
                metadata["artist"] = audioFileTag.getFirst(FieldKey.ARTIST)
                metadata["album"] = audioFileTag.getFirst(FieldKey.ALBUM)
                metadata["year"] = audioFileTag.getFirst(FieldKey.YEAR)
                metadata["genre"] = audioFileTag.getFirst(FieldKey.GENRE)

                val artwork = audioFileTag.firstArtwork
                if (artwork != null) {
                    val imageData = artwork.binaryData
                    metadata["artwork"] = imageData.joinToString(",")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return metadata
    }

    private fun getDuration(audioFilePath: String): Long? {
        return try {
            val audioFile = AudioFileIO.read(File(audioFilePath))
            val audioHeader = audioFile.audioHeader
            audioHeader.trackLength * 1000L
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        seekBar = findViewById(R.id.seekBar)

        audioPlayer = AudioPlayer.getInstance()
        audioPlayer.setProgressUpdateCallback(::onProgressUpdate)
        audioPlayer.setOnNewAudioStartedCallback(::onNewAudioStarted)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newPosition = audioPlayer.getDuration() * progress / 100
                    audioPlayer.seekTo(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        stopPlayButton = findViewById(R.id.stopPlayButton)
        backwardButton = findViewById(R.id.backwardButton)
        forwardButton = findViewById(R.id.forwardButton)
        modeButton = findViewById(R.id.modeButton)
        val audioPlayerControls = AudioPlayerControls(audioPlayer)
        if(!audioPlayer.getAudioCurrentState()) {
            stopPlayButton.setImageResource(R.drawable.baseline_pause_circle_black_24dp)
            audioPlayerControls.play()
        }
        else {
            stopPlayButton.setImageResource(R.drawable.baseline_play_circle_filled_black_24dp)
            audioPlayerControls.pause()
        }
        audioPlayerControls.initializeControls(stopPlayButton,
            backwardButton,
            forwardButton,
            modeButton,
            ::onNewAudioStarted)
        seekBarManager = SeekBarManager(seekBar,audioPlayer)
        seekBarManager.startUpdatingSeekBar()
        audioFilePath = audioPlayer.getCurrentlyPlayingFile()
        getMetadata(audioFilePath!!)


    }
    override fun onPause() {
        super.onPause()
        seekBarManager.stopUpdatingSeekBar()
    }

    private fun onProgressUpdate(progress: Int) {
        seekBar.progress = progress
    }

    private fun onNewAudioStarted(newSong: String) {
        getMetadata(newSong)
    }

    private fun getMetadata(audioFilePath: String){
        var audioTitle: String? = null
        var audioArtist: String? = null
        var audioAlbum: String? = null
        val audioArtworkStr: String?
        val audioDuration = getDuration(audioFilePath)
        val metadata = extractAudioMetadata(audioFilePath)

        metadata["title"]?.let { audioTitle = it }
        metadata["artist"]?.let { audioArtist = it }
        metadata["album"]?.let { audioAlbum = it }
        audioArtworkStr = metadata["artwork"]


        findViewById<TextView>(R.id.audio_title).text = audioTitle ?: extractAudioTitle(audioFilePath)
        findViewById<TextView>(R.id.audio_artist).text = audioArtist ?: "Unknown Artist"
        findViewById<TextView>(R.id.audio_album).text = audioAlbum ?: "Unknown Album"
        if (audioDuration != null) {
            findViewById<TextView>(R.id.totalTime).text = formatDuration(audioDuration.toInt())
        }
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                findViewById<TextView>(R.id.currentTime).text = formatDuration(audioPlayer.getCurrentPosition())
                Handler(Looper.getMainLooper()).postDelayed(this, 1000)
            }
        }, 1000)
        if (audioArtworkStr != null) {
            val audioArtworkBytes = audioArtworkStr.split(",").map { byteStr -> byteStr.toByte() }.toByteArray()
            val imageView = findViewById<ImageView>(R.id.audio_image)
            val bitmap = BitmapFactory.decodeByteArray(audioArtworkBytes, 0, audioArtworkBytes.size)
            imageView.setImageBitmap(bitmap)
        } else {
            findViewById<ImageView>(R.id.audio_image).setImageResource(R.drawable.outline_image_black_24dp)
        }
    }
    private fun extractAudioTitle(audioFilePath: String): String {
        return audioFilePath.substringAfterLast("/")
            .substringBeforeLast(".")
    }
}
