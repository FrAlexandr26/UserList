package com.hugo.userlist

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushServise : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)


    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        var intent = Intent("PUSH_EVENT")
        for (contains in message.data){
            intent.putExtra(contains.key, contains.value)
        }
        sendBroadcast(intent)
    }
}