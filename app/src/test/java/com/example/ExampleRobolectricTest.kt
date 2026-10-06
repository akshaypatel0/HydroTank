package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.BluetoothController
import com.example.data.model.TankState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HydroTank", appName)
  }

  @Test
  fun `hc05 device name filter detection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val controller = BluetoothController(context)

    assertTrue(controller.isHc05DeviceName("HC-05"))
    assertTrue(controller.isHc05DeviceName("hc-05"))
    assertTrue(controller.isHc05DeviceName("HC05"))
    assertTrue(controller.isHc05DeviceName("BT-05"))
    assertTrue(controller.isHc05DeviceName("HC-06"))
    assertFalse(controller.isHc05DeviceName("Galaxy Buds"))
    assertFalse(controller.isHc05DeviceName("Sony WH-1000XM4"))
    assertFalse(controller.isHc05DeviceName(null))
  }

  @Test
  fun `tank state volume calculation`() {
    val tank = TankState(
      levelPercent = 75f,
      tankCapacityLiters = 1000f,
      targetLevel = 80f,
      safetyLevel = 95f
    )
    assertTrue(tank.hasValidReading)
    assertEquals(750f, tank.volumeLiters!!, 0.1f)
    assertFalse(tank.isCriticalLow)
    assertFalse(tank.isFullOrOverflow)

    val lowTank = TankState(levelPercent = 10f, tankCapacityLiters = 1000f)
    assertTrue(lowTank.isCriticalLow)

    val fullTank = TankState(levelPercent = 98f, tankCapacityLiters = 1000f)
    assertTrue(fullTank.isFullOrOverflow)
  }

  @Test
  fun `tank state disconnected has no reading`() {
    val disconnectedTank = TankState()
    assertFalse(disconnectedTank.hasValidReading)
    assertNull(disconnectedTank.levelPercent)
    assertNull(disconnectedTank.distanceCM)
    assertNull(disconnectedTank.volumeLiters)
  }
}
