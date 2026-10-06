package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.BluetoothDeviceItem
import com.example.data.model.ConnectionState
import com.example.ui.theme.PumpActiveGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListSheet(
  sheetState: SheetState,
  pairedDevices: List<BluetoothDeviceItem>,
  scannedDevices: List<BluetoothDeviceItem>,
  isScanning: Boolean,
  connectionState: ConnectionState,
  settings: AppSettings,
  onDismiss: () -> Unit,
  onStartScan: () -> Unit,
  onStopScan: () -> Unit,
  onQuickConnect: () -> Unit,
  onConnectDevice: (BluetoothDeviceItem) -> Unit,
  onUpdateSettings: (AppSettings) -> Unit,
  onEnsurePermissions: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .testTag("device_list_bottom_sheet")
    ) {
      // Sheet Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "HC-05 Bluetooth Setup",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = "Seamless pairing for Arduino water tank & pump",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = {
            onEnsurePermissions()
            if (isScanning) onStopScan() else onStartScan()
          },
          modifier = Modifier.testTag("scan_toggle_icon_button")
        ) {
          if (isScanning) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Scan",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 1-Tap Quick Connect Button
      Button(
        onClick = {
          onEnsurePermissions()
          onQuickConnect()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("quick_connect_hc05_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(
          imageVector = Icons.Default.FlashOn,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "1-Tap Quick Connect to HC-05",
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // HC-05 Strict Filter Toggle Bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "HC-05 Only Filter",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (settings.filterHc05Only) "Only detects HC-05/BT-05 modules" else "Showing all Bluetooth devices",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = settings.filterHc05Only,
            onCheckedChange = { checked ->
              onUpdateSettings(settings.copy(filterHc05Only = checked))
            },
            modifier = Modifier.testTag("filter_hc05_switch")
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Devices List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
      ) {
        // Section 1: Paired Devices
        item {
          SectionHeader(
            title = "Paired HC-05 Devices",
            count = pairedDevices.size
          )
        }

        if (pairedDevices.isEmpty()) {
          item {
            EmptySectionPlaceholder(
              message = if (settings.filterHc05Only)
                "No paired HC-05 module found in Android settings. Pair in Bluetooth settings (PIN: 1234) or scan below."
              else
                "No paired devices found."
            )
          }
        } else {
          items(pairedDevices) { device ->
            DeviceRow(
              device = device,
              isConnecting = connectionState is ConnectionState.Connecting && connectionState.address == device.address,
              isConnected = connectionState is ConnectionState.Connected && connectionState.address == device.address,
              onConnect = {
                onEnsurePermissions()
                onConnectDevice(device)
              }
            )
          }
        }

        // Section 2: Scanned / Discovered Devices
        item {
          Spacer(modifier = Modifier.height(16.dp))
          SectionHeader(
            title = "Nearby Discovered Devices",
            count = scannedDevices.size,
            showLoading = isScanning
          )
        }

        if (scannedDevices.isEmpty()) {
          item {
            EmptySectionPlaceholder(
              message = if (isScanning)
                "Searching for nearby HC-05 signals..."
              else
                "Tap refresh at the top to discover nearby HC-05 modules."
            )
          }
        } else {
          items(scannedDevices) { device ->
            DeviceRow(
              device = device,
              isConnecting = connectionState is ConnectionState.Connecting && connectionState.address == device.address,
              isConnected = connectionState is ConnectionState.Connected && connectionState.address == device.address,
              onConnect = {
                onEnsurePermissions()
                onConnectDevice(device)
              }
            )
          }
        }

        // Section 3: Helpful Quick Pairing Tips
        item {
          Spacer(modifier = Modifier.height(16.dp))
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .size(18.dp)
                  .padding(top = 2.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "HC-05 Quick Pairing Tip",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "• Default pairing PIN code is 1234 or 0000.\n• Connect HC-05 VCC to 5V and GND to Arduino GND.\n• Check that the HC-05 red LED is blinking fast before pairing.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  count: Int,
  showLoading: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (showLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(14.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = "$count",
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun EmptySectionPlaceholder(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      fontSize = 12.sp
    )
  }
}

@Composable
private fun DeviceRow(
  device: BluetoothDeviceItem,
  isConnecting: Boolean,
  isConnected: Boolean,
  onConnect: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(enabled = !isConnecting && !isConnected, onClick = onConnect)
      .testTag("device_row_${device.address.replace(":", "_")}"),
    shape = RoundedCornerShape(12.dp),
    color = if (isConnected) PumpActiveGreen.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
              if (device.isHc05) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = if (device.isHc05) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = device.name,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold
            )
            if (device.isHc05) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "HC-05",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }
          Text(
            text = device.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }

      // Trailing status/action
      when {
        isConnected -> {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Connected",
            tint = PumpActiveGreen,
            modifier = Modifier.size(22.dp)
          )
        }
        isConnecting -> {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
          )
        }
        else -> {
          OutlinedButton(
            onClick = onConnect,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text(text = "Connect", fontSize = 12.sp)
          }
        }
      }
    }
  }
}
