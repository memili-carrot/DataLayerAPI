package com.example.datalayerapi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messageList = mutableListOf<WorkoutData>()

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val json = intent?.getStringExtra("JsonData") ?: return
            Log.d("MainActivity", "Received JSON: $json")
            WorkoutData.fromJson(json)?.let {
                // 💡 최근 10개 유지 로직
                if (messageList.size >= 10) {
                    messageList.removeAt(0) // 가장 오래된 데이터 제거
                    adapter.notifyItemRemoved(0)
                }

                messageList.add(it)
                adapter.notifyItemInserted(messageList.size - 1)
                recyclerView.scrollToPosition(messageList.size - 1)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.messageRecyclerView)
        adapter = MessageAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val filter = IntentFilter("com.example.datalayerapi.ACCELEROMETER_RECEIVED")
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(messageReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(messageReceiver)
    }
}