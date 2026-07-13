package com.sylo.core.common.dispatchers

import javax.inject.Qualifier

/**
 * Qualifiers so injected [kotlinx.coroutines.CoroutineDispatcher]s can be swapped
 * for test dispatchers, and so call sites declare intent (IO vs Default).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: SyloDispatcher)

enum class SyloDispatcher { Default, IO }
