package com.example.datalayerapi

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.provider.MediaStore
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
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var sensorConfigRecyclerView: RecyclerView
    private lateinit var sensorDataRecyclerView: RecyclerView  // ✅ 추가
    private lateinit var addSensorButton: Button
    private lateinit var sendButton: Button
    private lateinit var saveButton: Button
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
        sensorDataRecyclerView = findViewById(R.id.messageRecyclerView)
        addSensorButton = findViewById(R.id.addSensorButton)
        sendButton = findViewById(R.id.sendButton)
        saveButton = findViewById(R.id.saveDataButton)
        statusTextView = findViewById(R.id.statusTextView)
        sensorNameTextView = findViewById(R.id.sensorNameTextView)
        vibrator = getSystemService(Vibrator::class.java)

        updatePhoneStatus("대기 중")

        sensorConfigAdapter = SensorConfigAdapter(this, sensorOptions, delayOptions, configList)
        sensorConfigRecyclerView.layoutManager = LinearLayoutManager(this)
        sensorConfigRecyclerView.adapter = sensorConfigAdapter

        messageAdapter = MessageAdapter(messageList)
        sensorDataRecyclerView.layoutManager = LinearLayoutManager(this)
        sensorDataRecyclerView.adapter = messageAdapter  // ✅ 여기서 연결

        addSensorButton.setOnClickListener {
            configList.add(SensorConfigItem("", "", 5))
            sensorConfigAdapter.notifyItemInserted(configList.size - 1)
        }

        sendButton.setOnClickListener {
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

        saveButton.setOnClickListener {
            saveSensorDataToDownloadsFolder()
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
                val node = Tasks.await(Wearable.getNodeClient(this).connectedNodes).firstOrNull()
                node?.let {
                    Wearable.getMessageClient(this)
                        .sendMessage(it.id, "/config_multi", payload.toString().toByteArray())
                        .addOnSuccessListener {
                            runOnUiThread {
                                Toast.makeText(this, "설정 전송 완료", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread {
                                Toast.makeText(this, "전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "전송 중 오류: ${e.message}", e)
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
                                messageAdapter.notifyItemInserted(messageList.size - 1)  // ✅ 수신 후 리스트 갱신
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

    private fun saveSensorDataToDownloadsFolder() {
        val jsonArray = JSONArray()
        messageList.forEach { data ->
            val obj = JSONObject().apply {
                put("timestamp", data.timestamp)
                put("sensor", data.sensor)
                put("x", data.x)
                put("y", data.y)
                put("z", data.z)
            }
            jsonArray.put(obj)
        }

        val fileName = "sensor_data_${getCurrentTimestamp()}.json"
        val fileContent = jsonArray.toString(4).toByteArray()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(fileContent)
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use {
                it.write(fileContent)
            }
        }

        runOnUiThread {
            Toast.makeText(this, "✅ 다운로드 폴더에 저장 완료: $fileName", Toast.LENGTH_LONG).show()
        }
    }

    private fun getCurrentTimestamp(): String {
        return System.currentTimeMillis().toString()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorReceiver)
        super.onDestroy()
    }
}