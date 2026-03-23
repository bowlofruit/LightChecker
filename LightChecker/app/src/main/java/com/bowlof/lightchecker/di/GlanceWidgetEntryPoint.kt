package com.bowlof.lightchecker.di

import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlanceWidgetEntryPoint {
    fun database(): LightCheckerDatabase
}
