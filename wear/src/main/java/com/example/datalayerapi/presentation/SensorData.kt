package com.example.datalayerapi.presentation

data class SensorData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
    val sensorName: String
)