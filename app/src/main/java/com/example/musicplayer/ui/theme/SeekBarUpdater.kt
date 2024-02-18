package com.example.musicplayer.ui.theme
import android.os.Handler

class SeekBarUpdater(private val updateIntervalMillis: Long = 1000, private val updateCallback: () -> Unit) {
    private val seekBarUpdateHandler = Handler()
    private lateinit var seekBarUpdateRunnable: Runnable

    fun startUpdatingSeekBar() {
        // Initialize the runnable
        seekBarUpdateRunnable = object : Runnable {
            override fun run() {
                updateCallback.invoke() // Perform the update action
                seekBarUpdateHandler.postDelayed(this, updateIntervalMillis) // Schedule the next update
            }
        }
        // Start updating the seek bar
        seekBarUpdateHandler.postDelayed(seekBarUpdateRunnable, updateIntervalMillis)
    }

    fun stopUpdatingSeekBar() {
        seekBarUpdateHandler.removeCallbacks(seekBarUpdateRunnable)
    }
}