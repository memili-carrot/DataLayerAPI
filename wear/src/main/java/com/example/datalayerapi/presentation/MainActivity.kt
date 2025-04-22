package com.example.datalayerapi.presentation

import android.hardware.*
import android.os.Bundle
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
import org.json.JSONObject

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var currentSensor: Sensor? = null
    private val buffer = SensorDataBuffer()

    private var selectedSensorType by mutableStateOf(SensorType.ACCELEROMETER)
    private var latestX by mutableFloatStateOf(0f)
    private var latestY by mutableFloatStateOf(0f)
    private var latestZ by mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

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
                    Text("Sensor: ${selectedSensorType.label}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SensorType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                expanded = false
                                switchSensor(type)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("X: $latestX")
            Text("Y: $latestY")
            Text("Z: $latestZ")
            Spacer(Modifier.height(16.dp))

            Button(onClick = { sendSensorData() }) {
                Text("Send Last 10")
            }
        }
    }

    private fun switchSensor(type: SensorType) {
        sensorManager.unregisterListener(this)
        selectedSensorType = type
        currentSensor = sensorManager.getDefaultSensor(type.androidType)
        currentSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            latestX = it.values.getOrElse(0) { 0f }
            latestY = it.values.getOrElse(1) { 0f }
            latestZ = it.values.getOrElse(2) { 0f }

            buffer.add(SensorData(latestX, latestY, latestZ, System.currentTimeMillis()))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendSensorData() {
        val jsonArray = buffer.getAll().map {
            JSONObject().apply {
                put("x", it.x)
                put("y", it.y)
                put("z", it.z)
                put("timestamp", it.timestamp)
            }
        }

        val jsonList = JSONObject().apply {
            put("sensor", selectedSensorType.label)
            put("data", jsonArray)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val nodeId = getNodes().firstOrNull()
            nodeId?.let {
                Wearable.getMessageClient(applicationContext).sendMessage(
                    it, "/${selectedSensorType.name.lowercase()}", jsonList.toString().toByteArray()
                ).apply {
                    addOnSuccessListener {
                        Log.d("MainActivity", "✅ Sent sensor data:\n${jsonList.toString(2)}")
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

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}