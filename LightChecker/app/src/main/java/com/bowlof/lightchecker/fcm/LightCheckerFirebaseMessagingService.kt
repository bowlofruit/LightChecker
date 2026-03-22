package com.bowlof.lightchecker.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LightCheckerFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        // Sync worker will be triggered here in a later milestone.
    }
}
