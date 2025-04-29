package com.example.datalayerapi.presentation

import android.hardware.SensorManager

enum class DelayType(val label: String) {
    NORMAL("Normal"),
    UI("UI"),
    GAME("Game"),
    FASTEST("Fastest");

    fun toSensorDelay(): Int {
        return when (this) {
            NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
            UI -> SensorManager.SENSOR_DELAY_UI
            GAME -> SensorManager.SENSOR_DELAY_GAME
            FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
        }
    }
}
