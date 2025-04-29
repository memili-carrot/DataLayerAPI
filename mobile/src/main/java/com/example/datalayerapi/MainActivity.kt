package com.example.datalayerapi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
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

class MainActivity : AppCompatActivity() {

    private lateinit var sensorConfigRecyclerView: RecyclerView
    private lateinit var messageRecyclerView: RecyclerView  // ✨ 추가
    private lateinit var addSensorButton: Button
    private lateinit var sendButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var sensorNameTextView: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<WorkoutData>()
    private val configList = mutableListOf<SensorConfigItem>()
    private lateinit var sensorConfigAdapter: SensorConfigAdapter
    private lateinit var vibrator: Vibrator
    private var phoneStatus: String = "대기 중"

    private val delayOptions = listOf("FASTEST", "GAME", "UI", "NORMAL")
    private val sensorOptions = listOf("Accelerometer", "Gyroscope", "Light", "Magnetic", "Gravity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorConfigRecyclerView = findViewById(R.id.sensorConfigRecyclerView)
        messageRecyclerView = findViewById(R.id.messageRecyclerView)  // ✨ 추가

        addSensorButton = findViewById(R.id.addSensorButton)
        sendButton = findViewById(R.id.sendButton)
        statusTextView = findViewById(R.id.statusTextView)
        sensorNameTextView = findViewById(R.id.sensorNameTextView)
        vibrator = getSystemService(Vibrator::class.java)

        updatePhoneStatus("대기 중")

        sensorConfigAdapter = SensorConfigAdapter(
            this,
            sensorOptions,
            delayOptions,
            configList
        )
        sensorConfigRecyclerView.layoutManager = LinearLayoutManager(this)
        sensorConfigRecyclerView.adapter = sensorConfigAdapter

        // ✨ 메시지 표시용 RecyclerView 세팅
        messageAdapter = MessageAdapter(messageList)
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = messageAdapter

        addSensorButton.setOnClickListener {
            configList.add(SensorConfigItem("", "", 5))
            sensorConfigAdapter.notifyItemInserted(configList.size - 1)
        }

        sendButton.setOnClickListener {
            currentFocus?.clearFocus() // ✨ 포커스 해제

            val configJson = JSONArray()
            configList.forEach { config ->
                if (config.sensorName.isNotEmpty() && config.delayOption.isNotEmpty() && config.durationSec > 0) {
                    val obj = JSONObject().apply {
                        put("sensor", config.sensorName)
                        put("delay", config.delayOption)
                        put("durationSec", config.durationSec)
                    }
                    configJson.put(obj)
                }
            }

            sendConfigToWatch(configJson)
            updatePhoneStatus("센서 설정 전송 완료")
        }

        val filter = IntentFilter().apply {
            addAction("com.example.datalayerapi.ACCELEROMETER_RECEIVED")
            addAction("com.example.datalayerapi.GYROSCOPE_RECEIVED")
            addAction("com.example.datalayerapi.LIGHT_RECEIVED")
            addAction("com.example.datalayerapi.MAGNETIC_RECEIVED")
            addAction("com.example.datalayerapi.GRAVITY_RECEIVED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorReceiver, filter)
    }

    private fun sendConfigToWatch(configArray: JSONArray) {
        val payload = JSONObject().apply {
            put("sensors", configArray)
        }

        Thread {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(this).connectedNodes)
                if (nodes.isEmpty()) {
                    Log.e("MainActivity", "❌ 연결된 워치 노드 없음")
                    runOnUiThread {
                        Toast.makeText(this, "연결된 워치 없음", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val node = nodes.first()
                Log.d("MainActivity", "✅ 연결된 워치 노드: ${node.displayName}")

                Wearable.getMessageClient(this)
                    .sendMessage(node.id, "/config_multi", payload.toString().toByteArray())
                    .addOnSuccessListener {
                        runOnUiThread {
                            Toast.makeText(this, "설정 전송 완료", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "❌ 설정 전송 실패: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this, "전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

            } catch (e: Exception) {
                Log.e("MainActivity", "❌ 전송 시도 중 에러: ${e.message}", e)
            }
        }.start()
    }

    private fun updatePhoneStatus(status: String) {
        phoneStatus = status
        statusTextView.text = "상태: $phoneStatus"
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private val sensorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("JsonData")?.let { json ->
                try {
                    val jsonObj = JSONObject(json)
                    val sensorKeys = jsonObj.keys()
                    while (sensorKeys.hasNext()) {
                        val key = sensorKeys.next()
                        val dataArray = jsonObj.getJSONArray(key)
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val data = WorkoutData.fromJson(item.toString())
                            data?.let {
                                messageList.add(it)
                                messageAdapter.notifyItemInserted(messageList.size - 1)
                            }
                        }
                    }
                    sensorNameTextView.text = "📦 수신 완료"
                    updatePhoneStatus("수신 완료")
                } catch (e: Exception) {
                    Log.e("MainActivity", "수신 파싱 오류: ${e.message}", e)
                }
            }
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorReceiver)
        super.onDestroy()
    }
}