package com.bowlof.lightchecker.domain.messaging

private val topicUnsafe = Regex("[^a-zA-Z0-9-_.~%]")

object FirebaseTopicNames {

    /** FCM topic для пари регіон/черга (обмеження довжини топіка). */
    fun forRegionQueue(regionId: String, queueId: String): String {
        val raw = "lc_${regionId}_$queueId"
        return raw.replace(topicUnsafe, "_").take(200)
    }
}
