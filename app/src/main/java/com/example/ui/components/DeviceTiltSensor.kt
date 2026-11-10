package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.roundToInt

data class DeviceTilt(
    val x: Float = 0f, // Normalized -1f (left) to +1f (right)
    val y: Float = 0f  // Normalized -1f (top) to +1f (bottom)
)

/**
 * Listens to hardware Gravity / Accelerometer sensors with low-power UI rate,
 * deadband quantization (zero micro-jitter recompositions when stationary), and
 * lifecycle-aware pause/resume so the device never lags or overheats.
 */
@Composable
fun rememberDeviceTilt(
    pointerTiltOffset: Offset = Offset.Zero
): DeviceTilt {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var rawSensorX by remember { mutableFloatStateOf(0f) }
    var rawSensorY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var filteredX = 0f
        var filteredY = 0f
        var lastEmitMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size >= 2) {
                    val nowMs = SystemClock.uptimeMillis()
                    if (nowMs - lastEmitMs < 48L) return // Max ~20Hz state check to save CPU/battery

                    val normX = (-values[0] / 7.5f).coerceIn(-1f, 1f)
                    val normY = ((values[1] - 4.5f) / 7.5f).coerceIn(-1f, 1f)
                    filteredX = filteredX * 0.78f + normX * 0.22f
                    filteredY = filteredY * 0.78f + normY * 0.22f

                    // Quantize to 0.025f steps and apply deadband so resting phones trigger 0 recompositions
                    val quantX = (filteredX * 40f).roundToInt() / 40f
                    val quantY = (filteredY * 40f).roundToInt() / 40f
                    if (abs(quantX - rawSensorX) >= 0.03f || abs(quantY - rawSensorY) >= 0.03f) {
                        lastEmitMs = nowMs
                        rawSensorX = quantX
                        rawSensorY = quantY
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun registerSensorSafely() {
            if (sensor != null) {
                try {
                    sensorManager?.registerListener(
                        listener,
                        sensor,
                        SensorManager.SENSOR_DELAY_UI
                    )
                } catch (_: Throwable) {
                    // Safely ignore in headless test environments
                }
            }
        }

        fun unregisterSensorSafely() {
            try {
                sensorManager?.unregisterListener(listener)
            } catch (_: Throwable) {
            }
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registerSensorSafely()
                Lifecycle.Event.ON_PAUSE -> unregisterSensorSafely()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registerSensorSafely()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            unregisterSensorSafely()
        }
    }

    val targetX = ((rawSensorX + pointerTiltOffset.x).coerceIn(-1f, 1f) * 40f).roundToInt() / 40f
    val targetY = ((rawSensorY + pointerTiltOffset.y).coerceIn(-1f, 1f) * 40f).roundToInt() / 40f

    val smoothX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.008f
        ),
        label = "tilt_x"
    )
    val smoothY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.008f
        ),
        label = "tilt_y"
    )

    return DeviceTilt(x = smoothX, y = smoothY)
}
