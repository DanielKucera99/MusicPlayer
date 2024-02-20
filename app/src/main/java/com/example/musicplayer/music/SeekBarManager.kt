import android.widget.SeekBar
import com.example.musicplayer.music.AudioPlayer
import com.example.musicplayer.ui.theme.SeekBarUpdater

class SeekBarManager(
    private val seekBar: SeekBar,
    private val audioPlayer: AudioPlayer
) : SeekBar.OnSeekBarChangeListener {

    private val seekBarUpdater = SeekBarUpdater(updateIntervalMillis = 1000, updateCallback = this::updateSeekBar)

    init {
        seekBar.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            val newPosition = audioPlayer.getDuration() * progress / 100
            audioPlayer.seekTo(newPosition)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    fun startUpdatingSeekBar() {
        seekBarUpdater.startUpdatingSeekBar()
    }

    fun stopUpdatingSeekBar() {
        seekBarUpdater.stopUpdatingSeekBar()
    }

    private fun updateSeekBar() {
        val duration = audioPlayer.getDuration()
        val currentPosition = audioPlayer.getCurrentPosition()
        val progress = if (duration > 0) currentPosition * 100 / duration else 0
        seekBar.progress = progress
    }
}