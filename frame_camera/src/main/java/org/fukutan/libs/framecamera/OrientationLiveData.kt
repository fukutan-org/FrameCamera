package org.fukutan.libs.framecamera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData


// 向き情報を格納するクラス
data class Orientation(
    val azimuth: Float,
    val pitch: Float,
    val roll: Float
)

class OrientationLiveData(
    context: Context,
    private val sensorDelay: Int = SensorManager.SENSOR_DELAY_UI)
    : LiveData<Orientation>(), SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticField: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val orientation = FloatArray(3)    // orientation angles from mAcceleration and mMagnet
    private var rotationMatrix = FloatArray(9)

    override fun onActive() {
        super.onActive()

        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, sensorDelay)
        }
        magneticField?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, sensorDelay)
        }
    }

    override fun onInactive() {
        super.onInactive()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)  // save datas
                calculateAccMagOrientation()                                    // then calculate new orientation
            }
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, magnetometerReading, 0, 3) // save datas
            else -> {
            }
        }
    }

    private fun calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading))
            SensorManager.getOrientation(rotationMatrix, orientation)
        else { // Most chances are that there are no magnet datas

            val gx = (accelerometerReading[0] / 9.81f).toDouble()
            val gy = (accelerometerReading[1] / 9.81f).toDouble()
            val gz = (accelerometerReading[2] / 9.81f).toDouble()

            // http://theccontinuum.com/2012/09/24/arduino-imu-pitch-roll-from-accelerometer/
            val pitch = (-Math.atan(gy / Math.sqrt(gx * gx + gz * gz))).toFloat()
            val roll = (-Math.atan(gx / Math.sqrt(gy * gy + gz * gz))).toFloat()
            val azimuth = 0f // Impossible to guess

            orientation[0] = azimuth
            orientation[1] = pitch
            orientation[2] = roll
            rotationMatrix = getRotationMatrixFromOrientation(orientation)
            SensorManager.getOrientation(rotationMatrix, orientation)
        }

        value = Orientation(orientation[0], orientation[1], orientation[2])
    }

    private fun getRotationMatrixFromOrientation(o: FloatArray): FloatArray {
        val xM = FloatArray(9)
        val yM = FloatArray(9)
        val zM = FloatArray(9)

        val sinX = Math.sin(o[1].toDouble()).toFloat()
        val cosX = Math.cos(o[1].toDouble()).toFloat()
        val sinY = Math.sin(o[2].toDouble()).toFloat()
        val cosY = Math.cos(o[2].toDouble()).toFloat()
        val sinZ = Math.sin(o[0].toDouble()).toFloat()
        val cosZ = Math.cos(o[0].toDouble()).toFloat()

        // rotation about x-axis (pitch)
        xM[0] = 1.0f
        xM[1] = 0.0f
        xM[2] = 0.0f
        xM[3] = 0.0f
        xM[4] = cosX
        xM[5] = sinX
        xM[6] = 0.0f
        xM[7] = -sinX
        xM[8] = cosX

        // rotation about y-axis (roll)
        yM[0] = cosY
        yM[1] = 0.0f
        yM[2] = sinY
        yM[3] = 0.0f
        yM[4] = 1.0f
        yM[5] = 0.0f
        yM[6] = -sinY
        yM[7] = 0.0f
        yM[8] = cosY

        // rotation about z-axis (azimuth)
        zM[0] = cosZ
        zM[1] = sinZ
        zM[2] = 0.0f
        zM[3] = -sinZ
        zM[4] = cosZ
        zM[5] = 0.0f
        zM[6] = 0.0f
        zM[7] = 0.0f
        zM[8] = 1.0f

        // rotation order is y, x, z (roll, pitch, azimuth)
        var resultMatrix = matrixMultiplication(xM, yM)
        resultMatrix = matrixMultiplication(zM, resultMatrix)
        return resultMatrix
    }

    private fun matrixMultiplication(A: FloatArray, B: FloatArray): FloatArray {
        val result = FloatArray(9)

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6]
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7]
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8]

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6]
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7]
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8]

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6]
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7]
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8]

        return result
    }
}