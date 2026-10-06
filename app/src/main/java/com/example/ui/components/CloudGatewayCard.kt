package com.example.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.repository.GatewaySyncStatus
import com.example.ui.theme.PumpActiveGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WaterWaveCyan
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun CloudGatewayCard(
  settings: AppSettings,
  syncStatus: GatewaySyncStatus,
  isUserSignedIn: Boolean,
  currentUserEmail: String?,
  onManualSync: () -> Unit,
  onSignInWithGoogleToken: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()
  var isSigningIn by remember { mutableStateOf(false) }
  var showGuide by remember { mutableStateOf(false) }

  val shareUrl = settings.cloudGatewayUrl

  fun copyToClipboard() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("HydroSense Public Link", shareUrl)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Public link copied to clipboard!", Toast.LENGTH_SHORT).show()
  }

  fun shareViaChooser() {
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TITLE, "HydroSense Live Water Monitor")
      putExtra(
        Intent.EXTRA_TEXT,
        "💧 HydroSense: View our live water tank level, pump status, and telemetry here:\n$shareUrl"
      )
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Public Water Level Link")
    context.startActivity(shareIntent)
  }

  fun openInBrowser() {
    try {
      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl))
      context.startActivity(browserIntent)
    } catch (e: Exception) {
      Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun startGoogleSignIn() {
    if (activity == null) return
    isSigningIn = true
    scope.launch {
      try {
        val credentialManager = CredentialManager.create(activity)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
          serverClientId = activity.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
          .addCredentialOption(signInWithGoogleOption)
          .build()

        val result = credentialManager.getCredential(request = request, context = activity)
        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        onSignInWithGoogleToken(googleIdTokenCredential.idToken)
      } catch (e: GetCredentialCancellationException) {
        // User dismissed account picker
      } catch (e: Exception) {
        Toast.makeText(activity, "Sign-in error: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
      } finally {
        isSigningIn = false
      }
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("cloud_gateway_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header: Title & Broadcast Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Public View-Only Link",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Live web monitor for family & guests",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Live Cloud Status Pill
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = when {
            !isUserSignedIn -> MaterialTheme.colorScheme.surfaceVariant
            syncStatus is GatewaySyncStatus.Syncing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            syncStatus is GatewaySyncStatus.Synced -> PumpActiveGreen.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                  when {
                    !isUserSignedIn -> WarningAmber
                    syncStatus is GatewaySyncStatus.Syncing -> MaterialTheme.colorScheme.primary
                    syncStatus is GatewaySyncStatus.Synced -> PumpActiveGreen
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  }
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when {
                !isUserSignedIn -> "Sign-In Needed"
                syncStatus is GatewaySyncStatus.Syncing -> "Broadcasting"
                syncStatus is GatewaySyncStatus.Synced -> "Cloud Synced"
                syncStatus is GatewaySyncStatus.Error -> "Sync Error"
                else -> "Cloud Ready"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = when {
                !isUserSignedIn -> WarningAmber
                syncStatus is GatewaySyncStatus.Synced -> PumpActiveGreen
                else -> MaterialTheme.colorScheme.onSurface
              }
            )
          }
        }
      }

      // Feature explanation & view-only assurance
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = WaterWaveCyan,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Anyone with this link can watch live water levels in their web browser. Motor controls and settings are strictly read-only for viewers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
        }
      }

      // Public URL Display & Action Bar
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
          )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "PUBLIC WEB ADDRESS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = shareUrl,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Primary Share Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // 1. Share via Chooser (WhatsApp, Telegram, SMS, etc.)
        Button(
          onClick = { shareViaChooser() },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("share_public_link_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Share Link", fontWeight = FontWeight.Bold)
        }

        // 2. Copy Link
        FilledTonalButton(
          onClick = { copyToClipboard() },
          modifier = Modifier
            .height(44.dp)
            .testTag("copy_public_link_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Copy")
        }

        // 3. Open in Browser
        OutlinedButton(
          onClick = { openInBrowser() },
          modifier = Modifier
            .height(44.dp)
            .testTag("open_web_monitor_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.OpenInBrowser,
            contentDescription = "Open Web View",
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Gateway Authentication Section
      if (!isUserSignedIn) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Gateway Sign-In Required",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = "To publish your Arduino Bluetooth telemetry to the cloud so viewers can see it, sign in with Google on this device as the gateway owner.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
              onClick = { startGoogleSignIn() },
              enabled = !isSigningIn,
              modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("google_signin_button"),
              shape = RoundedCornerShape(10.dp)
            ) {
              if (isSigningIn) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Signing in...")
              } else {
                Icon(
                  imageVector = Icons.Default.Login,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sign in with Google to Broadcast", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        // Authenticated Gateway Info Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "Gateway Owner",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = currentUserEmail ?: "Signed In",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onManualSync,
              modifier = Modifier.testTag("manual_sync_button")
            ) {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sync now to Cloud",
                tint = MaterialTheme.colorScheme.primary
              )
            }
            TextButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("signout_button")
            ) {
              Text(text = "Sign Out", fontSize = 12.sp)
            }
          }
        }
      }

      // Expandable "How it works" guide
      Column {
        TextButton(
          onClick = { showGuide = !showGuide },
          modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
          Icon(
            imageVector = if (showGuide) Icons.Default.Info else Icons.Default.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (showGuide) "Hide Hosting Instructions" else "How to host or customize this link?",
            style = MaterialTheme.typography.labelMedium
          )
        }

        AnimatedVisibility(visible = showGuide) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Hosting & Sharing Setup:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "1. GitHub Pages (Free):\nIn your GitHub repository, navigate to Settings > Pages, choose Branch 'main' and Folder '/docs', then click Save.\n\n2. Real-Time Telemetry:\nWhen this phone is connected to your HC-05 module and signed in, it automatically pushes tank level changes to Firestore. Viewers see fluid wave animations and status update live without refreshing!\n\n3. Customize URL:\nYou can update your public link address anytime in the 'Config' tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
              )
            }
          }
        }
      }
    }
  }
}
