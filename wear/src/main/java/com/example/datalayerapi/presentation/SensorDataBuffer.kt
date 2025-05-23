package com.example.datalayerapi.presentation

import org.json.JSONArray
import org.json.JSONObject

class SensorDataBuffer {

    private val buffer = mutableListOf<SensorData>()

    fun add(data: SensorData) {
        buffer.add(data)
    }

    fun toJsonArray(): JSONArray {
        val array = JSONArray()
        buffer.forEach {
            array.put(JSONObject().apply {
                put("timestamp", it.timestamp)
                put("sensor", it.sensorName)

                when (it.sensorName) {
                    "Light" -> put("lux", it.x)         // 조도 센서
                    "HeartRate" -> put("bpm", it.x)     // 심박수 센서
                    else -> {
                        put("x", it.x)
                        put("y", it.y)
                        put("z", it.z)
                    }
                }
            })
        }
        return array
    }

    fun clear() {
        buffer.clear()
    }

    fun isEmpty(): Boolean = buffer.isEmpty()

    fun size(): Int = buffer.size
}