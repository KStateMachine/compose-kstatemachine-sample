package com.stickman

import kotlinx.coroutines.Job
import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DefaultState


sealed interface ControlEvent : Event {
    object JumpPressEvent : ControlEvent
    object JumpCompleteEvent : ControlEvent
    object DuckPressEvent : ControlEvent
    object DuckReleaseEvent : ControlEvent
    object FirePressEvent : ControlEvent
    object FireReleaseEvent : ControlEvent
}
object OutOfAmmoEvent : ControlEvent

sealed class HeroState : DefaultState() {
    object Standing : HeroState()
    object Jumping : HeroState()
    object Ducking : HeroState()
    class AirAttacking : HeroState()
    {
        var isDuckPressed: Boolean = true
    }
    object NotShooting : HeroState()
    class Shooting : HeroState()
    {
        lateinit var shootingTimer: Job
    }
}

public inline fun <reified S : HeroState> List<HeroState>.hasState(): Boolean {
    return this.filterIsInstance<S>().isNotEmpty()
}