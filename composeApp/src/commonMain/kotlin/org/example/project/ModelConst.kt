package org.example.project

import kotlin.time.Duration.Companion.milliseconds

val JUMP_DURATION_MS = 1000.milliseconds
const val INITIAL_AMMO = 40u
val SHOOTING_INTERVAL_MS = 50.milliseconds

data class ModelData(val ammoLeft: UInt, val activeStates: List<HeroState>)

sealed interface ModelEffect {
    data object AmmoDecremented : ModelEffect
    data class StateEntered(val state: HeroState) : ModelEffect
    data class ControlEventSent(val event: ControlEvent) : ModelEffect
    data class TransitionTriggered(val name: String?) : ModelEffect
}