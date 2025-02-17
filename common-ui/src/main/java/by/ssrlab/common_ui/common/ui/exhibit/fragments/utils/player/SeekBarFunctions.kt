package by.ssrlab.common_ui.common.ui.exhibit.fragments.utils.player

import android.widget.SeekBar


class SeekBarFunctions {

    fun convertToTimerMode(duration: Int): String {
        val minute = duration % (1000 * 60 * 60) / (1000 * 60)
        val seconds = duration % (1000 * 60 * 60) % (1000 * 60) / 1000

        var finalString = ""
        finalString += "0$minute:"
        if (seconds < 10) finalString += "0"
        finalString += "$seconds"

        return finalString
    }

    fun createSeekBarProgressListener(onProgressChangeFun: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onProgressChangeFun(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }
}