package com.example.datalayerapi.presentation

import android.Manifest
import android.content.*
import android.hardware.*
import android.os.*
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.content.pm.PackageManager

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var watchStatus by mutableStateOf("Waiting")
    private var batteryPercent by mutableStateOf(-1)

    private val bufferMap = mutableMapOf<SensorType, SensorDataBuffer>()
    private var latestValues by mutableStateOf<Map<SensorType, Triple<Float, Float, Float>>>(emptyMap())

    // ✅ 위치 권한 요청
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) requestLocationUpdate()
            else Log.w("MainActivity", "❌ 위치 권한 거부됨")
        }

    // ✅ 심박수 권한 요청
    private val sensorPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) Log.d("MainActivity", "✅ 심박수 권한 허용됨")
            else Log.w("MainActivity", "❌ 심박수 권한 거부됨")
        }

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateWatchStatus("Config received")
            startSensorCollection(SensorType.ACCELEROMETER, SensorManager.SENSOR_DELAY_NORMAL)
            requestLocationUpdate()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        vibrator = getSystemService(Vibrator::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        registerReceiver(configReceiver, IntentFilter("com.example.datalayerapi.CONFIG_RECEIVED"), RECEIVER_NOT_EXPORTED)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)

        setContent { SensorStatusUI() }

        requestLocationPermission()
        requestHeartRatePermission() // ✅ 심박수 권한 요청 추가
    }

    @Composable
    fun SensorStatusUI() {
        val isLoading = watchStatus.contains("Collecting") || watchStatus.contains("Sending")

        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("State: $watchStatus", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text("Battery: ${if (batteryPercent >= 0) "$batteryPercent%" else "None"}")
            Spacer(Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            requestLocationUpdate()
        }
    }

    // ✅ 심박수 권한 요청
    private fun requestHeartRatePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            sensorPermissionRequest.launch(Manifest.permission.BODY_SENSORS)
        }
    }

    private fun requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "❌ 위치 권한 없음")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                val timestamp = System.currentTimeMillis()
                Log.d("MainActivity", "📍 위치 수신: $latitude, $longitude")

                bufferMap[SensorType.GPS] = bufferMap[SensorType.GPS] ?: SensorDataBuffer()
                bufferMap[SensorType.GPS]?.add(SensorData(latitude.toFloat(), longitude.toFloat(), 0f, timestamp, "GPS"))

                updateWatchStatus("Waiting")
            }
        }.addOnFailureListener {
            Log.e("MainActivity", "❌ 위치 요청 실패", it)
            updateWatchStatus("Waiting")
        }
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

    private fun startSensorCollection(sensorType: SensorType, delay: Int) {
        val sensor = sensorManager.getDefaultSensor(sensorType.androidType)

        // ✅ 심박수 센서 권한 확인
        if (sensorType == SensorType.HEART_RATE &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "❌ 심박수 권한 없음")
            return
        }

        if (sensor != null) {
            bufferMap[sensorType] = SensorDataBuffer()
            updateWatchStatus("Collecting")
            sensorManager.registerListener(this, sensor, delay)
        } else {
            Log.d("MainActivity", "❌ Sensor not available: ${sensorType.name}")
        }
    }

    private fun sendSensorData() {
        updateWatchStatus("Sending")

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
                        Log.d("MainActivity", "✅ Sent multi sensor data")
                        updateWatchStatus("Sent successfully", revertToWaiting = true)
                    }
                    addOnFailureListener {
                        Log.d("MainActivity", "❌ Failed to send sensor data")
                        updateWatchStatus("Send failed", revertToWaiting = true)
                    }
                }
            }
        }
    }

    private fun getNodes(): Collection<String> {
        return Tasks.await(Wearable.getNodeClient(this).connectedNodes).map { it.id }
    }

    private fun updateWatchStatus(status: String, revertToWaiting: Boolean = false) {
        watchStatus = status
        vibrateShort()

        if (revertToWaiting) {
            lifecycleScope.launch {
                delay(1500)
                watchStatus = "Waiting"
            }
        }
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
        unregisterReceiver(configReceiver)
        unregisterReceiver(batteryReceiver)
    }
}