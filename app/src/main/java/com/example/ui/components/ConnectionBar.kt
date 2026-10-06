package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.data.model.ConnectionState
import com.example.ui.theme.PumpActiveGreen
import com.example.ui.theme.WarningAmber

@Composable
fun ConnectionBar(
  connectionState: ConnectionState,
  onOpenDeviceSheet: () -> Unit,
  onDisconnect: () -> Unit,
  onQuickConnect: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isConnected = connectionState is ConnectionState.Connected
  val isConnecting = connectionState is ConnectionState.Connecting

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("bluetooth_connection_bar"),
    shape = RoundedCornerShape(18.dp),
    color = when {
      isConnected -> PumpActiveGreen.copy(alpha = 0.12f)
      isConnecting -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    },
    tonalElevation = 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status Dot / Icon
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
              when {
                isConnected -> PumpActiveGreen.copy(alpha = 0.2f)
                isConnecting -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          when {
            isConnecting -> {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            }
            isConnected -> {
              Icon(
                imageVector = Icons.Default.BluetoothConnected,
                contentDescription = "Connected",
                tint = PumpActiveGreen,
                modifier = Modifier.size(20.dp)
              )
            }
            else -> {
              Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Disconnected",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          when (connectionState) {
            is ConnectionState.Connected -> {
              Text(
                text = connectionState.deviceName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "HC-05 Paired • ${connectionState.address}",
                style = MaterialTheme.typography.labelSmall,
                color = PumpActiveGreen
              )
            }
            is ConnectionState.Connecting -> {
              Text(
                text = "Connecting to ${connectionState.deviceName}...",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Establishing RFCOMM SPP channel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            is ConnectionState.Error -> {
              Text(
                text = "Connection Failed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = connectionState.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
              )
            }
            else -> {
              Text(
                text = "HC-05 Not Connected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Tap to pair & link Arduino",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Action button
      if (isConnected) {
        OutlinedButton(
          onClick = onDisconnect,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("bluetooth_disconnect_button")
        ) {
          Text(text = "Disconnect", fontSize = 12.sp)
        }
      } else {
        FilledTonalButton(
          onClick = onOpenDeviceSheet,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("bluetooth_connect_sheet_button")
        ) {
          Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Connect HC-05", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
