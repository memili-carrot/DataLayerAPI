package com.example.datalayerapi.presentation

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.hardware.Sensor
import kotlinx.coroutines.tasks.await

class ConfigListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/config") {
            val configJson = String(messageEvent.data)
            Log.d(TAG, "📥 Config received: $configJson")

            try {
                val obj = JSONObject(configJson)
                val sensorName = obj.getString("sensor")
                val sensorDelay = obj.getInt("sensorDelay")
                val durationSec = obj.getInt("durationSec")

                val sensorType = when (sensorName) {
                    "Accelerometer" -> Sensor.TYPE_ACCELEROMETER
                    "Gyroscope" -> Sensor.TYPE_GYROSCOPE
                    "Light" -> Sensor.TYPE_LIGHT
                    else -> {
                        Log.w(TAG, "Unsupported sensor: $sensorName")
                        return
                    }
                }

                val collector = SensorCollector(
                    context = this,
                    sensorType = sensorType,
                    sensorDelay = sensorDelay,
                    durationSec = durationSec
                ) { result ->
                    sendSensorData(sensorName, result)
                }

                collector.start()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to parse config: ${e.message}")
            }
        }
    }

    private fun sendSensorData(sensorName: String, data: List<SensorData>) {
        val dataArray = data.map {
            JSONObject().apply {
                put("x", it.x)
                put("y", it.y)
                put("z", it.z)
                put("timestamp", it.timestamp)
            }
        }

        val json = JSONObject().apply {
            put("sensor", sensorName)
            put("data", dataArray.toString())
        }.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val nodes = Wearable.getNodeClient(this@ConfigListenerService).connectedNodes.await()
            val nodeId = nodes.firstOrNull()?.id ?: return@launch
            val path = "/${sensorName.lowercase()}"
            Wearable.getMessageClient(applicationContext).sendMessage(nodeId, path, json.toByteArray())
                .addOnSuccessListener { Log.d(TAG, "Sent $sensorName data to phone") }
                .addOnFailureListener { Log.e(TAG, "Send failed: ${it.message}") }
        }
    }

    companion object {
        private const val TAG = "ConfigListenerService"
    }
}