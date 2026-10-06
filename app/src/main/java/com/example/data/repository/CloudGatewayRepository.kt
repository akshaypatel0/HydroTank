package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.TankState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
  val auth = FirebaseAuth.getInstance()
  val currentUser = auth.currentUser

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", exception.message ?: exception.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", jsonString, exception)
  return jsonString
}

sealed interface GatewaySyncStatus {
  data object Idle : GatewaySyncStatus
  data object Syncing : GatewaySyncStatus
  data class Synced(val lastSyncTime: Long) : GatewaySyncStatus
  data class Error(val message: String) : GatewaySyncStatus
}

class CloudGatewayRepository(
  private val firestore: FirebaseFirestore
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private val _syncStatus = MutableStateFlow<GatewaySyncStatus>(GatewaySyncStatus.Idle)
  val syncStatus: StateFlow<GatewaySyncStatus> = _syncStatus.asStateFlow()

  val currentUserId: String?
    get() = auth.currentUser?.uid

  val currentUserEmail: String?
    get() = auth.currentUser?.email

  val isUserSignedIn: Boolean
    get() = auth.currentUser != null

  fun signOut() {
    auth.signOut()
    _syncStatus.value = GatewaySyncStatus.Idle
  }

  /**
   * Pushes the latest tank reading to Firestore at /tanks/main_tank.
   * Anyone with the public web link can view this document in real-time.
   */
  suspend fun syncTankTelemetry(tankState: TankState): Result<Unit> = withContext(Dispatchers.IO) {
    val user = auth.currentUser
    if (user == null) {
      val errorMsg = "Gateway device must be signed in with Google to push cloud telemetry."
      _syncStatus.value = GatewaySyncStatus.Error(errorMsg)
      return@withContext Result.failure(IllegalStateException(errorMsg))
    }

    if (!tankState.hasValidReading) {
      // Don't push null / offline placeholders
      return@withContext Result.success(Unit)
    }

    _syncStatus.value = GatewaySyncStatus.Syncing

    val docRef = firestore.collection("tanks").document("main_tank")
    val payload = hashMapOf<String, Any>(
      "levelPercent" to (tankState.levelPercent ?: 0f),
      "isMotorOn" to tankState.isMotorOn,
      "targetLevel" to tankState.targetLevel,
      "statusMessage" to tankState.statusMessage,
      "isDeviceConnected" to true,
      "gatewayUserId" to user.uid,
      "lastUpdated" to FieldValue.serverTimestamp()
    )

    tankState.distanceCM?.let {
      payload["distanceCM"] = it
    }

    try {
      docRef.set(payload).await()
      val now = System.currentTimeMillis()
      _syncStatus.value = GatewaySyncStatus.Synced(now)
      Result.success(Unit)
    } catch (e: Exception) {
      val errJson = handleFirestoreError(e, OperationType.WRITE, docRef.path)
      _syncStatus.value = GatewaySyncStatus.Error(e.message ?: "Failed to sync tank")
      Result.failure(e)
    }
  }

  /**
   * Sets the public cloud document status to disconnected / offline
   * when Bluetooth disconnects, ensuring the web link never displays fake or stale readings.
   */
  suspend fun syncDeviceDisconnected(): Result<Unit> = withContext(Dispatchers.IO) {
    val user = auth.currentUser ?: return@withContext Result.success(Unit)
    val docRef = firestore.collection("tanks").document("main_tank")
    val payload = hashMapOf<String, Any>(
      "levelPercent" to 0f,
      "isMotorOn" to false,
      "targetLevel" to 0f,
      "statusMessage" to "HC-05 Disconnected • Hardware Offline",
      "isDeviceConnected" to false,
      "gatewayUserId" to user.uid,
      "lastUpdated" to FieldValue.serverTimestamp()
    )

    try {
      docRef.set(payload).await()
      _syncStatus.value = GatewaySyncStatus.Idle
      Result.success(Unit)
    } catch (e: Exception) {
      val errJson = handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }
}
