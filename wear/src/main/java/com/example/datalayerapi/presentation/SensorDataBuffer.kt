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
        buffer.forEach { data ->
            array.put(JSONObject().apply {
                put("x", data.x)
                put("y", data.y)
                put("z", data.z)
                put("timestamp", data.timestamp)
            })
        }
        return array
    }

    fun clear() {
        buffer.clear()
    }

    fun getAll(): List<SensorData> = buffer
}