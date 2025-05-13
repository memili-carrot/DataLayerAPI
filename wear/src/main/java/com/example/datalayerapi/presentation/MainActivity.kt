package com.example.datalayerapi.presentation

import android.content.*
import android.hardware.*
import android.os.*
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator

    private var watchStatus by mutableStateOf("Waiting")
    private val sensorMap = mutableMapOf<SensorType, Sensor?>()
    private val bufferMap = mutableMapOf<SensorType, SensorDataBuffer>()
    private var latestValues by mutableStateOf<Map<SensorType, Triple<Float, Float, Float>>>(emptyMap())

    // ✅ 설정 수신/종료 감지 브로드캐스트 리시버
    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newStatus = intent?.getStringExtra("status") ?: return
            updateWatchStatus(newStatus)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        vibrator = getSystemService(Vibrator::class.java)

        // ✅ 브로드캐스트 등록
        registerReceiver(
            configReceiver,
            IntentFilter("com.example.datalayerapi.CONFIG_RECEIVED"),
            RECEIVER_NOT_EXPORTED // 👈 이 부분이 필수
        )

        setContent {
            SensorStatusUI()
        }
    }

    @Composable
    fun SensorStatusUI() {
        val isLoading = watchStatus.contains("수집중") || watchStatus.contains("전송중")
        val battery = getBatteryPercentage()

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("State: $watchStatus", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text("Battery: ${if (battery >= 0) "$battery%" else "None"}")
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    private fun getBatteryPercentage(): Int {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val type = SensorType.values().find { it.androidType == event.sensor.type } ?: return
            val x = event.values.getOrElse(0) { 0f }
            val y = event.values.getOrElse(1) { 0f }
            val z = event.values.getOrElse(2) { 0f }

            latestValues = latestValues.toMutableMap().apply {
                put(type, Triple(x, y, z))
            }

            bufferMap[type]?.add(SensorData(x, y, z, System.currentTimeMillis(), type.label))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendSensorData() {
        updateWatchStatus("전송중")

        lifecycleScope.launch(Dispatchers.IO) {
            val nodeId = getNodes().firstOrNull()
            nodeId?.let {
                val jsonObject = JSONObject()
                bufferMap.forEach { (type, buffer) ->
                    jsonObject.put(type.name.lowercase(), buffer.toJsonArray())
                }

                Wearable.getMessageClient(applicationContext).sendMessage(
                    it, "/multi_sensor", jsonObject.toString().toByteArray()
                ).apply {
                    addOnSuccessListener {
                        Log.d("MainActivity", "✅ Sent multi sensor data: ${jsonObject.toString(2)}")
                        updateWatchStatus("전송 완료")
                    }
                    addOnFailureListener {
                        Log.d("MainActivity", "❌ Failed to send sensor data: ${it.message}")
                        updateWatchStatus("전송 실패")
                    }
                }
            }
        }
    }

    private fun getNodes(): Collection<String> {
        return Tasks.await(Wearable.getNodeClient(this).connectedNodes).map { it.id }
    }

    private fun updateWatchStatus(status: String) {
        watchStatus = status
        vibrateShort()
    }

    private fun vibrateShort() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(configReceiver) // ✅ 해제
    }
}