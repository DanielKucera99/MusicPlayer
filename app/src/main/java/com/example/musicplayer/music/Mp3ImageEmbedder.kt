package com.example.musicplayer.music

import java.io.File
import java.io.RandomAccessFile

class Mp3ImageEmbedder {
    fun embedImage(mp3File: File, imageFile: File): Boolean {
        // Read the image file bytes
        val imageBytes = imageFile.readBytes()

        // Open the MP3 file in read-write mode
        val mp3RandomAccessFile = RandomAccessFile(mp3File, "rw")

        // Find the location to embed the image
        val location = findImageLocation(mp3RandomAccessFile)

        if (location != -1L) {
            // Write image bytes at the found location
            mp3RandomAccessFile.seek(location)
            mp3RandomAccessFile.write(imageBytes)

            // Update MP3 file metadata (if required)

            // Close the MP3 file
            mp3RandomAccessFile.close()

            return true
        }

        // Close the MP3 file
        mp3RandomAccessFile.close()

        return false
    }

    private fun findImageLocation(mp3RandomAccessFile: RandomAccessFile): Long {
        // Define the magic bytes of the image data
        val magicBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte()) // Example: JPEG magic bytes

        // Start searching from the beginning of the file
        mp3RandomAccessFile.seek(0)

        var location: Long = -1

        // Read the file byte by byte and search for the image magic bytes
        var byteRead: Int
        while (mp3RandomAccessFile.read().also { byteRead = it } != -1) {
            if (byteRead == magicBytes[0].toInt()) {
                val nextByte = mp3RandomAccessFile.read()
                if (nextByte == magicBytes[1].toInt()) {
                    // Found the magic bytes
                    location = mp3RandomAccessFile.filePointer - 2 // Adjust for the magic bytes length
                    break
                }
            }
        }

        return location
    }
}