package com.example.datalayerapi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject
import android.hardware.SensorManager

class MainActivity : AppCompatActivity() {

    private lateinit var sensorSpinner: Spinner
    private lateinit var durationEditText: EditText
    private lateinit var delaySpinner: Spinner
    private lateinit var sendButton: Button
    private lateinit var sensorNameTextView: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<WorkoutData>()

    private val sensorTypes = listOf("Accelerometer", "Gyroscope", "Light")
    private val delayOptions = listOf("FASTEST", "GAME", "UI", "NORMAL")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorSpinner = findViewById(R.id.sensorSpinner)
        durationEditText = findViewById(R.id.durationEditText)
        delaySpinner = findViewById(R.id.delaySpinner)
        sendButton = findViewById(R.id.sendButton)
        sensorNameTextView = findViewById(R.id.sensorNameTextView)

        recyclerView = findViewById(R.id.messageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(messageList)
        recyclerView.adapter = messageAdapter

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sensorTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sensorSpinner.adapter = adapter

        val delayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, delayOptions)
        delayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        delaySpinner.adapter = delayAdapter

        sendButton.setOnClickListener {
            val sensor = sensorSpinner.selectedItem.toString()
            val duration = durationEditText.text.toString().toIntOrNull() ?: 5
            val delayLabel = delaySpinner.selectedItem.toString()

            // ❗ 수정: delayLabel을 숫자(SensorManager 상수)로 변환
            val sensorDelay = when (delayLabel) {
                "FASTEST" -> SensorManager.SENSOR_DELAY_FASTEST
                "GAME" -> SensorManager.SENSOR_DELAY_GAME
                "UI" -> SensorManager.SENSOR_DELAY_UI
                "NORMAL" -> SensorManager.SENSOR_DELAY_NORMAL
                else -> SensorManager.SENSOR_DELAY_NORMAL
            }

            sendConfigToWatch(sensor, duration, sensorDelay)
        }

        val filter = IntentFilter().apply {
            addAction("com.example.datalayerapi.GYROSCOPE_RECEIVED")
            addAction("com.example.datalayerapi.ACCELEROMETER_RECEIVED")
            addAction("com.example.datalayerapi.LIGHT_RECEIVED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(gyroReceiver, filter)
    }

    // ❗ sendConfigToWatch 함수 수정
    private fun sendConfigToWatch(sensor: String, duration: Int, sensorDelay: Int) {
        val configJson = JSONObject().apply {
            put("sensor", sensor)
            put("durationSec", duration)
            put("sensorDelay", sensorDelay) // 숫자형 전송
        }

        Thread {
            try {
                val node = Tasks.await(Wearable.getNodeClient(this).connectedNodes).firstOrNull()
                node?.let {
                    Wearable.getMessageClient(this)
                        .sendMessage(it.id, "/config", configJson.toString().toByteArray())
                        .addOnSuccessListener {
                            runOnUiThread {
                                Toast.makeText(this, "설정 전송 완료", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            runOnUiThread {
                                Toast.makeText(this, "전송 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } ?: run {
                    Log.w("MainActivity", "No connected node found to send config")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception during sendConfigToWatch: ${e.message}", e)
            }
        }.start()
    }

    private val gyroReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            intent?.getStringExtra("JsonData")?.let { json ->
                Log.d("MainActivity", "📥 Received action: $action, data: $json")
                try {
                    val jsonObj = JSONObject(json)
                    val sensorName = jsonObj.getString("sensor")
                    sensorNameTextView.text = "센서: $sensorName"

                    val dataStr = jsonObj.getString("data")
                    val dataArray = JSONArray(dataStr)
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val data = WorkoutData.fromJson(item.toString())
                        if (data != null) {
                            Log.d("MainActivity", "✅ Parsed data: $data")
                            messageList.add(data)
                            messageAdapter.notifyItemInserted(messageList.size - 1)
                        } else {
                            Log.w("MainActivity", "⚠️ Failed to parse item: $item")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Error parsing received data: ${e.message}", e)
                }
            }
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gyroReceiver)
        super.onDestroy()
    }
}