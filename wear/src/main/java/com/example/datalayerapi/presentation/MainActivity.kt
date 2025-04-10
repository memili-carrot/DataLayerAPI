package com.example.datalayerapi.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var latestX by mutableStateOf(0f)
    private var latestY by mutableStateOf(0f)
    private var latestZ by mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            AccelerometerApp()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            latestX = it.values[0]
            latestY = it.values[1]
            latestZ = it.values[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    fun AccelerometerApp() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("X: $latestX")
            Text("Y: $latestY")
            Text("Z: $latestZ")

            Spacer(Modifier.height(16.dp))

            Button(onClick = { sendAccelerometerData(latestX, latestY, latestZ) }) {
                Text("Send")
            }
        }
    }

    private fun sendAccelerometerData(x: Float, y: Float, z: Float) {
        val json = JSONObject().apply {
            put("x", x)
            put("y", y)
            put("z", z)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        lifecycleScope.launch(Dispatchers.IO) {
            val nodeId = getNodes().firstOrNull()
            nodeId?.let {
                Wearable.getMessageClient(applicationContext).sendMessage(
                    it, "/accelerometer", json.toByteArray()
                ).apply {
                    addOnSuccessListener { Log.d("MainActivity", "Accelerometer data sent: $json") }
                    addOnFailureListener { Log.d("MainActivity", "Failed to send accelerometer data") }
                }
            }
        }
    }

    private fun getNodes(): Collection<String> {
        return Tasks.await(Wearable.getNodeClient(this).connectedNodes).map { it.id }
    }
}