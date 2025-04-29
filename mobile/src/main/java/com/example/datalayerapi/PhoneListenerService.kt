package com.example.datalayerapi

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val jsonMessage = String(messageEvent.data)
        Log.d(TAG, "Received message on path: $path")

        when (path) {
            "/accelerometer" -> broadcast("com.example.datalayerapi.ACCELEROMETER_RECEIVED", jsonMessage)
            "/gyroscope" -> broadcast("com.example.datalayerapi.GYROSCOPE_RECEIVED", jsonMessage)
            "/light" -> broadcast("com.example.datalayerapi.LIGHT_RECEIVED", jsonMessage)
            "/magnetic" -> broadcast("com.example.datalayerapi.MAGNETIC_RECEIVED", jsonMessage)
            "/gravity" -> broadcast("com.example.datalayerapi.GRAVITY_RECEIVED", jsonMessage)
            else -> Log.w(TAG, "Unknown path received: $path")
        }
    }

    private fun broadcast(action: String, message: String) {
        val intent = Intent(action).apply {
            putExtra("JsonData", message)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "PhoneListenerService"
    }
}