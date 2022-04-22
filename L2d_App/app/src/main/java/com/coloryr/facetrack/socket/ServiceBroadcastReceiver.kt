package com.coloryr.facetrack.socket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ServiceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(
            ConnectService.TAG, Thread.currentThread().name + "---->"
                    + "ServiceBroadcastReceiver onReceive"
        )
        val action = intent.action
        if (START_ACTION.equals(action, ignoreCase = true)) {
            if (ConnectService.isStart) return
            val intent1 = Intent(context, ConnectService::class.java)
            context.startForegroundService(intent1)
            Log.d(
                ConnectService.TAG, Thread.currentThread().name + "---->"
                        + "ServiceBroadcastReceiver onReceive start end"
            )
        }
    }

    companion object {
        const val START_ACTION = "Live2dStart"
    }
}