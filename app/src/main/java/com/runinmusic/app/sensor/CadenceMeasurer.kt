package com.runinmusic.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.runinmusic.app.core.cadence.CadenceMapper

class CadenceMeasurer(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepDetector = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val handler = Handler(Looper.getMainLooper())

    private var stepCount = 0
    private var onResult: ((CadenceResult) -> Unit)? = null
    private var finishRunnable: Runnable? = null

    val isSupported: Boolean = stepDetector != null

    fun startMeasurement(
        seconds: Int = CadenceMapper.DEFAULT_MEASUREMENT_SECONDS,
        onResult: (CadenceResult) -> Unit,
    ): Boolean {
        require(seconds in CadenceMapper.SUPPORTED_MEASUREMENT_SECONDS) { "unsupported measurement duration" }
        val sensor = stepDetector ?: return false
        stop()
        stepCount = 0
        this.onResult = onResult
        val registered = sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST) == true
        if (!registered) return false

        finishRunnable = Runnable {
            val steps = stepCount
            stop()
            onResult(
                CadenceResult(
                    steps = steps,
                    seconds = seconds,
                    spm = CadenceMapper.stepsToSpm(steps, seconds),
                    source = CadenceSource.StepDetector,
                ),
            )
        }.also { handler.postDelayed(it, seconds * 1000L) }
        return true
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        finishRunnable?.let { handler.removeCallbacks(it) }
        finishRunnable = null
        onResult = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCount += 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

data class CadenceResult(
    val steps: Int,
    val seconds: Int,
    val spm: Double,
    val source: CadenceSource,
)

enum class CadenceSource {
    StepDetector,
    ManualTap,
}
