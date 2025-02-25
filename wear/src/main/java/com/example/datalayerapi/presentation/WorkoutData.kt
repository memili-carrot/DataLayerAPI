package com.example.datalayerapi.presentation

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkoutData(
    val reps: Int,
    val timestamp: String,
    val duration: Long
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("reps", reps)
            put("timestamp", timestamp)
            put("duration", duration)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): WorkoutData? {
            return try {
                val jsonObject = JSONObject(json)
                WorkoutData(
                    reps = jsonObject.getInt("reps"),
                    timestamp = jsonObject.getString("timestamp"),
                    duration = jsonObject.getLong("duration")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun getCurrentTimestamp(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }
}
