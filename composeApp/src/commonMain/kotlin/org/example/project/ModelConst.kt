package com.stickman

import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.FinalState

// 0.26.0
//import ru.nsk.kstatemachine.DefaultState
//import ru.nsk.kstatemachine.FinalState

//import ru.nsk.kstatemachine.state.*
//import ru.nsk.kstatemachine.state.DefaultState
//import ru.nsk.kstatemachine.state.FinalState

//import ru.nsk.kstatemachine.state.DefaultState
//import ru.nsk.kstatemachine.state.FinalState

const val JUMP_DURATION_MS = 1000L
const val INITIAL_AMMO = 40u
const val SHOOTING_INTERVAL_MS = 50L

data class ModelData(val ammoLeft: UInt, val activeStates: List<HeroState>)

sealed interface ModelEffect {
    object AmmoDecremented : ModelEffect
    class StateEntered(val state: HeroState) : ModelEffect
    class ControlEventSent(val event: ControlEvent) : ModelEffect
}
//// Define your States as classes or objects
//sealed class StickManStates : DefaultState() {
//    object StandingState : StickManStates()
//    object DuckingState : StickManStates()
//    object JumpingState : StickManStates()
//    object FiringState : StickManStates()
//    object ReloadingState : StickManStates(), FinalState
//}