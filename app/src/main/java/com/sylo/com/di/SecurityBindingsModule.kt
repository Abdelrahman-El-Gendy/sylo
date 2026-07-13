package com.sylo.com.di

import com.sylo.core.database.security.DatabaseKeyProvider
import com.sylo.core.network.auth.AuthTokenProvider
import com.sylo.core.security.crypto.CryptoKeyManager
import com.sylo.core.security.crypto.SessionKeyHolder
import com.sylo.core.security.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The glue that keeps modules decoupled: :core-network and :core-database each
 * declare an interface for what they need (a token, a DB key) without depending on
 * :core-security. Only :app — which knows every module — binds the concrete
 * security implementations to those contracts.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityBindingsModule {

    @Provides
    @Singleton
    fun provideAuthTokenProvider(
        sessionManager: SessionManager,
    ): AuthTokenProvider = object : AuthTokenProvider {
        override fun currentAccessToken(): String? = sessionManager.accessToken()

        override fun refreshAccessToken(): String? {
            // TODO: call the refresh endpoint; for now, no silent refresh.
            return null
        }
    }

    @Provides
    @Singleton
    fun provideDatabaseKeyProvider(
        cryptoKeyManager: CryptoKeyManager,
        sessionKeyHolder: SessionKeyHolder,
    ): DatabaseKeyProvider = object : DatabaseKeyProvider {
        // Prefer the passphrase released by a biometric CryptoObject unlock (held in
        // memory for the session); otherwise use the Keystore-encrypted store (PIN path).
        override fun getDatabasePassphrase(): ByteArray =
            sessionKeyHolder.databasePassphrase ?: cryptoKeyManager.getOrCreateDatabasePassphrase()
    }
}
