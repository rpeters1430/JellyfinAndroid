package com.rpeters.jellyfin.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.offlineDownloadsDataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_downloads")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideOfflineDownloadsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.offlineDownloadsDataStore
    }
}
