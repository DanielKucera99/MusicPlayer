package com.example.musicplayer.music

import android.Manifest
import android.app.Activity
import android.content.ContentUris
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val WRITE_MEDIA_AUDIO_REQUEST = 101
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_details_edit)
        val audioFilePath = intent.getStringExtra("AUDIO_FILE_PATH")

        Log.d("ADEA","audioFilePath: $audioFilePath")
        requestPermissions(audioFilePath ?: "")


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
            saveMetadata(audioFilePath ?: "")
        }

        // Request permission for writing media audio

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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestPermissions(audioFilePath: String) {
        if (!checkPermissions()) {
            // Specify the ID of the item you want to modify
            val itemId = getAudioFileId(audioFilePath)
            // Create a URI with the specified item ID
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, itemId)
            // Create a write request for the specified URI
           /* val intentSender = MediaStore.createWriteRequest(contentResolver, listOf(uri))
            // Start the intent sender to request permission
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            requestPermissionLauncher.launch(intentSenderRequest)*/
            requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted, handle the action
                    Log.d("ADEA","granted")
                } else {
                    // Permission is denied, handle the action accordingly
                }
            }
        } else {
            // Permissions are already granted, proceed with accessing the MediaStore
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
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
