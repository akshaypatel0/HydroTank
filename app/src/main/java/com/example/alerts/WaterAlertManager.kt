package com.example.alerts

import android.content.Context
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.TankState

class WaterAlertManager(private val context: Context) {

  private val audioPlayer = AudioAlertPlayer(context)
  private val notificationHelper = NotificationHelper(context)

  // State machine flags with hysteresis
  private var fired25 = false
  private var fired50 = false
  private var fired75 = false
  private var fired95 = false
  private var firedTarget = false

  private var previousLevel: Float? = null
  private var previousMotorState = false

  /**
   * Evaluates incoming real-time tank state against milestone thresholds.
   * Only triggers when valid real telemetry is received.
   */
  fun evaluateState(tankState: TankState, settings: AppSettings) {
    val level = tankState.levelPercent ?: return
    val prev = previousLevel ?: level

    // Check 25% Milestone
    if (settings.voiceMilestones25 && level >= 25f && prev < 24.5f && !fired25) {
      fired25 = true
      dispatchAlert(
        rawResId = R.raw.alert_level_25,
        gujaratiSpeech = "પાણીની ટાંકી 25 ટકા ભરાઈ ગઈ છે.",
        gujaratiTitle = "💧 ટાંકી 25% ભરાઈ ગઈ છે",
        englishSubtitle = "Water tank reached 25% capacity",
        notificationId = NotificationHelper.NOTIFICATION_ID_25,
        level = level.toInt(),
        isUrgent = false,
        settings = settings
      )
    } else if (level < 20f) {
      fired25 = false
    }

    // Check 50% Milestone
    if (settings.voiceMilestones50 && level >= 50f && prev < 49f && !fired50) {
      fired50 = true
      dispatchAlert(
        rawResId = R.raw.alert_level_50,
        gujaratiSpeech = "પાણીની ટાંકી 50 ટકા ભરાઈ ગઈ છે.",
        gujaratiTitle = "💧 ટાંકી 50% ભરાઈ ગઈ છે",
        englishSubtitle = "Water tank reached 50% capacity",
        notificationId = NotificationHelper.NOTIFICATION_ID_50,
        level = level.toInt(),
        isUrgent = false,
        settings = settings
      )
    } else if (level < 45f) {
      fired50 = false
    }

    // Check 75% Milestone
    if (settings.voiceMilestones75 && level >= 75f && prev < 74f && !fired75) {
      fired75 = true
      dispatchAlert(
        rawResId = R.raw.alert_level_75,
        gujaratiSpeech = "પાણીની ટાંકી 75 ટકા ભરાઈ ગઈ છે.",
        gujaratiTitle = "💧 ટાંકી 75% ભરાઈ ગઈ છે",
        englishSubtitle = "Water tank reached 75% capacity",
        notificationId = NotificationHelper.NOTIFICATION_ID_75,
        level = level.toInt(),
        isUrgent = false,
        settings = settings
      )
    } else if (level < 70f) {
      fired75 = false
    }

    // Check 95% Hard Safety / Overflow Limit
    if (settings.voiceMilestones95 && level >= 95f && prev < 94f && !fired95) {
      fired95 = true
      dispatchAlert(
        rawResId = R.raw.alert_level_95,
        gujaratiSpeech = "ચેતવણી! પાણીની ટાંકી 95 ટકા ભરાઈ ગઈ છે. મોટર બંધ કરો.",
        gujaratiTitle = "🚨 ચેતવણી! ટાંકી 95% ભરાઈ ગઈ છે",
        englishSubtitle = "Critical: Tank 95% full. Prevent overflow!",
        notificationId = NotificationHelper.NOTIFICATION_ID_95,
        level = level.toInt(),
        isUrgent = true,
        settings = settings
      )
    } else if (level < 90f) {
      fired95 = false
    }

    // Check Target Reached Milestone
    val target = tankState.targetLevel
    if (settings.voiceTargetReached && level >= target && prev < (target - 0.5f) && !firedTarget) {
      firedTarget = true
      dispatchAlert(
        rawResId = R.raw.alert_target_reached,
        gujaratiSpeech = "પાણીનો ટાર્ગેટ પૂરો થઈ ગયો છે. મોટર બંધ થઈ ગઈ છે.",
        gujaratiTitle = "🎯 ટાર્ગેટ લેવલ પૂરું થઈ ગયું છે",
        englishSubtitle = "Target level (${target.toInt()}%) reached",
        notificationId = NotificationHelper.NOTIFICATION_ID_TARGET,
        level = level.toInt(),
        isUrgent = false,
        settings = settings
      )
    } else if (level < (target - 5f)) {
      firedTarget = false
    }

    previousLevel = level
    previousMotorState = tankState.isMotorOn
  }

  /**
   * Called directly when Arduino transmits an explicit TARGET_REACHED or SAFETY_LIMIT event.
   */
  fun onTargetReachedEvent(settings: AppSettings) {
    if (!settings.voiceTargetReached) return
    dispatchAlert(
      rawResId = R.raw.alert_target_reached,
      gujaratiSpeech = "પાણીનો ટાર્ગેટ પૂરો થઈ ગયો છે. મોટર બંધ થઈ ગઈ છે.",
      gujaratiTitle = "🎯 ટાર્ગેટ લેવલ પૂરું થઈ ગયું છે",
      englishSubtitle = "Motor stopped automatically by Arduino",
      notificationId = NotificationHelper.NOTIFICATION_ID_TARGET,
      level = previousLevel?.toInt(),
      isUrgent = false,
      settings = settings
    )
  }

  fun onSafetyLimitEvent(settings: AppSettings) {
    if (!settings.voiceMilestones95) return
    dispatchAlert(
      rawResId = R.raw.alert_level_95,
      gujaratiSpeech = "ચેતવણી! પાણીની ટાંકી 95 ટકા ભરાઈ ગઈ છે. મોટર બંધ કરો.",
      gujaratiTitle = "🚨 ચેતવણી! સલામતી મર્યાદા 95%",
      englishSubtitle = "Safety limit reached. Motor stopped by Arduino.",
      notificationId = NotificationHelper.NOTIFICATION_ID_95,
      level = 95,
      isUrgent = true,
      settings = settings
    )
  }

  private fun dispatchAlert(
    rawResId: Int,
    gujaratiSpeech: String,
    gujaratiTitle: String,
    englishSubtitle: String,
    notificationId: Int,
    level: Int?,
    isUrgent: Boolean,
    settings: AppSettings
  ) {
    if (settings.voiceAlertsEnabled) {
      audioPlayer.playAlert(rawResId, gujaratiSpeech)
    }
    if (settings.pushNotificationsEnabled) {
      notificationHelper.showWaterMilestoneNotification(
        id = notificationId,
        gujaratiTitle = gujaratiTitle,
        englishSubtitle = englishSubtitle,
        levelPercent = level,
        isUrgent = isUrgent
      )
    }
  }

  /**
   * Preview button handler for Settings screen to test voice and notification delivery.
   */
  fun testVoiceAlert(milestone: Int) {
    val resId = when (milestone) {
      25 -> R.raw.alert_level_25
      75 -> R.raw.alert_level_75
      95 -> R.raw.alert_level_95
      else -> R.raw.alert_level_50
    }
    val speech = "પાણીની ટાંકી $milestone ટકા ભરાઈ ગઈ છે."
    audioPlayer.playAlert(resId, speech)
    notificationHelper.showWaterMilestoneNotification(
      id = NotificationHelper.NOTIFICATION_ID_TEST,
      gujaratiTitle = "💧 ટેસ્ટ એલર્ટ ($milestone%)",
      englishSubtitle = "Gujarati voice alert preview",
      levelPercent = milestone,
      isUrgent = milestone >= 95
    )
  }

  fun resetFiredMilestones() {
    fired25 = false
    fired50 = false
    fired75 = false
    fired95 = false
    firedTarget = false
    previousLevel = null
  }

  fun release() {
    audioPlayer.release()
  }
}
