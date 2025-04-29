package com.example.datalayerapi.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class SensorCollector(
    private val sensorManager: SensorManager,
    private val sensor: Sensor,
    private val sensorDelay: Int,
    private val durationSec: Int,
    private val context: Context
) : SensorEventListener {

    private val buffer = mutableListOf<SensorData>()
    private var isCollecting = false
    private val handler = Handler(Looper.getMainLooper())
    private val sensorName: String = SensorType.values().find { it.androidType == sensor.type }?.label ?: "Unknown"

    fun start() {
        if (!isCollecting) {
            sensorManager.registerListener(this, sensor, sensorDelay)
            isCollecting = true

            handler.postDelayed({
                stopAndSend()
            }, durationSec * 1000L)
        }
    }

    private fun stopAndSend() {
        if (isCollecting) {
            sensorManager.unregisterListener(this)
            isCollecting = false

            if (buffer.isNotEmpty()) {
                sendBuffer()
            }
        }
    }

    private fun sendBuffer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodeId = Wearable.getNodeClient(context).connectedNodes.await().firstOrNull()?.id
                nodeId?.let { id ->
                    val jsonArray = JSONArray()
                    buffer.forEach { data ->
                        jsonArray.put(JSONObject().apply {
                            put("x", data.x)
                            put("y", data.y)
                            put("z", data.z)
                            put("timestamp", data.timestamp)
                            put("sensor", data.sensorName)
                        })
                    }

                    val jsonObject = JSONObject().apply {
                        put(sensorName.lowercase(), jsonArray)
                    }

                    Wearable.getMessageClient(context)
                        .sendMessage(id, "/${sensorName.lowercase()}", jsonObject.toString().toByteArray())
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Sent sensor: $sensorName")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "❌ Failed to send sensor: $sensorName, ${it.message}")
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending buffer: ${e.message}", e)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values.getOrNull(0) ?: 0f
            val y = it.values.getOrNull(1) ?: 0f
            val z = it.values.getOrNull(2) ?: 0f
            val timestamp = System.currentTimeMillis()

            buffer.add(SensorData(x, y, z, timestamp, sensorName))  // ✅ sensorName 포함
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "SensorCollector"
    }
}