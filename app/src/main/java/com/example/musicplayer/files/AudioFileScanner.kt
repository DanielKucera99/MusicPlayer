package com.example.musicplayer.files

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

class AudioFileScanner(private val context: Context) {

    fun getAudioFiles(): ArrayList<String> {
        val audioFilesList = ArrayList<String>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            var i = 0
            if (it.moveToFirst()) {
                do {
                    val data = it.getString(dataColumn)
                    audioFilesList.add(data)
                    i++
                } while (it.moveToNext())
            }
        }
        return audioFilesList
    }
}