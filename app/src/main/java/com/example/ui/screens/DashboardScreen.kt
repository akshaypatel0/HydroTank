package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.ConnectionState
import com.example.data.model.TankState
import com.example.data.repository.GatewaySyncStatus
import com.example.ui.components.CloudGatewayCard
import com.example.ui.components.ConnectionBar
import com.example.ui.components.MotorControlCard
import com.example.ui.components.WaterTankVisualizer
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PumpActiveGreen
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
  tankState: TankState,
  connectionState: ConnectionState,
  settings: AppSettings,
  syncStatus: GatewaySyncStatus,
  isUserSignedIn: Boolean,
  currentUserEmail: String?,
  onToggleMotor: () -> Unit,
  onSetTargetLevel: (Float) -> Unit,
  onOpenDeviceSheet: () -> Unit,
  onDisconnectBluetooth: () -> Unit,
  onQuickConnect: () -> Unit,
  onManualSync: () -> Unit,
  onSignInWithGoogleToken: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("dashboard_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Bluetooth Connection Status Bar
    ConnectionBar(
      connectionState = connectionState,
      onOpenDeviceSheet = onOpenDeviceSheet,
      onDisconnect = onDisconnectBluetooth,
      onQuickConnect = onQuickConnect
    )

    // Disconnected Info Banner
    if (connectionState !is ConnectionState.Connected) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "HC-05 Disconnected — Connect to your HC-05 module to view live sensor readings and control the motor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      }
    }

    // 2. Animated Water Tank Graphic
    WaterTankVisualizer(
      tankState = tankState
    )

    // 3. Motor Relay Control Unit
    MotorControlCard(
      tankState = tankState,
      isConnected = connectionState is ConnectionState.Connected,
      onToggleMotor = onToggleMotor,
      onSetTargetLevel = onSetTargetLevel
    )

    // 4. Quick Safeguards Telemetry Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Card A: Safety Cutoff Status
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = DangerRed,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Hard Cut-Off",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Arduino limit: ${tankState.safetyLevel.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Card B: Target Level Status
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Water,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Target Level",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Auto-stop: ${tankState.targetLevel.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 5. Cloud IoT Gateway & Public Web Monitor Share
    CloudGatewayCard(
      settings = settings,
      syncStatus = syncStatus,
      isUserSignedIn = isUserSignedIn,
      currentUserEmail = currentUserEmail,
      onManualSync = onManualSync,
      onSignInWithGoogleToken = onSignInWithGoogleToken,
      onSignOut = onSignOut
    )

    Spacer(modifier = Modifier.height(28.dp))
  }
}
