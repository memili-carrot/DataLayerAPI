package com.example.datalayerapi.presentation

import android.hardware.*
import android.os.*
import android.util.Log
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
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator

    private var watchStatus by mutableStateOf("대기 중")

    // 선택된 센서들
    private var selectedSensors by mutableStateOf(listOf<SensorType>())

    // 센서 객체들
    private val sensorMap = mutableMapOf<SensorType, Sensor?>()

    // 센서별 버퍼
    private val bufferMap = mutableMapOf<SensorType, SensorDataBuffer>()

    // 최신값 상태
    private var latestValues by mutableStateOf<Map<SensorType, Triple<Float, Float, Float>>>(emptyMap())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        vibrator = getSystemService(Vibrator::class.java)

        setContent {
            SensorSelectorUI()
        }
    }

    @Composable
    fun SensorSelectorUI() {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text("센서 선택 (${selectedSensors.size})")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SensorType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                expanded = false
                                toggleSensor(type)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            selectedSensors.forEach { type ->
                val values = latestValues[type] ?: Triple(0f, 0f, 0f)
                Text("${type.label} X: ${values.first}")
                Text("${type.label} Y: ${values.second}")
                Text("${type.label} Z: ${values.third}")
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("상태: $watchStatus")

            Spacer(Modifier.height(16.dp))
            Button(onClick = { sendSensorData() }) {
                Text("Send Collected Data")
            }
        }
    }

    private fun toggleSensor(type: SensorType) {
        if (selectedSensors.contains(type)) {
            selectedSensors = selectedSensors - type
            stopSensor(type)
        } else {
            selectedSensors = selectedSensors + type
            startSensor(type)
        }
    }

    private fun startSensor(type: SensorType) {
        val sensor = sensorManager.getDefaultSensor(type.androidType)
        sensor?.let {
            sensorMap[type] = it
            bufferMap[type] = SensorDataBuffer()
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            updateWatchStatus("센서 데이터 수집중")
        }
    }

    private fun stopSensor(type: SensorType) {
        sensorMap[type]?.let {
            sensorManager.unregisterListener(this, it)
        }
        sensorMap.remove(type)
        bufferMap.remove(type)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val matchingType = SensorType.values().find { type -> type.androidType == it.sensor.type }
            matchingType?.let { type ->
                val x = it.values.getOrElse(0) { 0f }
                val y = it.values.getOrElse(1) { 0f }
                val z = it.values.getOrElse(2) { 0f }

                // 최신 값 업데이트
                latestValues = latestValues.toMutableMap().apply {
                    put(type, Triple(x, y, z))
                }

                // 버퍼 추가
                bufferMap[type]?.add(
                    SensorData(
                        x,
                        y,
                        z,
                        System.currentTimeMillis(),
                        type.label
                    )
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendSensorData() {
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
}