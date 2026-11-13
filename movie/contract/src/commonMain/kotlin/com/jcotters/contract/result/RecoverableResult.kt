package com.jcotters.contract.result

import kotlinx.coroutines.CancellationException

/** Captures recoverable operation failures; cancellation and fatal errors always escape. */
inline fun <T> recoverableResult(block: () -> T): Result<T> = runCatching(block).onFailure { error ->
    if (error is CancellationException || error !is Exception) throw error
}
