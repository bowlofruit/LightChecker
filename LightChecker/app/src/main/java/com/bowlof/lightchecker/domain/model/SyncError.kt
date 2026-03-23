package com.bowlof.lightchecker.domain.model

sealed class SyncException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class Network(cause: Throwable) : SyncException(cause = cause)

    class Parse(message: String) : SyncException(message)

    class PermissionDenied : SyncException("permission_denied")

    class Unknown(cause: Throwable) : SyncException(cause = cause)
}
