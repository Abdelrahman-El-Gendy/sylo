package com.sylo.com.sms

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the interval-based bank-SMS rescan. The periodic job is persisted by
 * WorkManager (survives restarts), and enabling also kicks off an immediate one-shot
 * so the first import happens right away rather than at the next interval.
 */
@Singleton
class BankSmsCaptureScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Start (or refresh) periodic scanning and run one scan immediately. */
    fun enable() {
        val periodic = PeriodicWorkRequestBuilder<BankSmsScanWorker>(
            SCAN_INTERVAL_MINUTES, TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic,
        )
        scanNow()
    }

    /** Cancel all scanning work. */
    fun disable() {
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONESHOT_WORK)
    }

    /** Run a single scan now (used on enable and can be triggered manually). */
    fun scanNow() {
        val oneShot = OneTimeWorkRequestBuilder<BankSmsScanWorker>().build()
        workManager.enqueueUniqueWork(ONESHOT_WORK, ExistingWorkPolicy.REPLACE, oneShot)
    }

    private companion object {
        const val PERIODIC_WORK = "bank-sms-scan-periodic"
        const val ONESHOT_WORK = "bank-sms-scan-oneshot"
        const val SCAN_INTERVAL_MINUTES = 30L
    }
}
