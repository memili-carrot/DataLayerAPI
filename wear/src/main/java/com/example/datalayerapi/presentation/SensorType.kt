package com.example.datalayerapi.presentation

import android.hardware.Sensor

enum class SensorType(val label: String, val androidType: Int) {
    ACCELEROMETER("Accelerometer", Sensor.TYPE_ACCELEROMETER),
    GYROSCOPE("Gyroscope", Sensor.TYPE_GYROSCOPE),
    LIGHT("Light", Sensor.TYPE_LIGHT)
}