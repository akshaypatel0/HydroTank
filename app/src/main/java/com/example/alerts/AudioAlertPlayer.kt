package com.example.alerts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.R
import java.util.Locale

class AudioAlertPlayer(private val context: Context) : TextToSpeech.OnInitListener {

  private var mediaPlayer: MediaPlayer? = null
  private var textToSpeech: TextToSpeech? = null
  private var isTtsReady = false

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  init {
    try {
      textToSpeech = TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
      Log.w(TAG, "TTS initialization error: ${e.message}")
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val guLocale = Locale("gu", "IN")
      val res = textToSpeech?.setLanguage(guLocale)
      if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
        textToSpeech?.setLanguage(Locale("hi", "IN"))
      }
      isTtsReady = true
    }
  }

  /**
   * Plays the designated high-fidelity human Gujarati voice audio clip.
   * If the raw audio resource cannot be played, falls back to Gujarati TTS.
   */
  fun playAlert(rawResId: Int, fallbackGujaratiText: String) {
    try {
      requestAudioFocus()

      mediaPlayer?.release()
      mediaPlayer = null

      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

      val player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val sessionId = audioManager?.generateAudioSessionId() ?: AudioManager.AUDIO_SESSION_ID_GENERATE
        MediaPlayer.create(context, rawResId, audioAttributes, sessionId)
      } else {
        MediaPlayer.create(context, rawResId)
      }

      mediaPlayer = player?.apply {
        setOnCompletionListener { mp ->
          mp.release()
          mediaPlayer = null
          abandonAudioFocus()
        }
        setOnErrorListener { mp, _, _ ->
          mp.release()
          mediaPlayer = null
          abandonAudioFocus()
          playFallbackTts(fallbackGujaratiText)
          true
        }
        start()
      }

      if (mediaPlayer == null) {
        playFallbackTts(fallbackGujaratiText)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to play human audio clip: ${e.message}")
      playFallbackTts(fallbackGujaratiText)
    }
  }

  private fun playFallbackTts(text: String) {
    if (!isTtsReady || textToSpeech == null) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "water_alert_${System.currentTimeMillis()}")
      } else {
        @Suppress("DEPRECATION")
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
      }
    } catch (e: Exception) {
      Log.w(TAG, "TTS speak failed: ${e.message}")
    }
  }

  private fun requestAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .build()
      audioManager?.requestAudioFocus(focusRequest)
    } else {
      @Suppress("DEPRECATION")
      audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    }
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Audio focus automatically cleared
    } else {
      @Suppress("DEPRECATION")
      audioManager?.abandonAudioFocus(null)
    }
  }

  fun release() {
    try {
      mediaPlayer?.release()
      mediaPlayer = null
      textToSpeech?.stop()
      textToSpeech?.shutdown()
      textToSpeech = null
    } catch (_: Exception) {}
  }

  companion object {
    private const val TAG = "AudioAlertPlayer"
  }
}
