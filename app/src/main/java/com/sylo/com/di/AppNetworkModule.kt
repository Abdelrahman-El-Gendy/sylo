package com.sylo.com.di

import com.sylo.com.BuildConfig
import com.sylo.core.network.di.BaseUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Feeds the active product flavor's backend URL into the network layer, so
 * dev/staging/prod each hit the right environment (see productFlavors in
 * app/build.gradle.kts).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {

    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL
}
