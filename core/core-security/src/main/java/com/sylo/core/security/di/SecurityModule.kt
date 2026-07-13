package com.sylo.core.security.di

import com.sylo.core.security.biometric.BiometricAuthenticator
import com.sylo.core.security.biometric.BiometricAuthenticatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindBiometricAuthenticator(
        impl: BiometricAuthenticatorImpl,
    ): BiometricAuthenticator
}
