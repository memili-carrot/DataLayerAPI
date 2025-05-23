package com.example.datalayerapi

data class WorkoutData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
    val sensor: String
) {
    companion object {
        fun fromJson(json: String): WorkoutData? {
            return try {
                val obj = org.json.JSONObject(json)
                val sensor = obj.getString("sensor")
                val timestamp = obj.getLong("timestamp")

                when (sensor) {
                    "Light" -> {
                        val lux = obj.getDouble("lux").toFloat()
                        WorkoutData(x = lux, y = 0f, z = 0f, timestamp = timestamp, sensor = sensor)
                    }
                    "HeartRate" -> {
                        // heartrate 또는 bpm 둘 다 수용
                        val bpm = if (obj.has("heartrate")) obj.getDouble("heartrate")
                        else obj.optDouble("bpm", 0.0)
                        WorkoutData(x = bpm.toFloat(), y = 0f, z = 0f, timestamp = timestamp, sensor = sensor)
                    }
                    "GPS" -> {
                        val lat = obj.getDouble("x").toFloat() // 위도
                        val lon = obj.getDouble("y").toFloat() // 경도
                        WorkoutData(x = lat, y = lon, z = 0f, timestamp = timestamp, sensor = sensor)
                    }
                    else -> {
                        WorkoutData(
                            x = obj.getDouble("x").toFloat(),
                            y = obj.getDouble("y").toFloat(),
                            z = obj.getDouble("z").toFloat(),
                            timestamp = timestamp,
                            sensor = sensor
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}