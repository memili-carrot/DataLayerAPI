package com.example.datalayerapi

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/accelerometer") {
            val jsonMessage = String(messageEvent.data)
            Log.d(TAG, "Received Accelerometer Data: $jsonMessage")

            val intent = Intent("com.example.datalayerapi.ACCELEROMETER_RECEIVED").apply {
                putExtra("JsonData", jsonMessage)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        } else {
            Log.w(TAG, "Unknown path received: ${messageEvent.path}")
        }
    }

    companion object {
        private const val TAG = "PhoneListenerService"
    }
}