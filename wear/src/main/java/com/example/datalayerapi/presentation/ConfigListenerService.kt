package com.example.datalayerapi.presentation

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class ConfigListenerService : WearableListenerService() {

    private lateinit var sensorManager: SensorManager
    private val activeCollectors = mutableListOf<SensorCollector>()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/config_multi") {
            val configJson = String(messageEvent.data)
            Log.d(TAG, "\uD83D\uDCE5 Config received: $configJson")

            try {
                val obj = JSONObject(configJson)
                val sensorsArray = obj.getJSONArray("sensors")

                // ✅ 기존 센서 수집기 정리
                activeCollectors.forEach { it.stop() }
                activeCollectors.clear()

                // 설정 수신 완료 알림
                sendBroadcast(Intent("com.example.datalayerapi.CONFIG_RECEIVED").apply {
                    putExtra("status", "설정 수신 완료")
                })
                Log.d(TAG, "\uD83D\uDCE4 Broadcast sent to MainActivity")

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

                    // ✅ GPS는 별도 처리
                    if (sensorName == "GPS") {
                        val gpsCollector = GpsCollector(applicationContext, durationSec)
                        gpsCollector.start()
                        Log.d(TAG, "📡 Started GPS collector")
                        continue
                    }

                    val sensorType = SensorType.fromLabel(sensorName)?.androidType
                    if (sensorType != null) {
                        val sensor = sensorManager.getDefaultSensor(sensorType)
                        if (sensor != null) {
                            val collector = SensorCollector(
                                sensorManager,
                                sensor,
                                sensorDelay,
                                durationSec,
                                applicationContext
                            )
                            collector.start()
                            activeCollectors.add(collector)
                            Log.d(TAG, "✅ Registered sensor: $sensorName")
                        }
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