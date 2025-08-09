package org.example.project

const val JUMP_DURATION_MS = 1000L
const val INITIAL_AMMO = 40u
const val SHOOTING_INTERVAL_MS = 50L

data class ModelData(val ammoLeft: UInt, val activeStates: List<HeroState>)

sealed interface ModelEffect {
    data object AmmoDecremented : ModelEffect
    data class StateEntered(val state: HeroState) : ModelEffect
    data class ControlEventSent(val event: ControlEvent) : ModelEffect
}