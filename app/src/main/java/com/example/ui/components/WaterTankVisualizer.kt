package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TankState
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PumpActiveGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WaterWaveCrest
import com.example.ui.theme.WaterWaveCyan
import com.example.ui.theme.WaterWaveDeep
import kotlin.math.sin

@Composable
fun WaterTankVisualizer(
  tankState: TankState,
  modifier: Modifier = Modifier
) {
  val level = tankState.levelPercent?.coerceIn(0f, 100f) ?: 0f
  val hasData = tankState.hasValidReading

  val animatedLevel by animateFloatAsState(
    targetValue = level,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "waterLevelAnimation"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (tankState.isMotorOn) 1800 else 3200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wavePhase"
  )

  val secondaryWavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (tankState.isMotorOn) 2400 else 4200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "secondaryWavePhase"
  )

  val statusColor = when {
    !hasData -> MaterialTheme.colorScheme.onSurfaceVariant
    level >= tankState.safetyLevel -> DangerRed
    level >= tankState.targetLevel -> WarningAmber
    tankState.isMotorOn -> PumpActiveGreen
    else -> WaterWaveCyan
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("water_tank_visualizer_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Tank Header / Status row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (hasData) Icons.Default.WaterDrop else Icons.Default.SensorsOff,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "HydroSense Reservoir",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        }

        // Live badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = statusColor.copy(alpha = 0.15f),
          modifier = Modifier.wrapContentWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when {
                !hasData -> "NO CONNECTION"
                level >= tankState.safetyLevel -> "SAFETY 95%"
                level >= tankState.targetLevel -> "TARGET"
                tankState.isMotorOn -> "PUMP INFLOW"
                else -> "ONLINE"
              },
              style = MaterialTheme.typography.labelSmall,
              color = statusColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Responsive Middle Section: Tank Graphic on Left, Telemetry Cards on Right
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(250.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Measurement scale ticks on left
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 10.dp),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.End
        ) {
          listOf("100%", "95%", "80%", "50%", "20%", "0%").forEach { tick ->
            Text(
              text = tick,
              style = MaterialTheme.typography.labelSmall,
              color = if (tick == "95%") DangerRed.copy(alpha = 0.9f)
              else if (tick == "80%") WarningAmber.copy(alpha = 0.9f)
              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              fontSize = 9.sp,
              fontWeight = if (tick == "95%" || tick == "80%") FontWeight.Bold else FontWeight.Normal
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Physical Tank Graphic
        Box(
          modifier = Modifier
            .weight(1.1f)
            .fillMaxHeight()
            .shadow(6.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(Color(0xFF0A192F))
            .border(
              width = 2.dp,
              brush = Brush.verticalGradient(
                colors = listOf(
                  Color(0xFF64B5F6),
                  Color(0xFF1E88E5),
                  Color(0xFF0D47A1)
                )
              ),
              shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          // Dynamic Wave Canvas
          Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (hasData && animatedLevel > 0.5f) {
              val waterLevelFraction = animatedLevel / 100f
              val waterHeight = height * waterLevelFraction
              val waterTopY = height - waterHeight

              val waveAmplitude = if (tankState.isMotorOn) 8f else 5f

              // 1. Secondary wave (background)
              val pathBack = Path().apply {
                moveTo(0f, height)
                lineTo(0f, waterTopY)
                val steps = 25
                val dx = width / steps
                for (i in 0..steps) {
                  val x = i * dx
                  val y = waterTopY + waveAmplitude * sin(secondaryWavePhase + (i * 0.28f))
                  lineTo(x, y)
                }
                lineTo(width, height)
                close()
              }
              drawPath(
                path = pathBack,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    WaterWaveDeep.copy(alpha = 0.65f),
                    WaterWaveDeep.copy(alpha = 0.95f)
                  ),
                  startY = waterTopY,
                  endY = height
                )
              )

              // 2. Primary wave (foreground)
              val pathFront = Path().apply {
                moveTo(0f, height)
                lineTo(0f, waterTopY)
                val steps = 25
                val dx = width / steps
                for (i in 0..steps) {
                  val x = i * dx
                  val y = waterTopY + waveAmplitude * sin(wavePhase + (i * 0.32f))
                  lineTo(x, y)
                }
                lineTo(width, height)
                close()
              }
              drawPath(
                path = pathFront,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    WaterWaveCrest,
                    WaterWaveCyan,
                    WaterWaveDeep
                  ),
                  startY = waterTopY - waveAmplitude,
                  endY = height
                )
              )
            }

            // Target Level Line (Dashed Line on Tank)
            val targetY = height * (1f - (tankState.targetLevel / 100f))
            drawLine(
              color = Color(0xFFFFD54F),
              start = Offset(0f, targetY),
              end = Offset(width, targetY),
              strokeWidth = 2.5f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            // Safety 95% Cutoff Line (Red line)
            val safetyY = height * (1f - (tankState.safetyLevel / 100f))
            drawLine(
              color = Color(0xFFFF5252).copy(alpha = 0.7f),
              start = Offset(0f, safetyY),
              end = Offset(width, safetyY),
              strokeWidth = 2f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Glass Reflection Overlay on Left
            drawRoundRect(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  Color.White.copy(alpha = 0.2f),
                  Color.White.copy(alpha = 0.03f),
                  Color.Transparent
                )
              ),
              topLeft = Offset(10f, 8f),
              size = Size(16f, height - 16f),
              cornerRadius = CornerRadius(8f, 8f)
            )
          }

          // Center Level Stats Overlay
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.68f),
            modifier = Modifier.padding(6.dp)
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              if (hasData) {
                Text(
                  text = "${String.format(java.util.Locale.US, "%.1f", animatedLevel)}%",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White
                )
                Text(
                  text = "Target: ${tankState.targetLevel.toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFFD54F),
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp
                )
                Text(
                  text = "${tankState.volumeLiters?.toInt() ?: 0} / ${tankState.tankCapacityLiters.toInt()} L",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFE0F7FA),
                  fontSize = 9.sp
                )
              } else {
                Text(
                  text = "-- %",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF90A4AE)
                )
                Text(
                  text = "No Connection",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFB0BEC5),
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 10.sp
                )
                Text(
                  text = "-- / ${tankState.tankCapacityLiters.toInt()} L",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF78909C),
                  fontSize = 9.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right side live Arduino telemetry with flexible weight
        Column(
          modifier = Modifier
            .weight(1.2f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.SpaceEvenly,
          horizontalAlignment = Alignment.Start
        ) {
          IndicatorItem(
            label = "Ultrasonic Distance",
            value = if (tankState.distanceCM != null)
              "${String.format(java.util.Locale.US, "%.2f", tankState.distanceCM)} cm"
            else
              "-- cm",
            valueColor = if (tankState.distanceCM != null) WaterWaveCyan else MaterialTheme.colorScheme.onSurfaceVariant
          )

          IndicatorItem(
            label = "Target Cutoff",
            value = "${tankState.targetLevel.toInt()}%",
            valueColor = Color(0xFFFFB300)
          )

          IndicatorItem(
            label = "Safety Hard Limit",
            value = "${tankState.safetyLevel.toInt()}%",
            valueColor = DangerRed
          )

          IndicatorItem(
            label = "Motor (Relay D7)",
            value = when {
              !hasData -> "STANDBY"
              tankState.isMotorOn -> "RUNNING"
              else -> "STOPPED"
            },
            valueColor = if (tankState.isMotorOn) PumpActiveGreen else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun IndicatorItem(
  label: String,
  value: String,
  valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
      fontSize = 11.sp,
      maxLines = 1
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = valueColor,
      maxLines = 1
    )
  }
}
