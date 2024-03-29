package com.example.musicplayer.controls
import android.os.Handler

class SeekBarUpdater(private val updateIntervalMillis: Long = 1000, private val updateCallback: () -> Unit) {
    private val seekBarUpdateHandler = Handler()
    private lateinit var seekBarUpdateRunnable: Runnable

    fun startUpdatingSeekBar() {
        seekBarUpdateRunnable = object : Runnable {
            override fun run() {
                updateCallback.invoke() // Perform the update action
                seekBarUpdateHandler.postDelayed(this, updateIntervalMillis) // Schedule the next update
            }
        }
        seekBarUpdateHandler.postDelayed(seekBarUpdateRunnable, updateIntervalMillis)
    }

    fun stopUpdatingSeekBar() {
        seekBarUpdateHandler.removeCallbacks(seekBarUpdateRunnable)
    }
}