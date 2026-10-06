package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogEntry
import com.example.data.model.LogType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PumpActiveGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WaterWaveCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalScreen(
  logs: List<LogEntry>,
  onSendCommand: (String) -> Unit,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Auto-scroll to bottom on new log
  LaunchedEffect(logs.size) {
    if (logs.isNotEmpty()) {
      listState.animateScrollToItem(logs.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .testTag("terminal_screen")
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Terminal,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "HC-05 Serial Monitor",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }

      IconButton(
        onClick = onClearLogs,
        modifier = Modifier.testTag("clear_terminal_logs_button")
      ) {
        Icon(
          imageVector = Icons.Default.DeleteSweep,
          contentDescription = "Clear Logs",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Quick Command Chips
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val presets = listOf("MOTOR_ON", "MOTOR_OFF", "STATUS", "PING", "TARGET:80.0", "TARGET:90.0", "CAL_EMPTY", "CAL_FULL")
      items(presets) { preset ->
        AssistChip(
          onClick = { onSendCommand(preset) },
          label = { Text(text = preset, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Log Console Window (Terminal aesthetic)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F14)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(logs, key = { it.id }) { log ->
          LogItemRow(log = log)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Command Input Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = { Text("Enter command (e.g. 1 or 0)", fontSize = 13.sp) },
        modifier = Modifier
          .weight(1f)
          .testTag("terminal_input_field"),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = {
          if (inputText.isNotBlank()) {
            onSendCommand(inputText.trim())
            inputText = ""
          }
        }),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(
        onClick = {
          if (inputText.isNotBlank()) {
            onSendCommand(inputText.trim())
            inputText = ""
          }
        },
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary)
          .testTag("terminal_send_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send",
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Composable
private fun LogItemRow(log: LogEntry) {
  val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
  val timeStr = timeFormat.format(Date(log.timestamp))

  val textColor = when (log.type) {
    LogType.TX -> PumpActiveGreen
    LogType.RX -> WaterWaveCyan
    LogType.WARNING -> WarningAmber
    LogType.ERROR -> DangerRed
    LogType.SYSTEM -> Color(0xFFB0BEC5)
  }

  val prefix = when (log.type) {
    LogType.TX -> ">> TX:"
    LogType.RX -> "<< RX:"
    LogType.WARNING -> "WARN:"
    LogType.ERROR -> "ERR :"
    LogType.SYSTEM -> "SYS :"
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "[$timeStr]",
      color = Color(0xFF546E7A),
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(90.dp)
    )

    Spacer(modifier = Modifier.width(4.dp))

    Text(
      text = prefix,
      color = textColor,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(55.dp)
    )

    Spacer(modifier = Modifier.width(4.dp))

    Text(
      text = log.text,
      color = textColor,
      fontSize = 12.sp,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.weight(1f)
    )
  }
}
