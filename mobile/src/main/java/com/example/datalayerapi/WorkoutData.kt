package com.example.datalayerapi

data class WorkoutData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
) {
    companion object {
        fun fromJson(json: String): WorkoutData? {
            return try {
                val obj = org.json.JSONObject(json)
                WorkoutData(
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    z = obj.getDouble("z").toFloat(),
                    timestamp = obj.getLong("timestamp")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}