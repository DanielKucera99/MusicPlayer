package com.example.musicplayer.music

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musicplayer.R
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

class AudioDetailsEditActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var artistEditText: EditText
    private lateinit var albumEditText: EditText
    private lateinit var yearEditText: EditText
    private lateinit var genreEditText: EditText
    private lateinit var selectedImageView: ImageView
    private var imageFile: File? = null
    private val WRITE_MEDIA_AUDIO_REQUEST = 101

    companion object {
        private const val PICK_IMAGE_REQUEST = 1

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkPermission()) {
            // Permissions already granted, proceed with retrieving audio files
            Log.d("test", "Permission checked")
            inflateLayoutAndInitializeViews()
        } else {
            // Permissions not granted, request the permission
            requestPermission()
        }

    }


        private fun inflateLayoutAndInitializeViews() {
            setContentView(R.layout.activity_audio_details_edit)
            initializeViews()
        }

        private fun initializeViews() {
            val audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")
            // Initialize EditText fields
            titleEditText = findViewById(R.id.titleEditText)
            artistEditText = findViewById(R.id.artistEditText)
            albumEditText = findViewById(R.id.albumEditText)
            yearEditText = findViewById(R.id.yearEditText)
            genreEditText = findViewById(R.id.genreEditText)

            // Initialize ImageView for displaying selected image
            selectedImageView = findViewById(R.id.selectedImageView)

            // Set OnClickListener for the button to select image
            val selectImageButton: Button = findViewById(R.id.selectImageButton)
            selectImageButton.setOnClickListener {
                selectImage()
            }

            // Set OnClickListener for the button to save metadata
            val saveButton: Button = findViewById(R.id.saveButton)
            saveButton.setOnClickListener {
                saveMetadata(audioFilePath.toString())
            }
        }
        // Initialize EditText fields
        // Request permission for writing media audio

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission(): Boolean {
        // Check if the permission is granted
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission() {
        // Request the permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_MEDIA_AUDIO_REQUEST
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_MEDIA_AUDIO_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("test", "Permission granted!")
                // Permission granted, proceed with editing
                inflateLayoutAndInitializeViews()
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Permission denied. Metadata editing requires storage access.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun saveMetadata(audioFilePath: String) {
        // Get metadata values from EditText fields
        val title = titleEditText.text.toString()
        val artist = artistEditText.text.toString()
        val album = albumEditText.text.toString()
        val year = yearEditText.text.toString()
        val genre = genreEditText.text.toString()

        // Validate that the audio file exists
        if (audioFilePath.isNotEmpty()) {
            val audioFile = File(audioFilePath)

            // Check if the audio file exists
            if (audioFile.exists()) {
                // Set the metadata
                setMetadata(audioFile, title, artist, album, year, genre, imageFile)
            } else {
                // Handle error if audio file doesn't exist
                showToast("Audio file not found")
            }
        }
    }

    private fun setMetadata(
        audioFile: File,
        title: String,
        artist: String,
        album: String,
        year: String,
        genre: String,
        imageFile: File?
    ) {
        try {
            // Read audio file
            val audio: AudioFile = AudioFileIO.read(audioFile)

            // Set metadata
            val tag = audio.tagOrCreateAndSetDefault
            if (title.isNotEmpty()) {
                tag.setField(FieldKey.TITLE, title)
            }
            if (artist.isNotEmpty()) {
                tag.setField(FieldKey.ARTIST, artist)
            }
            if (album.isNotEmpty()) {
                tag.setField(FieldKey.ALBUM, album)
            }
            if (year.isNotEmpty()) {
                tag.setField(FieldKey.YEAR, year)
            }
            if (genre.isNotEmpty()) {
                tag.setField(FieldKey.GENRE, genre)
            }

            imageFile?.let {
                val imageData = it.readBytes()
                tag.setField(FieldKey.COVER_ART, imageData.toString())
            }

            // Save changes
            AudioFileIO.write(audio)

            // Finish activity
            finish()
        } catch (e: Exception) {
            // Handle exception
            e.printStackTrace()
            showToast("Failed to save metadata")
        }
    }

    private fun showToast(message: String) {
        // Show a Toast message
        // You can implement this method based on your application's Toast mechanism
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            selectedImageView.setImageURI(selectedImageUri)
            selectedImageView.tag = selectedImageUri?.toString()
            selectedImageView.visibility = ImageView.VISIBLE
            imageFile = getFileFromUri(selectedImageUri)

        }
    }

    private fun getFileFromUri(uri: Uri?): File? {
        val contentResolver = applicationContext.contentResolver
        val inputStream = uri?.let { contentResolver.openInputStream(it) }
        inputStream?.use { input ->
            val tempFile = createTempFile("temp_image", ".jpg")
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
            return tempFile
        }
        return null
    }
    private fun getAudioFileId(audioFilePath: String): Long {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val selectionArgs = arrayOf(audioFilePath)
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
            }
        }
        throw IllegalStateException("Audio file ID not found")
    }
}
