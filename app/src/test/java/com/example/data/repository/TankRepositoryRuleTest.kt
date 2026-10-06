package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TankState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TankRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun authenticatedGatewayUserCanSyncTankTelemetry() = runTest {
    val uid = signInTestUser("gateway_owner@example.com")
    val repository = CloudGatewayRepository(firestore)

    val tankState = TankState(
      levelPercent = 72.5f,
      distanceCM = 6.2f,
      isMotorOn = true,
      targetLevel = 80.0f,
      statusMessage = "Motor Running"
    )

    val result = repository.syncTankTelemetry(tankState)
    assertTrue("Telemetry write should succeed for authenticated gateway", result.isSuccess)

    // Verify document was written to Firestore
    val doc = firestore.collection("tanks").document("main_tank").get().await()
    assertTrue(doc.exists())
    assertEquals(uid, doc.getString("gatewayUserId"))
    assertEquals(72.5, doc.getDouble("levelPercent") ?: 0.0, 0.1)
    assertEquals(true, doc.getBoolean("isMotorOn"))
  }

  @Test
  fun unauthenticatedUserCannotSyncTankTelemetry() = runTest {
    auth.signOut()
    val repository = CloudGatewayRepository(firestore)

    val tankState = TankState(
      levelPercent = 50.0f,
      distanceCM = 9.0f,
      isMotorOn = false,
      targetLevel = 80.0f
    )

    val result = repository.syncTankTelemetry(tankState)
    assertTrue("Unauthenticated gateway write must fail", result.isFailure)
  }

  @Test
  fun publicVisitorCanReadTankStatusWithoutAuth() = runTest {
    // Authenticate and write initial tank state
    val uid = signInTestUser("gateway_owner@example.com")
    val repository = CloudGatewayRepository(firestore)
    val tankState = TankState(
      levelPercent = 88.0f,
      isMotorOn = false,
      targetLevel = 80.0f
    )
    repository.syncTankTelemetry(tankState)

    // Sign out to act as public visitor with link
    auth.signOut()

    // Public visitor reads document
    val doc = firestore.collection("tanks").document("main_tank").get().await()
    assertTrue(doc.exists())
    assertEquals(88.0, doc.getDouble("levelPercent") ?: 0.0, 0.1)
  }
}
