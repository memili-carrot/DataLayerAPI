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

            batteryBefore = getBatteryLevel()
            Log.d(TAG, "🔋 시작 배터리: $batteryBefore%")

            handler.postDelayed({
                stopAndSend()
            }, durationSec * 1000L)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isCollecting = false
        buffer.clear()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "🛑 수집 중단: $sensorName")
    }

    private fun stopAndSend() {
        if (isCollecting) {
            sensorManager.unregisterListener(this)
            isCollecting = false

            batteryAfter = getBatteryLevel()
            Log.d(TAG, "🔋 종료 배터리: $batteryAfter%")
            Log.d(TAG, "📉 사용된 배터리: ${batteryBefore - batteryAfter}% ($sensorName)")

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
                            put("timestamp", data.timestamp)
                            put("sensor", data.sensorName)

                            if (sensor.type == Sensor.TYPE_LIGHT) {
                                put("lux", data.x)  // 조도 센서
                            } else if (sensor.type == Sensor.TYPE_HEART_RATE) {
                                put("heartrate", data.x)  // 심박수 센서
                            } else {
                                put("x", data.x)
                                put("y", data.y)
                                put("z", data.z)
                            }
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
            val timestamp = System.currentTimeMillis()

            val (x, y, z) = if (sensor.type == Sensor.TYPE_LIGHT) {
                Triple(it.values.getOrNull(0) ?: 0f, 0f, 0f) // lux 값만 저장
            } else {
                Triple(
                    it.values.getOrNull(0) ?: 0f,
                    it.values.getOrNull(1) ?: 0f,
                    it.values.getOrNull(2) ?: 0f
                )
            }

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