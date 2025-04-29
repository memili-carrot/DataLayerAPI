package com.example.datalayerapi.presentation

import android.hardware.Sensor

enum class SensorType(val label: String, val androidType: Int) {
    ACCELEROMETER("Accelerometer", Sensor.TYPE_ACCELEROMETER),
    GYROSCOPE("Gyroscope", Sensor.TYPE_GYROSCOPE),
    LIGHT("Light", Sensor.TYPE_LIGHT),
    MAGNETIC("Magnetic", Sensor.TYPE_MAGNETIC_FIELD),
    GRAVITY("Gravity", Sensor.TYPE_GRAVITY),
    LINEAR_ACCELERATION("Linear Acceleration", Sensor.TYPE_LINEAR_ACCELERATION);

    companion object {
        fun fromLabel(label: String): SensorType? {
            return values().find { it.label.equals(label, ignoreCase = true) }
        }
    }
}