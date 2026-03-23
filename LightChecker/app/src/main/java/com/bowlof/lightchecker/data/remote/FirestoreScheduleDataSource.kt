package com.bowlof.lightchecker.data.remote

import com.bowlof.lightchecker.data.remote.dto.FirestoreScheduleDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreScheduleDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun fetchSchedule(documentId: String): FirestoreScheduleDto? {
        val snap = firestore.collection(COLLECTION_SCHEDULES)
            .document(documentId)
            .get()
            .await()
        return snap.toFirestoreScheduleDtoOrNull()
    }

    companion object {
        const val COLLECTION_SCHEDULES = "schedules"
    }
}
