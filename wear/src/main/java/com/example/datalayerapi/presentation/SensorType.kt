package com.example.datalayerapi.presentation

import android.hardware.Sensor

enum class SensorType(val label: String, val androidType: Int) {
    ACCELEROMETER("Accelerometer", Sensor.TYPE_ACCELEROMETER),
    GYROSCOPE("Gyroscope", Sensor.TYPE_GYROSCOPE),
    MAGNETIC_FIELD("Magnetic", Sensor.TYPE_MAGNETIC_FIELD),
    LIGHT("Light", Sensor.TYPE_LIGHT),
    HEART_RATE("HeartRate", Sensor.TYPE_HEART_RATE),

    // GPS는 SensorManager와 무관하므로 androidType을 -1로 처리
    GPS("GPS", -1); // ✅ 여기에 세미콜론 반드시 필요!!

    companion object {
        fun fromLabel(label: String): SensorType? {
            return entries.find { it.label.equals(label, ignoreCase = true) }
        }

        fun fromType(type: Int): SensorType? {
            return entries.find { it.androidType == type }
        }
    }

    fun isSingleValue(): Boolean {
        return this == LIGHT || this == HEART_RATE
    }
}