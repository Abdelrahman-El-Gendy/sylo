package com.sylo.core.network.di

import javax.inject.Qualifier

/**
 * The backend base URL. The value is supplied by the app layer from the active
 * product flavor's `BuildConfig.API_BASE_URL`, so dev/staging/prod each target a
 * different environment without changing the network module.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl
