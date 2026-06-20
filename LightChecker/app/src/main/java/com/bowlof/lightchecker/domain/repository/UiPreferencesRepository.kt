package com.bowlof.lightchecker.domain.repository

import kotlinx.coroutines.flow.Flow

/** Доступ до UI-налаштувань застосунку (абстракція над DataStore). */
interface UiPreferencesRepository {

    /** Чи показувати демонстраційний розклад (лише debug-білд). */
    val demoUiScheduleEnabled: Flow<Boolean>

    suspend fun setDemoUiScheduleEnabled(enabled: Boolean)
}
