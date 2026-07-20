package com.sylo.com

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sylo.com.sms.BankSmsCaptureScheduler
import com.sylo.core.common.locale.LocaleHelper
import com.sylo.core.database.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Hilt application root — the top of the DI graph for the whole app. */
@HiltAndroidApp
class SyloApplication : Application(), Configuration.Provider {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    /** Lets WorkManager construct our @HiltWorker with its injected dependencies. */
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var smsCaptureScheduler: BankSmsCaptureScheduler
    @Inject lateinit var userPreferences: UserPreferencesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Logging is driven by the flavor's ENABLE_LOGGING flag (on for dev/staging,
        // off for prod) rather than the build type, so a prod-debug build stays quiet.
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
        }

        // The Settings toggle only persists the preference; :app owns the actual
        // scheduling. Enabling (re)schedules the periodic scan and runs one now, so
        // opening the app also catches up on today's bank SMS.
        appScope.launch {
            userPreferences.smsAutoCaptureEnabled.distinctUntilChanged().collect { enabled ->
                if (enabled) smsCaptureScheduler.enable() else smsCaptureScheduler.disable()
            }
        }
    }
}
