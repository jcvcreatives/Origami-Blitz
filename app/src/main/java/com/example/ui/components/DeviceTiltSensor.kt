package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

data class DeviceTilt(
    val x: Float = 0f, // Normalized -1f (left) to +1f (right)
    val y: Float = 0f  // Normalized -1f (top) to +1f (bottom)
)

/**
 * Listens to hardware Gravity / Accelerometer sensors to compute real-time device tilt
 * for 3D paper parallax and dynamic global lighting overlays.
 */
@Composable
fun rememberDeviceTilt(
    pointerTiltOffset: Offset = Offset.Zero
): DeviceTilt {
    val context = LocalContext.current
    var rawSensorX by remember { mutableFloatStateOf(0f) }
    var rawSensorY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size >= 2) {
                    // Android sensor X is inverted relative to screen right tilt (-9.8..9.8 m/s^2)
                    val normX = (-values[0] / 7.5f).coerceIn(-1f, 1f)
                    // Subtract natural ~4.5 m/s^2 handheld resting angle on Y axis
                    val normY = ((values[1] - 4.5f) / 7.5f).coerceIn(-1f, 1f)
                    // Low-pass filter for silky-smooth motion
                    rawSensorX = rawSensorX * 0.8f + normX * 0.2f
                    rawSensorY = rawSensorY * 0.8f + normY * 0.2f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensor != null) {
            try {
                sensorManager?.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
            } catch (_: Throwable) {
                // Safely ignore in headless test environments
            }
        }

        onDispose {
            try {
                sensorManager?.unregisterListener(listener)
            } catch (_: Throwable) {
            }
        }
    }

    val targetX = (rawSensorX + pointerTiltOffset.x).coerceIn(-1f, 1f)
    val targetY = (rawSensorY + pointerTiltOffset.y).coerceIn(-1f, 1f)

    val smoothX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tilt_x"
    )
    val smoothY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tilt_y"
    )

    return DeviceTilt(x = smoothX, y = smoothY)
}
