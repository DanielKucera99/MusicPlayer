package com.example.musicplayer.music

import SeekBarManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.musicplayer.R
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.Locale

class AudioDetailsActivity : AppCompatActivity() {

    private lateinit var editButton: ImageButton
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var seekBarManager: SeekBarManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_details)
        audioPlayer = AudioPlayer.getInstance()
        seekBar = findViewById(R.id.seekBar)
        seekBarManager = SeekBarManager(seekBar,audioPlayer)
        audioPlayer.setProgressUpdateCallback(::onProgressUpdate)
        // Retrieve audio file details from intent extras (if passed from previous activity)
        val audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")
        editButton = findViewById(R.id.editDetails)
        editButton.setOnClickListener{

            if(audioFilePath != null) {
                Log.d("AudioDetailsActivity","file path is not null")
                val intent = Intent(this, AudioDetailsEditActivity::class.java)
                // Pass any necessary data to the AudioDetailsActivity using intent extras
                intent.putExtra("AUDIO_FILE_PATH", audioFilePath)
                // Similarly, pass other details like artist, album, etc., to the intent extras
                startActivity(intent)
                audioPlayer.stopAudio()
            } else {
                Log.d("AudioDetailsActivity","is file path null")
            }
        }
    }

    private fun formatDuration(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun getEmbeddedImage(audioFilePath: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(audioFilePath)
        val rawImage = retriever.embeddedPicture
        retriever.release()
        return rawImage
    }
    fun setEmbeddedImageToImageView(audioFilePath: String, imageView: ImageView) {
        val rawImage = getEmbeddedImage(audioFilePath)
        if (rawImage != null) {
            val bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.size)
            imageView.setImageBitmap(bitmap)
        } else {
            // Handle case when no embedded image is found
            imageView.setImageResource(R.drawable.outline_image_black_24dp)
        }
    }
    private fun extractAudioMetadata(audioFilePath: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            val audioFile = File(audioFilePath)
            val audioFileTag = AudioFileIO.read(audioFile).tag
            if (audioFileTag != null) {
                // Extract metadata fields
                metadata["title"] = audioFileTag.getFirst(FieldKey.TITLE)
                metadata["artist"] = audioFileTag.getFirst(FieldKey.ARTIST)
                metadata["album"] = audioFileTag.getFirst(FieldKey.ALBUM)
                metadata["year"] = audioFileTag.getFirst(FieldKey.YEAR)
                metadata["genre"] = audioFileTag.getFirst(FieldKey.GENRE)

                // Optionally, you can extract album artwork
                val artwork = audioFileTag.firstArtwork
                if (artwork != null) {
                    val imageData = artwork.binaryData
                    // Convert the byte array to a base64 string or handle it as needed
                    // For simplicity, let's just store the byte array directly
                    metadata["artwork"] = imageData.joinToString(",")
                }
            }
        } catch (e: Exception) {
            // Handle exceptions, such as file not found or invalid audio file format
            e.printStackTrace()
        }

        return metadata
    }

    private fun getDuration(audioFilePath: String): Long? {
        return try {
            val audioFile = AudioFileIO.read(File(audioFilePath))
            val audioHeader = audioFile.audioHeader
            audioHeader.trackLength * 1000L // Convert to milliseconds
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
        seekBarManager = SeekBarManager(seekBar,audioPlayer)
        seekBarManager.startUpdatingSeekBar()
        // Retrieve audio file details again
        val audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")
        var audioTitle: String? = null
        var audioArtist: String? = null
        var audioAlbum: String? = null
        var audioArtworkStr: String? = null
        var audioDuration = audioFilePath?.let { getDuration(it) }
        var metadata = audioFilePath?.let { extractAudioMetadata(it) }

        metadata?.get("title")?.let { audioTitle = it }
        metadata?.get("artist")?.let { audioArtist = it }
        metadata?.get("album")?.let { audioAlbum = it }
        audioArtworkStr = metadata?.get("artwork")


        findViewById<TextView>(R.id.audio_title).text = audioTitle ?: "Unknown Title"
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
        }

    }
    override fun onPause() {
        super.onPause()
        seekBarManager.stopUpdatingSeekBar()
    }

    private fun onProgressUpdate(progress: Int) {
        // Update seek bar's progress
        seekBar.progress = progress
    }

}