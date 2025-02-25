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
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var transcriptionNodeId: String? = null

    private var lastZ = 0f
    private var lastZDirection = 0  // 이전 Z축 방향 (1: 올리기, -1: 내리기)
    private var reps by mutableStateOf(0)  // 반복 횟수
    private var isRunning by mutableStateOf(false)  // 타이머 상태

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            TimerApp()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (!isRunning) return  // 타이머가 동작 중일 때만 측정

            val z = it.values[2]  // Z축 값 가져오기
            val threshold = 2.5f  // 임계값 설정

            // Z축 변화량이 임계값보다 크면 동작 시작
            if (Math.abs(z - lastZ) > threshold) {
                // Z축의 방향 전환을 감지
                if (z > lastZ && lastZDirection != 1) {  // 올리기 (양의 가속도)
                    lastZDirection = 1
                } else if (z < lastZ && lastZDirection != -1) {  // 내리기 (음의 가속도)
                    lastZDirection = -1
                    reps++  // 내림 동작 후 카운트 증가
                    Log.d("MainActivity", "반복 횟수 증가: $reps")
                }
            }
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    fun TimerApp() {
        var timeInSeconds by remember { mutableStateOf(0L) }

        LaunchedEffect(isRunning) {
            while (isRunning) {
                kotlinx.coroutines.delay(1000L)
                timeInSeconds++
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "반복 횟수: $reps",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = formatTime(timeInSeconds),
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = { isRunning = true }) {
                    Text("S")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(onClick = { isRunning = false }) {
                    Text("F")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(onClick = {
                    isRunning = false
                    timeInSeconds = 0L
                    reps = 0  // 초기화
                }) {
                    Text("R")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { sendMessage(reps, timeInSeconds) },
                modifier = Modifier
                    .width(100.dp)
                    .height(36.dp) // 버튼 높이 조절
            ) {
                Text("Send", fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        }
    }

    private fun sendMessage(reps: Int, duration: Long) {
        val workoutData = WorkoutData(reps, WorkoutData.getCurrentTimestamp(), duration)
        val jsonMessage = workoutData.toJson()

        lifecycleScope.launch(Dispatchers.IO) {
            val nodeId = getNodes().firstOrNull()
            nodeId?.let {
                Wearable.getMessageClient(applicationContext).sendMessage(
                    it, MESSAGE_PATH, jsonMessage.toByteArray()
                ).apply {
                    addOnSuccessListener { Log.d(TAG, "Message sent: $jsonMessage") }
                    addOnFailureListener { Log.d(TAG, "Message failed") }
                }
            }
        }
    }

    private fun getNodes(): Collection<String> {
        return Tasks.await(Wearable.getNodeClient(this).connectedNodes).map { it.id }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val MESSAGE_PATH = "/deploy"
    }
}

fun formatTime(seconds: Long): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
