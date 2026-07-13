package com.sylo.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * A minimal, allocation-light result wrapper used across every layer instead of
 * throwing across module boundaries. Domain functions return [SyloResult] so that
 * the UI can render loading / success / error uniformly.
 */
sealed interface SyloResult<out T> {
    data class Success<T>(val data: T) : SyloResult<T>
    data class Error(val throwable: Throwable) : SyloResult<Nothing>
    data object Loading : SyloResult<Nothing>
}

inline fun <T, R> SyloResult<T>.map(transform: (T) -> R): SyloResult<R> = when (this) {
    is SyloResult.Success -> SyloResult.Success(transform(data))
    is SyloResult.Error -> this
    SyloResult.Loading -> SyloResult.Loading
}

/** Wraps a [Flow] so emissions become [SyloResult.Success] and errors are captured. */
fun <T> Flow<T>.asResult(): Flow<SyloResult<T>> = this
    .map<T, SyloResult<T>> { SyloResult.Success(it) }
    .onStart { emit(SyloResult.Loading) }
    .catch { emit(SyloResult.Error(it)) }
