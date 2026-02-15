package com.clonewhatsapp.app.di

import android.content.Context
import com.clonewhatsapp.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    @Named("apiUrl")
    fun provideApiUrl(@Named("baseUrl") baseUrl: String): String = "$baseUrl/api/"

    @Provides
    @Singleton
    @Named("hubUrl")
    fun provideHubUrl(@Named("baseUrl") baseUrl: String): String = "$baseUrl/hubs/chat"
}
