package com.bowlof.lightchecker.data.remote.dto

data class FirestoreScheduleDto(
    val f: Int,
    val v: Long,
    val d: Long,
    val s: List<Int>,
    val g: Long?,
)
