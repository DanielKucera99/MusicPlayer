package com.example.musicplayer.music

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log

class AudioFileScanner(private val context: Context) {

    fun getAudioFiles(): ArrayList<String> {
        val audioFilesList = ArrayList<String>()
        // Querying MediaStore for audio files
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            var i = 0
            if (it.moveToFirst()) {
                do {
                    Log.d("AudioFileScanner", "iterations: $i")
                    val id = it.getLong(idColumn)
                    val data = it.getString(dataColumn)
                    val displayName = it.getString(displayNameColumn)

                    // You can add more information retrieval here as needed
                    // For example, you can retrieve artist, album, duration, etc.
                    // For now, let's just add the file path to the list
                    audioFilesList.add(data)
                    i++
                } while (it.moveToNext())
            } else {
                Log.d("AudioFileScanner", "Cursor is empty")
            }
        }
        return audioFilesList
    }
}