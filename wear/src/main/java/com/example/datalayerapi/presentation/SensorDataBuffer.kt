package com.example.datalayerapi.presentation

class SensorDataBuffer(private val maxSize: Int = 10) {
    private val buffer = mutableListOf<SensorData>()

    fun add(data: SensorData) {
        if (buffer.size >= maxSize) {
            buffer.removeAt(0) // 가장 오래된 데이터 제거
        }
        buffer.add(data)
    }

    fun getAll(): List<SensorData> = buffer.toList()

    fun clear() {
        buffer.clear()
    }
}