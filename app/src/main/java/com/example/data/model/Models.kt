package com.example.data.model

enum class LogType {
  TX, RX, SYSTEM, WARNING, ERROR
}

data class LogEntry(
  val id: Long = System.currentTimeMillis() + (0..999).random(),
  val type: LogType,
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class BluetoothDeviceItem(
  val name: String,
  val address: String,
  val isBonded: Boolean,
  val isHc05: Boolean,
  val rssi: Int? = null
)

sealed interface ConnectionState {
  data object Disconnected : ConnectionState
  data object Scanning : ConnectionState
  data class Connecting(val deviceName: String, val address: String) : ConnectionState
  data class Connected(val deviceName: String, val address: String) : ConnectionState
  data class Error(val message: String) : ConnectionState
}

enum class LineEnding(val label: String, val delimiter: String) {
  NEWLINE("LF (\\n)", "\n"),
  CARRIAGE_RETURN_NEWLINE("CRLF (\\r\\n)", "\r\n"),
  NONE("None", "")
}

data class AppSettings(
  val motorOnCmd: String = "MOTOR_ON",
  val motorOffCmd: String = "MOTOR_OFF",
  val statusQueryCmd: String = "STATUS",
  val pingCmd: String = "PING",
  val lineEnding: LineEnding = LineEnding.NEWLINE,
  val autoCutoffEnabled: Boolean = true,
  val cutoffThresholdPercent: Int = 95,
  val lowLevelAlertPercent: Int = 20,
  val tankCapacityLiters: Float = 1000f,
  val tankHeightCm: Float = 14.0f,
  val filterHc05Only: Boolean = true,
  val autoReconnect: Boolean = true,
  val hapticAlerts: Boolean = true,
  val cloudGatewayEnabled: Boolean = true,
  val cloudGatewayUrl: String = "https://nrahir778.github.io/HydroTank/"
)

data class TankState(
  val levelPercent: Float? = null,
  val targetLevel: Float = 80.0f,
  val distanceCM: Float? = null,
  val emptyDistanceCM: Float = 14.00f,
  val fullDistanceCM: Float = 2.42f,
  val safetyLevel: Float = 95.0f,
  val isMotorOn: Boolean = false,
  val isAutoMode: Boolean = false,
  val motorRunSeconds: Long = 0L,
  val tankCapacityLiters: Float = 1000f,
  val lastTelemetryTime: Long = 0L,
  val statusMessage: String = "Disconnected",
  val lastAlertReason: String? = null
) {
  val hasValidReading: Boolean
    get() = levelPercent != null

  val volumeLiters: Float?
    get() = levelPercent?.let { (it / 100f) * tankCapacityLiters }

  val isCriticalLow: Boolean
    get() = (levelPercent ?: 0f) <= 15f && hasValidReading

  val isFullOrOverflow: Boolean
    get() = (levelPercent ?: 0f) >= safetyLevel && hasValidReading
}
