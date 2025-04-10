package com.example.datalayerapi

data class WorkoutData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
) {
    fun toJson(): String {
        return org.json.JSONObject().apply {
            put("x", x)
            put("y", y)
            put("z", z)
            put("timestamp", timestamp)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): WorkoutData? {
            return try {
                val jsonObject = org.json.JSONObject(json)
                WorkoutData(
                    x = jsonObject.getDouble("x").toFloat(),
                    y = jsonObject.getDouble("y").toFloat(),
                    z = jsonObject.getDouble("z").toFloat(),
                    timestamp = jsonObject.getLong("timestamp")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}