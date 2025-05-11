package com.example.datalayerapi.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
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
    private val sensorName: String = SensorType.values()
        .find { it.androidType == sensor.type }?.label ?: "Unknown"

    private var batteryBefore: Int = -1
    private var batteryAfter: Int = -1

    fun start() {
        if (!isCollecting) {
            sensorManager.registerListener(this, sensor, sensorDelay)
            isCollecting = true

            // 배터리 수집 시작 시점 측정
            batteryBefore = getBatteryLevel()
            Log.d(TAG, "🔋 시작 배터리: $batteryBefore%")

            handler.postDelayed({
                stopAndSend()
            }, durationSec * 1000L)
        }
    }

    private fun stopAndSend() {
        if (isCollecting) {
            sensorManager.unregisterListener(this)
            isCollecting = false

            // 배터리 종료 시점 측정
            batteryAfter = getBatteryLevel()
            Log.d(TAG, "🔋 종료 배터리: $batteryAfter%")

            val batteryUsed = batteryBefore - batteryAfter
            Log.d(TAG, "📉 사용된 배터리: ${batteryUsed}% ($sensorName)")

            if (buffer.isNotEmpty()) {
                sendBuffer(buffer.toList())
                buffer.clear()
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun sendBuffer(dataToSend: List<SensorData>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodeId = Wearable.getNodeClient(context).connectedNodes.await()
                    .firstOrNull()?.id

                nodeId?.let { id ->
                    val jsonArray = JSONArray()
                    dataToSend.forEach { data ->
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
                            Log.d(TAG, "✅ Sent ${dataToSend.size} from $sensorName")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "❌ Failed to send buffer from $sensorName", it)
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

            buffer.add(SensorData(x, y, z, timestamp, sensorName))

            if (buffer.size >= 100) {
                sendBuffer(buffer.toList())
                buffer.clear()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "SensorCollector"
    }
}