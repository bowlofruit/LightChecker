package com.bowlof.lightchecker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.bowlof.lightchecker.domain.repository.UiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiPreferencesRepositoryImpl @Inject constructor(
    private val preferences: DataStore<Preferences>,
) : UiPreferencesRepository {

    override val demoUiScheduleEnabled: Flow<Boolean> = preferences.data
        .map { it[DEMO_UI_SCHEDULE_KEY] == true }
        .distinctUntilChanged()

    override suspend fun setDemoUiScheduleEnabled(enabled: Boolean) {
        preferences.edit { it[DEMO_UI_SCHEDULE_KEY] = enabled }
    }

    private companion object {
        val DEMO_UI_SCHEDULE_KEY = booleanPreferencesKey("ui_demo_schedule")
    }
}
