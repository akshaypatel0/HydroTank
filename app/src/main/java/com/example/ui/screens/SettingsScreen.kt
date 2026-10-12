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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.LineEnding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  onSaveSettings: (AppSettings) -> Unit,
  onTestVoiceAlert: (Int) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var motorOnCmd by remember(settings) { mutableStateOf(settings.motorOnCmd) }
  var motorOffCmd by remember(settings) { mutableStateOf(settings.motorOffCmd) }
  var autoCutoffEnabled by remember(settings) { mutableStateOf(settings.autoCutoffEnabled) }
  var cutoffThreshold by remember(settings) { mutableFloatStateOf(settings.cutoffThresholdPercent.toFloat()) }
  var lowLevelAlert by remember(settings) { mutableFloatStateOf(settings.lowLevelAlertPercent.toFloat()) }
  var tankCapacity by remember(settings) { mutableStateOf(settings.tankCapacityLiters.toInt().toString()) }
  var tankHeight by remember(settings) { mutableStateOf(settings.tankHeightCm.toInt().toString()) }
  var filterHc05Only by remember(settings) { mutableStateOf(settings.filterHc05Only) }
  var hapticAlerts by remember(settings) { mutableStateOf(settings.hapticAlerts) }
  var voiceAlertsEnabled by remember(settings) { mutableStateOf(settings.voiceAlertsEnabled) }
  var pushNotificationsEnabled by remember(settings) { mutableStateOf(settings.pushNotificationsEnabled) }
  var voiceMilestones25 by remember(settings) { mutableStateOf(settings.voiceMilestones25) }
  var voiceMilestones50 by remember(settings) { mutableStateOf(settings.voiceMilestones50) }
  var voiceMilestones75 by remember(settings) { mutableStateOf(settings.voiceMilestones75) }
  var voiceMilestones95 by remember(settings) { mutableStateOf(settings.voiceMilestones95) }
  var voiceTargetReached by remember(settings) { mutableStateOf(settings.voiceTargetReached) }
  var cloudGatewayEnabled by remember(settings) { mutableStateOf(settings.cloudGatewayEnabled) }
  var cloudGatewayUrl by remember(settings) { mutableStateOf(settings.cloudGatewayUrl) }
  var selectedLineEnding by remember(settings) { mutableStateOf(settings.lineEnding) }
  var lineEndingExpanded by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp)
      .testTag("settings_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.Tune,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "System Configuration",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Commands, safeguards & sensor calibration",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Card 1: Motor Command Protocol
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Arduino Motor Commands", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = motorOnCmd,
            onValueChange = { motorOnCmd = it },
            label = { Text("Motor ON Command") },
            modifier = Modifier.weight(1f).testTag("motor_on_cmd_field"),
            singleLine = true
          )
          OutlinedTextField(
            value = motorOffCmd,
            onValueChange = { motorOffCmd = it },
            label = { Text("Motor OFF Command") },
            modifier = Modifier.weight(1f).testTag("motor_off_cmd_field"),
            singleLine = true
          )
        }

        // Line Ending Delimiter
        ExposedDropdownMenuBox(
          expanded = lineEndingExpanded,
          onExpandedChange = { lineEndingExpanded = !lineEndingExpanded }
        ) {
          OutlinedTextField(
            value = selectedLineEnding.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Line Delimiter (UART Ending)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lineEndingExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = lineEndingExpanded,
            onDismissRequest = { lineEndingExpanded = false }
          ) {
            LineEnding.entries.forEach { ending ->
              DropdownMenuItem(
                text = { Text(ending.label) },
                onClick = {
                  selectedLineEnding = ending
                  lineEndingExpanded = false
                }
              )
            }
          }
        }
      }
    }

    // Card 2: Automated Safeguards
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Safety Cutoffs & Safeguards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // Auto Cutoff Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "High Level Auto-Cutoff", fontWeight = FontWeight.SemiBold)
            Text(
              text = "Automatically stops pump motor when full to avoid water overflow",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = autoCutoffEnabled,
            onCheckedChange = { autoCutoffEnabled = it }
          )
        }

        if (autoCutoffEnabled) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = "Cutoff Trigger Level", style = MaterialTheme.typography.bodySmall)
              Text(text = "${cutoffThreshold.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
              value = cutoffThreshold,
              onValueChange = { cutoffThreshold = it },
              valueRange = 70f..100f,
              steps = 5
            )
          }
        }

        // Low Level Alert Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Low Water Alert Threshold", style = MaterialTheme.typography.bodySmall)
            Text(text = "${lowLevelAlert.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = lowLevelAlert,
            onValueChange = { lowLevelAlert = it },
            valueRange = 5f..40f,
            steps = 6
          )
        }
      }
    }

    // Card 3: Tank Physical Specifications
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.WaterDamage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Tank Dimensions & Sensor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = tankCapacity,
            onValueChange = { tankCapacity = it },
            label = { Text("Capacity (Liters)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
          )
          OutlinedTextField(
            value = tankHeight,
            onValueChange = { tankHeight = it },
            label = { Text("Tank Height (cm)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
          )
        }
      }
    }

    // Card 4: Hardware & App Toggles
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Strict HC-05 Filter", fontWeight = FontWeight.SemiBold)
            Text(text = "Exclusively list HC-05 Bluetooth devices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Switch(checked = filterHc05Only, onCheckedChange = { filterHc05Only = it })
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Haptic Vibration Alerts", fontWeight = FontWeight.SemiBold)
            Text(text = "Tactile feedback for commands and alarms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Switch(checked = hapticAlerts, onCheckedChange = { hapticAlerts = it })
        }
      }
    }

    // Card 5: Cloud Gateway & Public Web Monitor Link
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Cloud Gateway & Public Monitor Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Broadcast Live Telemetry", fontWeight = FontWeight.SemiBold)
            Text(
              text = "Sync HC-05 readings to Firestore cloud for viewers",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(checked = cloudGatewayEnabled, onCheckedChange = { cloudGatewayEnabled = it })
        }

        OutlinedTextField(
          value = cloudGatewayUrl,
          onValueChange = { cloudGatewayUrl = it },
          label = { Text("Public Web Viewer URL") },
          supportingText = { Text("GitHub Pages link (docs/ folder) or Firebase Hosting URL") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_gateway_url_field"),
          singleLine = true
        )
      }
    }

    // Card 6: Gujarati Voice & Notification Alerts
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "ગુજરાતી અવાજ & એલર્ટ્સ (Voice & Alerts)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Human Gujarati voice & heads-up background alerts",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Master Voice Alerts Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Gujarati Voice Alerts (ગુજરાતી અવાજ)", fontWeight = FontWeight.SemiBold)
            Text(
              text = "Speaks milestone status (e.g. પાણીની ટાંકી 50% ભરાઈ ગઈ છે)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = voiceAlertsEnabled,
            onCheckedChange = { voiceAlertsEnabled = it },
            modifier = Modifier.testTag("switch_voice_alerts")
          )
        }

        // Master Push Notifications Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Push Notifications (નોટિફિકેશન)", fontWeight = FontWeight.SemiBold)
            Text(
              text = "Heads-up alert banner on lock screen & background",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = pushNotificationsEnabled,
            onCheckedChange = { pushNotificationsEnabled = it },
            modifier = Modifier.testTag("switch_push_notifications")
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text(
          text = "સક્રિય માઈલસ્ટોન (Active Milestones):",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )

        // 25% Milestone
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "💧 25% Water Milestone", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = voiceMilestones25, onCheckedChange = { voiceMilestones25 = it })
        }

        // 50% Milestone
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "💧 50% Water Milestone", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = voiceMilestones50, onCheckedChange = { voiceMilestones50 = it })
        }

        // 75% Milestone
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "💧 75% Water Milestone", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = voiceMilestones75, onCheckedChange = { voiceMilestones75 = it })
        }

        // 95% Safety Milestone
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "🚨 95% Critical Overflow Warning", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
          Switch(checked = voiceMilestones95, onCheckedChange = { voiceMilestones95 = it })
        }

        // Target Reached Milestone
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "🎯 Target Reached (Motor Cutoff)", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = voiceTargetReached, onCheckedChange = { voiceTargetReached = it })
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Voice Test Buttons
        Text(
          text = "ટેસ્ટ વોઈસ અવાજ (Test Human Audio):",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { onTestVoiceAlert(50) },
            modifier = Modifier.weight(1f).testTag("test_voice_50_button"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ટેસ્ટ 50%", style = MaterialTheme.typography.bodySmall)
          }

          OutlinedButton(
            onClick = { onTestVoiceAlert(95) },
            modifier = Modifier.weight(1f).testTag("test_voice_95_button"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ટેસ્ટ 95%", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }

    // Save Settings Button
    Button(
      onClick = {
        val updated = settings.copy(
          motorOnCmd = motorOnCmd.ifBlank { "MOTOR_ON" },
          motorOffCmd = motorOffCmd.ifBlank { "MOTOR_OFF" },
          autoCutoffEnabled = autoCutoffEnabled,
          cutoffThresholdPercent = cutoffThreshold.toInt(),
          lowLevelAlertPercent = lowLevelAlert.toInt(),
          tankCapacityLiters = tankCapacity.toFloatOrNull() ?: 1000f,
          tankHeightCm = tankHeight.toFloatOrNull() ?: 100f,
          lineEnding = selectedLineEnding,
          filterHc05Only = filterHc05Only,
          hapticAlerts = hapticAlerts,
          voiceAlertsEnabled = voiceAlertsEnabled,
          pushNotificationsEnabled = pushNotificationsEnabled,
          voiceMilestones25 = voiceMilestones25,
          voiceMilestones50 = voiceMilestones50,
          voiceMilestones75 = voiceMilestones75,
          voiceMilestones95 = voiceMilestones95,
          voiceTargetReached = voiceTargetReached,
          cloudGatewayEnabled = cloudGatewayEnabled,
          cloudGatewayUrl = cloudGatewayUrl.ifBlank { "https://akshaypatel0.github.io/HydroTank/" }
        )
        onSaveSettings(updated)
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("save_settings_button"),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(imageVector = Icons.Default.Save, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "Save Configuration", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
