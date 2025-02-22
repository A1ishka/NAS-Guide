package by.ssrlab.common_ui.common.ui.exhibit.fragments.utils.player

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.net.toUri
import by.ssrlab.common_ui.R
import by.ssrlab.common_ui.common.ui.base.BaseActivity
import by.ssrlab.common_ui.common.ui.exhibit.ExhibitActivity
import by.ssrlab.common_ui.databinding.FragmentExhibitBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object MediaPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val seekBarFuns = SeekBarFunctions()

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val scopeUI = CoroutineScope(Dispatchers.Main) //same scope for UI
    private var playerScope: CoroutineScope? = null

    fun initializeMediaPlayerWithString(
        activity: BaseActivity,
        binding: FragmentExhibitBinding,
        url: String,
        onSuccess: () -> Unit
    ) {
        mediaPlayer = MediaPlayer()

        try {
            setDataSource(activity, url.toUri())
            mediaPlayer?.setOnPreparedListener {
                binding.apply {
                    exhibitEndTime.text = seekBarFuns.convertToTimerMode(mediaPlayer!!.duration)
                    exhibitCurrentTime.text =
                        seekBarFuns.convertToTimerMode(mediaPlayer!!.currentPosition)
                }
                listenProgress(binding = binding, activity = activity as ExhibitActivity)
                onSuccess()
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun handlePlayerState(
        state: PlayerStatus,
        activity: ExhibitActivity,
        binding: FragmentExhibitBinding
    ) {
        val fragmentSettingsManager = FragmentSettingsManager(binding, activity)

        when (state) {
            is PlayerStatus.Playing -> {
                fragmentSettingsManager.makeProgressVisible()

                try {
                    mediaPlayer?.start()
                    startProgressTracking(binding = binding, activity = activity)
                } catch (e: Exception) {
                    Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

            is PlayerStatus.Paused -> {
                mediaPlayer?.pause()

                scope.launch {
                    activity.runOnUiThread {
                        binding.apply {
                            if (mediaPlayer?.isPlaying == true) {
                                exhibitCurrentTime.text =
                                    seekBarFuns.convertToTimerMode(mediaPlayer!!.currentPosition)
                                exhibitProgress.progress = mediaPlayer!!.currentPosition
                            }
                        }
                    }
                }
            }

            is PlayerStatus.Seeking -> {
                updateProgress(binding = binding, activity = activity)
            }

            is PlayerStatus.Ended -> {
                mediaPlayer?.seekTo(0)
                mediaPlayer?.pause()

                stopProgressTracking()

                fragmentSettingsManager.makeProgressInvisible()
                PlayerStatus.Paused
            }
        }
    }

    fun isPlaying() = mediaPlayer?.isPlaying ?: false

    fun playAudio(
        activity: ExhibitActivity,
        binding: FragmentExhibitBinding,
        playerStatus: PlayerStatus
    ) {
        scope.launch {
            when (playerStatus) {
                PlayerStatus.Paused -> {
                    mediaPlayer?.pause()
                    handlePlayerState(PlayerStatus.Paused, activity, binding)
                }

                PlayerStatus.Playing -> {
                    try {
                        mediaPlayer?.start()
                        handlePlayerState(PlayerStatus.Playing, activity, binding)
                        mediaPlayer?.setOnSeekCompleteListener {
                            updateProgress(binding = binding, activity = activity)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                    }
                }

                PlayerStatus.Seeking -> {
                    handlePlayerState(PlayerStatus.Seeking, activity, binding)
                }

                PlayerStatus.Ended -> {
                    handlePlayerState(PlayerStatus.Ended, activity, binding)
                }
            }
        }
    }

    fun pauseAudio(imageButton: ImageButton) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
            it.stop()
            it.reset()
        }
        imageButton.setImageResource(R.drawable.ic_play)
    }

    private fun setDataSource(
        activity: BaseActivity, uri: Uri
    ) {
        try {
            mediaPlayer?.setDataSource(activity, uri)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenProgress(
        binding: FragmentExhibitBinding,
        activity: ExhibitActivity
    ) {
        binding.exhibitProgress.max = mediaPlayer?.duration ?: 0

        scopeUI.launch {
            binding.exhibitProgress.setOnSeekBarChangeListener(
                seekBarFuns.createSeekBarProgressListener { progress ->
                    mediaPlayer?.seekTo(progress)
                    updateProgress(binding = binding, activity = activity)
                }
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(
        binding: FragmentExhibitBinding,
        activity: BaseActivity
    ) {
        scopeUI.launch {
            val currentPosition = mediaPlayer?.currentPosition ?: 0
            val duration = mediaPlayer?.duration ?: 0

            if (currentPosition < duration - 500) {
                binding.exhibitCurrentTime.text = seekBarFuns.convertToTimerMode(currentPosition)
            } else {
                binding.exhibitProgress.progress = mediaPlayer!!.duration
                binding.exhibitCurrentTime.text = seekBarFuns.convertToTimerMode(duration)

                handlePlayerState(PlayerStatus.Ended, activity as ExhibitActivity, binding)
            }
            binding.exhibitProgress.progress = currentPosition
        }
    }

    private fun startProgressTracking(binding: FragmentExhibitBinding, activity: ExhibitActivity) {
        createNewPlayerScope()

        playerScope?.launch {
            while (isActive) {
                if (mediaPlayer?.isPlaying == true) {
                    updateProgress(binding = binding, activity = activity)
                }
                delay(100)
            }
        }
    }

    private fun createNewPlayerScope() {
        playerScope?.cancel()
        playerScope = CoroutineScope(Dispatchers.Main) //everytime it's new
    }

    private fun stopProgressTracking() {
        playerScope?.cancel()
        playerScope = null
    }

    fun destroyPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer?.setVolume(leftVolume, rightVolume)
    }
}