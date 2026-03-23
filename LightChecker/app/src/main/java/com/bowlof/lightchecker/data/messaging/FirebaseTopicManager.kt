package com.bowlof.lightchecker.data.messaging

import com.bowlof.lightchecker.data.local.db.SavedLocationDao
import com.bowlof.lightchecker.domain.messaging.FirebaseTopicNames
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTopicManager @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val savedLocationDao: SavedLocationDao,
) {

    private val mutex = Mutex()
    private var lastTopics: Set<String> = emptySet()

    suspend fun syncSubscriptionsAfterDataChange() {
        mutex.withLock {
            val rows = savedLocationDao.getAllSnapshot()
            val desired = rows.map { FirebaseTopicNames.forRegionQueue(it.regionId, it.queueId) }.toSet()
            val toUnsubscribe = lastTopics - desired
            val toSubscribe = desired - lastTopics
            toUnsubscribe.forEach { topic ->
                runCatching { messaging.unsubscribeFromTopic(topic).await() }
                    .onFailure { Timber.w(it, "unsubscribe $topic") }
            }
            toSubscribe.forEach { topic -> subscribeWithBackoff(topic) }
            lastTopics = desired
        }
    }

    private suspend fun subscribeWithBackoff(topic: String) {
        var backoffMs = 1_000L
        repeat(4) { attempt ->
            runCatching { messaging.subscribeToTopic(topic).await() }
                .onSuccess { return }
                .onFailure { e ->
                    if (attempt == 3) {
                        Timber.w(e, "subscribe failed after retries: %s", topic)
                    } else {
                        delay(backoffMs)
                        backoffMs *= 2
                    }
                }
        }
    }
}
