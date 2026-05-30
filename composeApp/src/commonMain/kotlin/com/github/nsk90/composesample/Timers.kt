package com.github.nsk90.composesample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

fun CoroutineScope.singleShotTimer(timeoutMillis: Duration, block: suspend () -> Unit) = launch {
    delay(timeoutMillis)
    block()
}

fun tickerFlow(delayMillis: Duration) = flow {
    while (true) {
        emit(Unit)
        delay(delayMillis)
    }
}