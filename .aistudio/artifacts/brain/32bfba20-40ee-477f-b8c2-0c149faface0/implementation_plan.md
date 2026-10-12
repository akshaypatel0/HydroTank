# Gujarati Voice & Background Notification Alert System

Implement real-time Gujarati human voice announcements and high-priority Android notifications for critical water tank milestones (25%, 50%, 75%, 95%, and target reached) while preserving 100% of the existing Bluetooth communication, Arduino protocol, and Cloud Gateway architecture.

### User Review & Critical Decisions

> [!IMPORTANT]
> All primary user preferences have been confirmed and are integrated into this plan:
> - **Milestone Thresholds**: Audio and notifications trigger at **25%, 50%, 75%, 95% (overflow safety)**, and **Target Level Reached**.
> - **Audio Engine**: High-fidelity bundled human Gujarati voice clips in `res/raw/` with zero latency, accompanied by a graceful Android Text-To-Speech (`gu-IN`) fallback.
> - **Background Operation**: Alerts will deliver high-priority heads-up notifications with sound/vibration and speak audio even when the phone is locked or the app is minimized.
> - **Zero Regression Guarantee**: No changes to existing Bluetooth HC-05 communication protocols, baud rates, Arduino packet formats, or cloud telemetry sync mechanisms.

---

### 1. Overview & Core Concept

- **What It Does**: As the Arduino HC-05 sends real-time water telemetry, HydroSense evaluates water level transitions. When the water level crosses 25%, 50%, 75%, 95%, or when the pump automatically shuts off because target level is reached, the app announces the exact status in a natural Gujarati voice (e.g. *"પાણીની ટાંકી 50 ટકા ભરાઈ ગઈ છે"*) and posts an Android heads-up notification with action buttons and haptic vibration.
- **Target Audience**: Homeowners, farm operators, and caretakers who cannot keep staring at their phone screen and need clear, audible voice announcements in their native language when the water tank fills up.
- **Key Value**: Hands-free peace of mind without touching the phone, preventing water overflow and ensuring timely pump shutdown awareness.

---

### 2. User Experience & Visual Design

- **Audio Announcements (Natural Gujarati Voice)**:
  - **25% Milestone**: *"પાણીની ટાંકી 25 ટકા ભરાઈ ગઈ છે."*
  - **50% Milestone**: *"પાણીની ટાંકી 50 ટકા ભરાઈ ગઈ છે."*
  - **75% Milestone**: *"પાણીની ટાંકી 75 ટકા ભરાઈ ગઈ છે."*
  - **95% Critical Alert**: *"ચેતવણી! પાણીની ટાંકી 95 ટકા ભરાઈ ગઈ છે. ટાંકી છલકાઈ શકે છે."*
  - **Target Reached**: *"ટાર્ગેટ લેવલ પૂરું થઈ ગયું છે. મોટર બંધ થઈ ગઈ છે."*
  - **Motor Started**: *"મોટર શરૂ થઈ ગઈ છે."* (Optional switch in settings)

- **Notification Experience**:
  - High-priority Notification Channel: `water_level_alerts` with maximum importance (Heads-up banner on lock screen).
  - Clean bilingual notification cards (Gujarati title & English subtext + live level percentage).
  - Quick action button on notification: **"Open App"** to review the live dashboard.

- **Settings Screen Controls ("Voice & Notification Alerts" Card)**:
  - Master toggle: **Enable Voice Alerts (ગુજરાતી અવાજ)**.
  - Master toggle: **Enable Push Notifications**.
  - Volume slider and "Test Voice Alert" preview button so the user can sample the audio clips immediately.
  - Haptic feedback pattern customization (standard vs urgent vibration).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Bundled Pre-recorded Audio Clips in `res/raw/` with TTS Fallback**
  - *Chosen Approach*: Store studio-quality human voice clips (OGG/MP3/WAV) in `app/src/main/res/raw/` for instant offline playback via `MediaPlayer`, with an automatic fallback to Android's built-in Gujarati `TextToSpeech` engine if an audio resource is unavailable.
  - *Why*: Pre-recorded human voice clips sound 100% natural and pleasant, exactly as requested by the user, without robotic artifacts or internet dependency.
  - *Alternatives Considered*: Pure TTS engine alone was rejected because synthesized Gujarati TTS on some Android phone models can sound robotic or English-accented.

- **Decision 2: State-Machine Debouncing & Hysteresis Filtering**
  - *Chosen Approach*: Keep an alert state tracker in the ViewModel/AlertManager that requires water level to climb into a threshold window and only trigger once per filling session (e.g., must cross from <49% to >=50% to fire the 50% alert; will not re-trigger on sensor jitter between 49.9% and 50.1%).
  - *Why*: Ultrasonic water surface ripples can oscillate readings. Without debouncing, the app would repeat the announcement multiple times in a minute.

- **Decision 3: Dedicated Alert Notification Service & Channel**
  - *Chosen Approach*: Use Android `NotificationCompat` with `NotificationManager.IMPORTANCE_HIGH` and request `POST_NOTIFICATIONS` runtime permission on Android 13+.
  - *Why*: Ensures alerts break through lock screens and background battery optimizations without requiring heavy foreign libraries.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Arduino + HC-05 (UART)                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ "DISTANCE:xx,LEVEL:50.0"
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  BluetoothController (Unchanged)            │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow<ReceivedMessage>
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       MainViewModel                         │
│  - Parses live telemetry into TankState                     │
│  - Feeds reading to WaterAlertManager                       │
└──────────────────────────────┬──────────────────────────────┘
                               │ State Transition Detected
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     WaterAlertManager                       │
│  - Hysteresis & Debounce Gate (25%, 50%, 75%, 95%, Target)   │
│  - AudioFocus request (AudioManager)                        │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
                ▼                             ▼
┌───────────────────────────────┐ ┌───────────────────────────┐
│     AudioAlertPlayer          │ │  WaterNotificationManager │
│ - Plays res/raw/human clips   │ │ - POST_NOTIFICATIONS      │
│ - Android TTS (gu-IN) fallback│ │ - Heads-up banner         │
│ - AudioFocus & background ok  │ │ - Vibration pattern       │
└───────────────────────────────┘ └───────────────────────────┘
```

#### Step-by-Step Implementation Flow

1. **Android Permissions & Manifest (`AndroidManifest.xml`)**:
   - Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`
   - Add `<uses-permission android:name="android.permission.VIBRATE"/>`
   - Add runtime permission request handling in `MainActivity.kt` on Android 13+.

2. **Gujarati Audio Voice Assets (`res/raw/`)**:
   - Provide clear, natural human audio recordings for:
     - `alert_level_25.mp3`
     - `alert_level_50.mp3`
     - `alert_level_75.mp3`
     - `alert_level_95.mp3`
     - `alert_target_reached.mp3`
     - `alert_motor_on.mp3`

3. **Audio & Alert Manager Engine (`com.example.alerts`)**:
   - Create `WaterAlertManager.kt`: monitors `TankState.levelPercent` and `isMotorOn`, evaluates threshold transitions with hysteresis (minimum 3% gap before resetting), triggers notifications and audio.
   - Create `AudioAlertPlayer.kt`: manages `MediaPlayer` for raw audio assets with proper `AudioAttributes.USAGE_ALARM` / `USAGE_NOTIFICATION` and a Gujarati TTS fallback.
   - Create `NotificationHelper.kt`: creates the high-importance notification channel and dispatches heads-up notifications with Gujarati and English text.

4. **Settings & Customization UI (`SettingsScreen.kt` & `AppSettings.kt`)**:
   - Add toggles in `AppSettings`: `voiceAlertsEnabled` (default `true`), `pushNotificationsEnabled` (default `true`).
   - Add a test button in `SettingsScreen`: "અવાજ ચકાસો (Test Voice Alert)" so the user can immediately verify volume and pronunciation.

5. **Verification & Build**:
   - Run `compile_applet` to verify compilation, permissions, and zero regressions across Bluetooth, Firestore, and UI.
