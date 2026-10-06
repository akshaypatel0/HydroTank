package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ConnectionState
import com.example.ui.MainViewModel
import com.example.ui.screens.ArduinoGuideSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceListSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.HydroSenseTheme
import com.example.ui.theme.PumpActiveGreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      HydroSenseTheme {
        HydroTankApp()
      }
    }
  }
}

enum class AppNavTab(val label: String) {
  DASHBOARD("Tank"),
  TERMINAL("Serial Terminal"),
  SETTINGS("Config")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydroTankApp(viewModel: MainViewModel = viewModel()) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedTab by rememberSaveable { mutableIntStateOf(AppNavTab.DASHBOARD.ordinal) }

  val tankState by viewModel.tankState.collectAsStateWithLifecycle()
  val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val logs by viewModel.logs.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
  val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val isUserSignedIn by viewModel.isUserSignedIn.collectAsStateWithLifecycle()
  val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()

  var showDeviceSheet by remember { mutableStateOf(false) }
  var showArduinoSheet by remember { mutableStateOf(false) }

  val deviceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val arduinoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Bluetooth Permission Launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val allGranted = permissions.values.all { it }
    if (!allGranted) {
      Toast.makeText(
        context,
        "Bluetooth permission is required to connect to HC-05",
        Toast.LENGTH_LONG
      ).show()
    }
  }

  fun checkAndRequestPermissions(onGranted: () -> Unit = {}) {
    val neededPermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
      }
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
      }
    } else {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
      }
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH)
      }
    }

    if (neededPermissions.isNotEmpty()) {
      permissionLauncher.launch(neededPermissions.toTypedArray())
    } else {
      onGranted()
    }
  }

  // Handle back press to return to Dashboard tab
  if (selectedTab != AppNavTab.DASHBOARD.ordinal) {
    BackHandler {
      selectedTab = AppNavTab.DASHBOARD.ordinal
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "HydroSense",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          // Arduino Code Helper Action
          IconButton(
            onClick = { showArduinoSheet = true },
            modifier = Modifier.testTag("arduino_guide_action_button")
          ) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = "Arduino Code Guide",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Bluetooth Link Action
          IconButton(
            onClick = {
              checkAndRequestPermissions {
                showDeviceSheet = true
                viewModel.bluetoothController.refreshPairedDevices()
              }
            },
            modifier = Modifier.testTag("bluetooth_action_button")
          ) {
            BadgedBox(
              badge = {
                if (connectionState is ConnectionState.Connected) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(PumpActiveGreen)
                  )
                }
              }
            ) {
              Icon(
                imageVector = if (connectionState is ConnectionState.Connected)
                  Icons.Default.BluetoothConnected
                else
                  Icons.Default.Bluetooth,
                contentDescription = "Bluetooth HC-05",
                tint = if (connectionState is ConnectionState.Connected)
                  PumpActiveGreen
                else
                  MaterialTheme.colorScheme.onSurface
              )
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
        NavigationBarItem(
          selected = selectedTab == AppNavTab.DASHBOARD.ordinal,
          onClick = { selectedTab = AppNavTab.DASHBOARD.ordinal },
          icon = { Icon(Icons.Default.WaterDrop, contentDescription = "Tank") },
          label = { Text("Tank") },
          modifier = Modifier.testTag("tab_tank_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == AppNavTab.TERMINAL.ordinal,
          onClick = { selectedTab = AppNavTab.TERMINAL.ordinal },
          icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
          label = { Text("Terminal") },
          modifier = Modifier.testTag("tab_terminal")
        )
        NavigationBarItem(
          selected = selectedTab == AppNavTab.SETTINGS.ordinal,
          onClick = { selectedTab = AppNavTab.SETTINGS.ordinal },
          icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
          label = { Text("Settings") },
          modifier = Modifier.testTag("tab_settings")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tabContentTransition"
      ) { targetTab ->
        when (targetTab) {
          AppNavTab.DASHBOARD.ordinal -> {
            DashboardScreen(
              tankState = tankState,
              connectionState = connectionState,
              settings = settings,
              syncStatus = syncStatus,
              isUserSignedIn = isUserSignedIn,
              currentUserEmail = currentUserEmail,
              onToggleMotor = {
                checkAndRequestPermissions {
                  viewModel.toggleMotor()
                }
              },
              onSetTargetLevel = { target ->
                checkAndRequestPermissions {
                  viewModel.setTargetLevel(target)
                }
              },
              onOpenDeviceSheet = {
                checkAndRequestPermissions {
                  showDeviceSheet = true
                  viewModel.bluetoothController.refreshPairedDevices()
                }
              },
              onDisconnectBluetooth = { viewModel.disconnect() },
              onQuickConnect = {
                checkAndRequestPermissions {
                  viewModel.quickConnectHc05()
                }
              },
              onManualSync = { viewModel.manualSyncCloud() },
              onSignInWithGoogleToken = { token -> viewModel.signInWithGoogleToken(token) },
              onSignOut = { viewModel.signOutCloudGateway() }
            )
          }

          AppNavTab.TERMINAL.ordinal -> {
            TerminalScreen(
              logs = logs,
              onSendCommand = { cmd -> viewModel.sendCustomCommand(cmd) },
              onClearLogs = { viewModel.clearLogs() }
            )
          }

          AppNavTab.SETTINGS.ordinal -> {
            SettingsScreen(
              settings = settings,
              onSaveSettings = { updated -> viewModel.updateSettings(updated) }
            )
          }
        }
      }
    }

    // Modal Bottom Sheet: Device Pairing
    if (showDeviceSheet) {
      DeviceListSheet(
        sheetState = deviceSheetState,
        pairedDevices = pairedDevices,
        scannedDevices = scannedDevices,
        isScanning = isScanning,
        connectionState = connectionState,
        settings = settings,
        onDismiss = {
          scope.launch {
            deviceSheetState.hide()
            showDeviceSheet = false
          }
        },
        onStartScan = { viewModel.startScan() },
        onStopScan = { viewModel.stopScan() },
        onQuickConnect = { viewModel.quickConnectHc05() },
        onConnectDevice = { dev -> viewModel.connectDevice(dev) },
        onUpdateSettings = { cfg -> viewModel.updateSettings(cfg) },
        onEnsurePermissions = { checkAndRequestPermissions() }
      )
    }

    // Modal Bottom Sheet: Arduino Guide & Code
    if (showArduinoSheet) {
      ArduinoGuideSheet(
        sheetState = arduinoSheetState,
        onDismiss = {
          scope.launch {
            arduinoSheetState.hide()
            showArduinoSheet = false
          }
        }
      )
    }
  }
}
