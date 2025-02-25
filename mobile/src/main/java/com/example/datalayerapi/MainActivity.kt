package com.example.datalayerapi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<WorkoutData>()

    private val sharedPref by lazy {
        getSharedPreferences("messages", Context.MODE_PRIVATE)
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val jsonMessage = intent?.getStringExtra("JsonData") ?: return
            Log.d("MainActivity", "Received via Broadcast: $jsonMessage")

            WorkoutData.fromJson(jsonMessage)?.let { workoutData ->
                saveMessage(workoutData)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageAdapter = MessageAdapter(messageList)

        messageRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }

        // 🔹 앱 실행 시 저장된 메시지 불러오기
        loadMessages()

        // 앱이 처음 실행될 때 Intent로 받은 메시지 처리
        intent?.getByteArrayExtra("MessageData")?.let {
            val receivedMessage = String(it)
            WorkoutData.fromJson(receivedMessage)?.let { workoutData ->
                saveMessage(workoutData)
            }
        }

        // 🔹 플래그 추가하여 registerReceiver() 호출
        val filter = IntentFilter("com.example.datalayerapi.MESSAGE_RECEIVED")
        ContextCompat.registerReceiver(this, messageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(messageReceiver) // 앱 종료 시 BroadcastReceiver 해제
    }

    private fun saveMessage(workoutData: WorkoutData) {
        messageList.add(workoutData)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        messageRecyclerView.scrollToPosition(messageList.size - 1) // 최신 메시지로 스크롤 이동

        // 🔹 메시지를 SharedPreferences에 JSON 배열 형태로 저장
        val jsonArray = JSONArray(messageList.map { it.toJson() })
        sharedPref.edit().putString("saved_messages", jsonArray.toString()).apply()
    }

    private fun loadMessages() {
        val savedMessages = sharedPref.getString("saved_messages", null)
        if (!savedMessages.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(savedMessages)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getString(i)
                    WorkoutData.fromJson(jsonObject)?.let { messageList.add(it) }
                }
                messageAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error parsing saved messages", e)
            }
        }
    }
}
