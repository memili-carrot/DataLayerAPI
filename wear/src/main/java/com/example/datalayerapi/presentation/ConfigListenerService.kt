package com.example.datalayerapi.presentation

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class ConfigListenerService : WearableListenerService() {

    private lateinit var sensorManager: SensorManager
    private val activeCollectors = mutableListOf<SensorCollector>()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "📥 Received message: ${messageEvent.path}")

        if (messageEvent.path == "/config_multi") {
            val configJson = String(messageEvent.data)
            Log.d(TAG, "📥 Config received: $configJson")

            try {
                val obj = JSONObject(configJson)
                val sensorsArray = obj.getJSONArray("sensors")

                // ✅ 수정된 부분: 각각의 센서 설정 읽기
                for (i in 0 until sensorsArray.length()) {
                    val item = sensorsArray.getJSONObject(i)
                    val sensorName = item.getString("sensor")
                    val delayLabel = item.getString("delay")
                    val durationSec = item.getInt("durationSec")

                    val sensorDelay = when (delayLabel.uppercase()) {
                        "FASTEST" -> SensorManager.SENSOR_DELAY_FASTEST
                        "GAME" -> SensorManager.SENSOR_DELAY_GAME
                        "UI" -> SensorManager.SENSOR_DELAY_UI
                        "NORMAL" -> SensorManager.SENSOR_DELAY_NORMAL
                        else -> SensorManager.SENSOR_DELAY_NORMAL
                    }

                    val sensorType = SensorType.fromLabel(sensorName)?.androidType
                    if (sensorType != null) {
                        val sensor = sensorManager.getDefaultSensor(sensorType)
                        if (sensor != null) {
                            val collector = SensorCollector(sensorManager, sensor, sensorDelay, durationSec, applicationContext)
                            collector.start()
                            activeCollectors.add(collector)
                            Log.d(TAG, "✅ Registered sensor: $sensorName ($delayLabel, $durationSec sec)")
                        } else {
                            Log.w(TAG, "⚠️ Sensor not found: $sensorName")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Unknown sensor type: $sensorName")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to parse config_multi: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "ConfigListenerService"
    }
}