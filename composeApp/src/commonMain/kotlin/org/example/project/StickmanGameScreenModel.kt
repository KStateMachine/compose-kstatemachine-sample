package org.example.project

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import com.stickman.ControlEvent
import com.stickman.ControlEvent.DuckPressEvent
import com.stickman.ControlEvent.DuckReleaseEvent
import com.stickman.ControlEvent.FirePressEvent
import com.stickman.ControlEvent.FireReleaseEvent
import com.stickman.ControlEvent.JumpCompleteEvent
import com.stickman.ControlEvent.JumpPressEvent
import com.stickman.HeroState
import com.stickman.HeroState.AirAttacking
import com.stickman.HeroState.Ducking
import com.stickman.HeroState.Jumping
import com.stickman.HeroState.NotShooting
import com.stickman.HeroState.Shooting
import com.stickman.HeroState.Standing
import com.stickman.INITIAL_AMMO
import com.stickman.JUMP_DURATION_MS
import com.stickman.ModelData
import com.stickman.ModelEffect
import com.stickman.MviModel
import com.stickman.MviModelHost
import com.stickman.OutOfAmmoEvent
import com.stickman.SHOOTING_INTERVAL_MS
import com.stickman.singleShotTimer
import com.stickman.tickerFlow
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.state.ChildMode
import ru.nsk.kstatemachine.state.activeStates
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.invoke
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.onExit
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.onStateEntry
import ru.nsk.kstatemachine.statemachine.onTransitionComplete
import ru.nsk.kstatemachine.transition.onTriggered

class StickManGameScreenModel : ScreenModel, MviModelHost<ModelData, ModelEffect> {
    override val model = MviModel<ModelData, ModelEffect>(screenModelScope, ModelData(
        INITIAL_AMMO, listOf(
            Standing
        ))
    )

    private val machine = createStateMachineBlocking(screenModelScope, "Hero", ChildMode.PARALLEL,creationArguments = StateMachine.CreationArguments(doNotThrowOnMultipleTransitionsMatch=true)) {
        logger = StateMachine.Logger {
            Logger.i {
                "${this@StickManGameScreenModel::class.simpleName}: ${it()}"
            }
        }

        state("Movement") {

            val airAttacking = addState(AirAttacking())
            addInitialState(Standing) {
                transition<JumpPressEvent>("Jump", targetState = Jumping)
                transition<DuckPressEvent>("Duck", targetState = Ducking)
            }

            addState(Jumping) {
                onEntry {
                    screenModelScope.singleShotTimer(JUMP_DURATION_MS) {
                        sendEvent(JumpCompleteEvent)
                    }
                }
                transition<DuckPressEvent>("AirAttack", targetState = airAttacking)
                transition<JumpCompleteEvent>("Land after jump", targetState = Standing)
            }

            addState(Ducking) {
                transition<DuckReleaseEvent>("StandUp", targetState = Standing)
            }

            airAttacking  {
                onEntry { isDuckPressed = true }

                transitionOn<JumpCompleteEvent>("Land after attack") {
                    targetState = { if (this@airAttacking.isDuckPressed) Ducking else Standing }
                }
                transition<DuckPressEvent>("Duck pressed") {
                    onTriggered { this@airAttacking.isDuckPressed = true }
                }
                transition<DuckReleaseEvent>("Duck released") {
                    onTriggered { this@airAttacking.isDuckPressed = false }
                }
            }
        }

        state("Fire") {
            val shooting = addState(Shooting())


            addInitialState(NotShooting) {
                transition<FirePressEvent> {
                    guard = { state.ammoLeft > 0u }
                    targetState = shooting
                }
            }
            shooting {
                transition<FireReleaseEvent>(targetState = NotShooting)
                transition<OutOfAmmoEvent>(targetState = NotShooting)

                onEntry {
                    shootingTimer = screenModelScope.launch {
                        tickerFlow(SHOOTING_INTERVAL_MS).collect {
                            if (state.ammoLeft == 0u)
                                sendEvent(OutOfAmmoEvent)
                            else
                                decrementAmmo()
                        }
                    }
                }
                onExit { shootingTimer.cancel() }
            }




        }


        onTransitionComplete { activeStates, transitionParams ->
            Logger.i {
                buildString {
                    appendLine("Transition Complete")
                    appendLine("Event: ${transitionParams.toString()}")
//                    appendLine("From State: ${transitionParams.transition().name}")
//                    appendLine("To State: ${transitionParams.stream()}")
                    appendLine("Active States: ${activeStates().joinToString { it.name.toString() }}")
                }
            }
            intent {
                val filteredStates = (activeStates as? Iterable<*>)?.filterIsInstance<HeroState>()
                if (filteredStates != null) {
                    state { copy(activeStates = filteredStates) }
                }
            }
        }
        onStateEntry { state, transitionParams  ->
            Logger.i { """
                Entering State: ${state.name}
                Previous State: ${transitionParams.transition.name}
                Event Triggered: ${transitionParams.event}
                Active States: ${this.activeStates().map { it.name }}
                """.trimIndent()
            }
            intent {
                if (state is HeroState)
                    sendEffect(ModelEffect.StateEntered(state))
            }
        }

    }

    fun sendEvent(event: ControlEvent): Unit = intent {
        sendEffect(ModelEffect.ControlEventSent(event))
        machine.processEvent(event)
    }

    fun reloadAmmo() = intent {
        state { copy(ammoLeft = INITIAL_AMMO) }
    }

    private fun decrementAmmo() = intent {
        state { copy(ammoLeft = ammoLeft - 1u) }
        sendEffect(ModelEffect.AmmoDecremented)
    }
}