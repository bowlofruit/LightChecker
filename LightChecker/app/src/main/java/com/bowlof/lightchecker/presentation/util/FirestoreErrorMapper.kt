package com.bowlof.lightchecker.presentation.util

import android.content.Context
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.domain.model.SyncException
import com.google.firebase.firestore.FirebaseFirestoreException

fun Throwable.toScheduleUserMessage(context: Context): String {
    return when (this) {
        is SyncException.PermissionDenied -> context.getString(R.string.err_permission_denied)
        is SyncException.Parse -> message ?: context.getString(R.string.err_parse)
        is SyncException.Network -> context.getString(R.string.err_network)
        is FirebaseFirestoreException -> {
            val m = message ?: ""
            when {
                m.contains("PERMISSION_DENIED", ignoreCase = true) ->
                    context.getString(R.string.err_permission_denied)
                m.contains("UNAVAILABLE", ignoreCase = true) ->
                    context.getString(R.string.err_unavailable)
                else -> m.ifBlank { context.getString(R.string.err_unknown) }
            }
        }
        else -> message ?: context.getString(R.string.err_unknown)
    }
}
