package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothController
import com.example.data.model.AppSettings
import com.example.data.model.BluetoothDeviceItem
import com.example.data.model.ConnectionState
import com.example.data.model.LogEntry
import com.example.data.model.LogType
import com.example.data.model.TankState
import com.example.data.repository.CloudGatewayRepository
import com.example.data.repository.GatewaySyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(application: Application) : AndroidViewModel(application) {

  val bluetoothController = BluetoothController(application.applicationContext)
  val cloudGatewayRepository = CloudGatewayRepository(application.applicationContext)

  val connectionState: StateFlow<ConnectionState> = bluetoothController.connectionState
  val isScanning: StateFlow<Boolean> = bluetoothController.isScanning
  val syncStatus: StateFlow<GatewaySyncStatus> = cloudGatewayRepository.syncStatus

  private val _isUserSignedIn = MutableStateFlow(cloudGatewayRepository.isUserSignedIn)
  val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

  private val _currentUserEmail = MutableStateFlow(cloudGatewayRepository.currentUserEmail)
  val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

  private var lastCloudSyncTime = 0L

  private val _settings = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  // Initial tank state with NO random readings
  private val _tankState = MutableStateFlow(TankState())
  val tankState: StateFlow<TankState> = _tankState.asStateFlow()

  private val _logs = MutableStateFlow<List<LogEntry>>(
    listOf(
      LogEntry(type = LogType.SYSTEM, text = "HydroSense Controller initialized"),
      LogEntry(type = LogType.SYSTEM, text = "Connect HC-05 to view live water level & control motor")
    )
  )
  val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = combine(
    bluetoothController.pairedDevices,
    _settings
  ) { list, cfg ->
    if (cfg.filterHc05Only) {
      list.filter { it.isHc05 }
    } else {
      list
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val scannedDevices: StateFlow<List<BluetoothDeviceItem>> = combine(
    bluetoothController.scannedDevices,
    _settings
  ) { list, cfg ->
    if (cfg.filterHc05Only) {
      list.filter { it.isHc05 }
    } else {
      list
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  private var timerJob: Job? = null

  init {
    viewModelScope.launch {
      bluetoothController.receivedMessages.collect { rawMessage ->
        handleIncomingData(rawMessage)
      }
    }

    viewModelScope.launch {
      bluetoothController.connectionState.collect { state ->
        when (state) {
          is ConnectionState.Connected -> {
            addLog(LogType.SYSTEM, "Connected to ${state.deviceName} (${state.address})")
            triggerHaptic(100)
            _tankState.value = _tankState.value.copy(statusMessage = "Connected • Waiting for telemetry")
            // Query status from Arduino
            delay(400)
            sendRawCommand(_settings.value.statusQueryCmd)
          }
          is ConnectionState.Disconnected -> {
            addLog(LogType.SYSTEM, "Disconnected from Bluetooth device")
            stopMotorTimer()
            // Reset to null so no fake or stale readings are shown without connection
            _tankState.value = TankState(
              targetLevel = _tankState.value.targetLevel,
              tankCapacityLiters = _tankState.value.tankCapacityLiters,
              statusMessage = "Disconnected"
            )
            // Immediately inform the cloud gateway so the public web monitor shows disconnected
            if (_settings.value.cloudGatewayEnabled && _isUserSignedIn.value) {
              viewModelScope.launch {
                cloudGatewayRepository.syncDeviceDisconnected()
              }
            }
          }
          is ConnectionState.Error -> {
            addLog(LogType.ERROR, state.message)
            _tankState.value = _tankState.value.copy(statusMessage = "Error: ${state.message}")
            if (_settings.value.cloudGatewayEnabled && _isUserSignedIn.value) {
              viewModelScope.launch {
                cloudGatewayRepository.syncDeviceDisconnected()
              }
            }
          }
          else -> {}
        }
      }
    }

    FirebaseAuth.getInstance().addAuthStateListener { auth ->
      val user = auth.currentUser
      _isUserSignedIn.value = user != null
      _currentUserEmail.value = user?.email
    }

    bluetoothController.refreshPairedDevices()
  }

  fun startScan() {
    addLog(LogType.SYSTEM, "Scanning for Bluetooth devices (HC-05 prioritized)...")
    bluetoothController.startScanning()
  }

  fun stopScan() {
    bluetoothController.stopScanning()
    addLog(LogType.SYSTEM, "Bluetooth scanning stopped")
  }

  fun quickConnectHc05() {
    bluetoothController.refreshPairedDevices()
    val paired = bluetoothController.pairedDevices.value
    val target = paired.firstOrNull { it.isHc05 }
      ?: bluetoothController.scannedDevices.value.firstOrNull { it.isHc05 }

    if (target != null) {
      addLog(LogType.SYSTEM, "Auto-connecting to detected HC-05: ${target.name} (${target.address})")
      bluetoothController.connect(target.address, target.name)
    } else {
      addLog(LogType.WARNING, "No paired HC-05 found. Starting Bluetooth scan...")
      startScan()
    }
  }

  fun connectDevice(device: BluetoothDeviceItem) {
    addLog(LogType.SYSTEM, "Connecting to ${device.name} [${device.address}]...")
    bluetoothController.connect(device.address, device.name)
  }

  fun disconnect() {
    bluetoothController.disconnect()
  }

  fun toggleMotor() {
    val isConnected = connectionState.value is ConnectionState.Connected
    if (!isConnected) {
      addLog(LogType.WARNING, "Cannot operate motor: HC-05 is not connected")
      return
    }

    val current = _tankState.value.isMotorOn
    val newState = !current
    val cmd = if (newState) _settings.value.motorOnCmd else _settings.value.motorOffCmd

    addLog(LogType.TX, "$cmd (${if (newState) "START MOTOR" else "STOP MOTOR"})")
    triggerHaptic(60)
    sendRawCommand(cmd)
  }

  fun setTargetLevel(newTarget: Float) {
    val clamped = newTarget.coerceIn(1.0f, 95.0f)
    _tankState.value = _tankState.value.copy(targetLevel = clamped)
    val cmd = "TARGET:${String.format(java.util.Locale.US, "%.1f", clamped)}"
    addLog(LogType.TX, "$cmd (Target Level set to ${clamped.toInt()}%)")
    triggerHaptic(60)
    if (connectionState.value is ConnectionState.Connected) {
      sendRawCommand(cmd)
    }
  }

  fun requestStatus() {
    if (connectionState.value is ConnectionState.Connected) {
      addLog(LogType.TX, "STATUS (Query system state)")
      sendRawCommand("STATUS")
    }
  }

  fun ping() {
    if (connectionState.value is ConnectionState.Connected) {
      addLog(LogType.TX, "PING")
      sendRawCommand("PING")
    }
  }

  fun setMotorState(isOn: Boolean) {
    _tankState.value = _tankState.value.copy(
      isMotorOn = isOn,
      statusMessage = if (isOn) "Motor Running" else "Motor Standby"
    )

    if (isOn) {
      startMotorTimer()
    } else {
      stopMotorTimer()
    }
  }

  fun sendCustomCommand(cmd: String) {
    if (cmd.isBlank()) return
    addLog(LogType.TX, "TX: $cmd")
    sendRawCommand(cmd)
  }

  private fun sendRawCommand(cmd: String) {
    val fullCmd = cmd + _settings.value.lineEnding.delimiter
    viewModelScope.launch {
      bluetoothController.sendData(fullCmd)
    }
  }

  private fun handleIncomingData(raw: String) {
    addLog(LogType.RX, "RX: $raw")

    try {
      parseHydroSensePayload(raw)
    } catch (e: Exception) {
      addLog(LogType.ERROR, "Parse error: ${e.message}")
    }
  }

  private fun parseHydroSensePayload(payload: String) {
    val text = payload.trim()

    when {
      text == "HYDROSENSE:MEGA_READY" -> {
        addLog(LogType.SYSTEM, "✅ Arduino Mega 2560 Ready! Ready to control motor.")
        requestStatus()
        return
      }

      text == "ACK:MOTOR_ON:OK" -> {
        setMotorState(true)
        addLog(LogType.SYSTEM, "Motor turned ON successfully (Relay D7 Active)")
        triggerHaptic(80)
        return
      }

      text == "ACK:MOTOR_ON:ERROR" -> {
        addLog(LogType.WARNING, "⚠️ Motor ON Rejected by Arduino: Water level >= target/safety or sensor error")
        triggerHaptic(200)
        return
      }

      text == "ACK:MOTOR_OFF:OK" -> {
        setMotorState(false)
        addLog(LogType.SYSTEM, "Motor turned OFF successfully")
        return
      }

      text == "MOTOR_OFF:TARGET_REACHED" -> {
        setMotorState(false)
        _tankState.value = _tankState.value.copy(lastAlertReason = "Target Level Reached")
        addLog(LogType.WARNING, "🎯 TARGET REACHED: Motor stopped automatically by Arduino")
        triggerHaptic(300)
        return
      }

      text == "MOTOR_OFF:SAFETY_LIMIT" -> {
        setMotorState(false)
        _tankState.value = _tankState.value.copy(lastAlertReason = "Safety Limit 95%")
        addLog(LogType.WARNING, "🚨 SAFETY LIMIT 95%: Motor stopped by Arduino to prevent overflow")
        triggerHaptic(400)
        return
      }

      text == "MOTOR_OFF:SENSOR_TIMEOUT" -> {
        setMotorState(false)
        _tankState.value = _tankState.value.copy(lastAlertReason = "Sensor Timeout Failsafe")
        addLog(LogType.ERROR, "⚠️ SENSOR FAILSAFE: Ultrasonic sensor timeout. Motor shut down safely.")
        triggerHaptic(500)
        return
      }

      text.startsWith("ACK:TARGET:") -> {
        if (text.endsWith("OK")) {
          addLog(LogType.SYSTEM, "Target water level updated on Arduino")
        } else {
          addLog(LogType.WARNING, "Invalid target water level (Must be between 1.0% and 95.0%)")
        }
        return
      }

      text == "PONG" -> {
        addLog(LogType.SYSTEM, "Arduino PONG received")
        return
      }

      // Telemetry: "DISTANCE:8.50,LEVEL:47.3,MOTOR:OFF,TARGET:80.0"
      // Status: "STATUS:EMPTY:14.00,FULL:2.42,LEVEL:47.3,TARGET:80.0,MOTOR:OFF"
      text.contains("LEVEL:") || text.contains("DISTANCE:") -> {
        val clean = if (text.startsWith("STATUS:")) text.substring(7) else text
        val tokens = clean.split(",")

        var dist: Float? = _tankState.value.distanceCM
        var lvl: Float? = _tankState.value.levelPercent
        var motorOn = _tankState.value.isMotorOn
        var target = _tankState.value.targetLevel
        var emptyDist = _tankState.value.emptyDistanceCM
        var fullDist = _tankState.value.fullDistanceCM

        for (token in tokens) {
          val parts = token.split(":")
          if (parts.size >= 2) {
            val key = parts[0].trim().uppercase()
            val value = parts[1].trim()

            when (key) {
              "DISTANCE" -> dist = value.toFloatOrNull() ?: dist
              "LEVEL" -> lvl = value.toFloatOrNull() ?: lvl
              "MOTOR" -> motorOn = value.equals("ON", ignoreCase = true) || value == "1"
              "TARGET" -> target = value.toFloatOrNull() ?: target
              "EMPTY" -> emptyDist = value.toFloatOrNull() ?: emptyDist
              "FULL" -> fullDist = value.toFloatOrNull() ?: fullDist
            }
          }
        }

        _tankState.value = _tankState.value.copy(
          distanceCM = dist,
          levelPercent = lvl?.coerceIn(0f, 100f),
          isMotorOn = motorOn,
          targetLevel = target,
          emptyDistanceCM = emptyDist,
          fullDistanceCM = fullDist,
          lastTelemetryTime = System.currentTimeMillis(),
          statusMessage = if (motorOn) "Motor Running" else "Standby"
        )

        // Sync motor timer
        if (motorOn && timerJob == null) {
          startMotorTimer()
        } else if (!motorOn && timerJob != null) {
          stopMotorTimer()
        }

        // Auto-broadcast to Cloud Gateway if enabled and user signed in
        if (_settings.value.cloudGatewayEnabled && _isUserSignedIn.value) {
          val now = System.currentTimeMillis()
          if (now - lastCloudSyncTime >= 3000L) {
            lastCloudSyncTime = now
            viewModelScope.launch {
              cloudGatewayRepository.syncTankTelemetry(_tankState.value)
            }
          }
        }
        return
      }
    }
  }

  fun manualSyncCloud() {
    viewModelScope.launch {
      if (!_isUserSignedIn.value) {
        addLog(LogType.WARNING, "Cloud sync requires Google Sign-In on gateway")
        return@launch
      }
      val res = cloudGatewayRepository.syncTankTelemetry(_tankState.value)
      if (res.isSuccess) {
        addLog(LogType.SYSTEM, "Cloud telemetry synced successfully")
      } else {
        addLog(LogType.ERROR, "Cloud sync error: ${res.exceptionOrNull()?.message}")
      }
    }
  }

  fun signInWithGoogleToken(idToken: String) {
    viewModelScope.launch {
      try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).await()
        addLog(LogType.SYSTEM, "Signed in as ${FirebaseAuth.getInstance().currentUser?.email}")
        if (_tankState.value.hasValidReading) {
          cloudGatewayRepository.syncTankTelemetry(_tankState.value)
        }
      } catch (e: Exception) {
        addLog(LogType.ERROR, "Sign-in failed: ${e.message}")
      }
    }
  }

  fun signOutCloudGateway() {
    cloudGatewayRepository.signOut()
    addLog(LogType.SYSTEM, "Signed out from Cloud Gateway")
  }

  private fun startMotorTimer() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (isActive && _tankState.value.isMotorOn) {
        delay(1000)
        _tankState.value = _tankState.value.copy(
          motorRunSeconds = _tankState.value.motorRunSeconds + 1
        )
      }
    }
  }

  private fun stopMotorTimer() {
    timerJob?.cancel()
    timerJob = null
    _tankState.value = _tankState.value.copy(motorRunSeconds = 0)
  }

  fun updateSettings(newSettings: AppSettings) {
    _settings.value = newSettings
    _tankState.value = _tankState.value.copy(tankCapacityLiters = newSettings.tankCapacityLiters)
    addLog(LogType.SYSTEM, "Configuration updated")
  }

  fun clearLogs() {
    _logs.value = listOf(
      LogEntry(type = LogType.SYSTEM, text = "Logs cleared")
    )
  }

  private fun addLog(type: LogType, text: String) {
    val entry = LogEntry(type = type, text = text)
    _logs.value = (_logs.value.takeLast(199) + entry)
  }

  private fun triggerHaptic(durationMs: Long) {
    if (!_settings.value.hapticAlerts) return
    try {
      val ctx = getApplication<Application>().applicationContext
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator?.vibrate(
          VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      } else {
        @Suppress("DEPRECATION")
        val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(
          VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      }
    } catch (_: Exception) {}
  }

  override fun onCleared() {
    super.onCleared()
    bluetoothController.unregister()
    timerJob?.cancel()
  }
}
