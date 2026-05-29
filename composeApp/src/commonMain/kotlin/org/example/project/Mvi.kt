package org.example.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MviModel<State, Effect>(val scope: CoroutineScope, initialState: State) {
    private val _stateFlow = MutableStateFlow(initialState)
    val stateFlow = _stateFlow.asStateFlow()

    private val _effectChannel = Channel<Effect>()
    val effectFlow = _effectChannel.receiveAsFlow()

    suspend fun sendEffect(effect: Effect) = _effectChannel.send(effect)

    fun state(block: State.() -> State) {
        _stateFlow.value = _stateFlow.value.block()
    }
}

interface MviModelHost<State, Effect> {
    val model: MviModel<State, Effect>

    fun intent(context: CoroutineContext = EmptyCoroutineContext, block: suspend MviModel<State, Effect>.() -> Unit) {
        model.scope.launch(context) { model.block() }
    }

    val state: State get() = model.stateFlow.value
}
