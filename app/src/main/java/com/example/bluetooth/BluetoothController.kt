package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.data.model.BluetoothDeviceItem
import com.example.data.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class BluetoothController(private val context: Context) {

  companion object {
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  }

  private val bluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
  val scannedDevices: StateFlow<List<BluetoothDeviceItem>> = _scannedDevices.asStateFlow()

  private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
  val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

  private val _receivedMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
  val receivedMessages: SharedFlow<String> = _receivedMessages.asSharedFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private var activeSocket: BluetoothSocket? = null
  private var outputStream: OutputStream? = null
  private var readJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.IO + Job())

  private var isReceiverRegistered = false

  private val receiver = object : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.action) {
        BluetoothDevice.ACTION_FOUND -> {
          val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          }
          val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

          device?.let { addScannedDevice(it, rssi) }
        }
        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
          _isScanning.value = false
        }
        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
          refreshPairedDevices()
        }
      }
    }
  }

  fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

  fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

  @SuppressLint("MissingPermission")
  fun refreshPairedDevices() {
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
      _pairedDevices.value = emptyList()
      return
    }

    try {
      val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
      val list = bonded.map { device ->
        val name = device.name ?: "Unknown Device"
        BluetoothDeviceItem(
          name = name,
          address = device.address,
          isBonded = true,
          isHc05 = isHc05DeviceName(name)
        )
      }
      _pairedDevices.value = list
    } catch (_: SecurityException) {
      _pairedDevices.value = emptyList()
    }
  }

  @SuppressLint("MissingPermission")
  fun startScanning() {
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

    refreshPairedDevices()
    _scannedDevices.value = emptyList()
    _isScanning.value = true

    registerReceiver()

    try {
      if (bluetoothAdapter.isDiscovering) {
        bluetoothAdapter.cancelDiscovery()
      }
      bluetoothAdapter.startDiscovery()
    } catch (_: SecurityException) {
      _isScanning.value = false
    }
  }

  @SuppressLint("MissingPermission")
  fun stopScanning() {
    try {
      if (bluetoothAdapter?.isDiscovering == true) {
        bluetoothAdapter.cancelDiscovery()
      }
    } catch (_: SecurityException) {
      // Ignored
    }
    _isScanning.value = false
  }

  @SuppressLint("MissingPermission")
  private fun addScannedDevice(device: BluetoothDevice, rssi: Int) {
    val name = device.name ?: "Unknown Device"
    val item = BluetoothDeviceItem(
      name = name,
      address = device.address,
      isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
      isHc05 = isHc05DeviceName(name),
      rssi = if (rssi != Short.MIN_VALUE.toInt()) rssi else null
    )

    val current = _scannedDevices.value.toMutableList()
    val index = current.indexOfFirst { it.address == item.address }
    if (index >= 0) {
      current[index] = item
    } else {
      current.add(item)
    }
    _scannedDevices.value = current
  }

  fun isHc05DeviceName(name: String?): Boolean {
    if (name.isNullOrBlank()) return false
    val clean = name.trim().uppercase()
    return clean.contains("HC-05") ||
      clean.contains("HC05") ||
      clean.contains("HC-06") ||
      clean.contains("BT-05") ||
      clean.startsWith("HC")
  }

  @SuppressLint("MissingPermission")
  fun connect(deviceAddress: String, targetDeviceName: String? = null) {
    if (bluetoothAdapter == null) {
      _connectionState.value = ConnectionState.Error("Bluetooth not available on this device")
      return
    }

    stopScanning()

    scope.launch {
      disconnectInternal()

      val name = targetDeviceName ?: "HC-05"
      _connectionState.value = ConnectionState.Connecting(deviceName = name, address = deviceAddress)

      try {
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val devName = device.name ?: name

        // Attempt primary SPP connection
        var socket: BluetoothSocket? = null
        try {
          socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
          socket.connect()
        } catch (e: Exception) {
          // Fallback reflection trick for HC-05 modules with custom firmware
          try {
            socket?.close()
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            socket = m.invoke(device, 1) as? BluetoothSocket
            socket?.connect()
          } catch (fallbackEx: Exception) {
            socket?.close()
            throw IOException("Failed to connect to $devName: ${e.message ?: fallbackEx.message}")
          }
        }

        activeSocket = socket
        outputStream = socket?.outputStream

        _connectionState.value = ConnectionState.Connected(deviceName = devName, address = deviceAddress)

        startReadingStream(socket!!)
      } catch (e: Exception) {
        disconnectInternal()
        _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
      }
    }
  }

  private fun startReadingStream(socket: BluetoothSocket) {
    readJob?.cancel()
    readJob = scope.launch(Dispatchers.IO) {
      try {
        val inputStream = socket.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))

        while (isActive && socket.isConnected) {
          val line = reader.readLine()
          if (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
              _receivedMessages.emit(trimmed)
            }
          } else {
            // End of stream - device disconnected
            break
          }
        }
      } catch (e: Exception) {
        // Disconnected or read failure
      } finally {
        withContext(Dispatchers.Main) {
          if (_connectionState.value is ConnectionState.Connected) {
            _connectionState.value = ConnectionState.Disconnected
          }
        }
      }
    }
  }

  suspend fun sendData(data: String): Boolean = withContext(Dispatchers.IO) {
    try {
      val out = outputStream ?: return@withContext false
      out.write(data.toByteArray())
      out.flush()
      true
    } catch (e: Exception) {
      withContext(Dispatchers.Main) {
        _connectionState.value = ConnectionState.Error("Failed to send data: ${e.message}")
      }
      false
    }
  }

  fun disconnect() {
    scope.launch {
      disconnectInternal()
      withContext(Dispatchers.Main) {
        _connectionState.value = ConnectionState.Disconnected
      }
    }
  }

  private fun disconnectInternal() {
    readJob?.cancel()
    readJob = null
    try {
      outputStream?.close()
    } catch (_: Exception) {}
    outputStream = null
    try {
      activeSocket?.close()
    } catch (_: Exception) {}
    activeSocket = null
  }

  private fun registerReceiver() {
    if (!isReceiverRegistered) {
      val filter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
      }
      context.registerReceiver(receiver, filter)
      isReceiverRegistered = true
    }
  }

  fun unregister() {
    if (isReceiverRegistered) {
      try {
        context.unregisterReceiver(receiver)
      } catch (_: Exception) {}
      isReceiverRegistered = false
    }
    stopScanning()
    disconnectInternal()
  }
}
