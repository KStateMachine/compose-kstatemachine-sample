package org.example.project

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import org.example.project.ControlEvent.DuckPressEvent
import org.example.project.ControlEvent.DuckReleaseEvent
import org.example.project.ControlEvent.FirePressEvent
import org.example.project.ControlEvent.FireReleaseEvent
import org.example.project.ControlEvent.JumpCompleteEvent
import org.example.project.ControlEvent.JumpPressEvent
import org.example.project.HeroState.AirAttacking
import org.example.project.HeroState.Ducking
import org.example.project.HeroState.Jumping
import org.example.project.HeroState.NotShooting
import org.example.project.HeroState.Shooting
import org.example.project.HeroState.Standing
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.state.ChildMode
import ru.nsk.kstatemachine.state.activeStates
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.addState
import ru.nsk.kstatemachine.state.invoke
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.onExit
import ru.nsk.kstatemachine.state.state
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.state.transitionOn
import ru.nsk.kstatemachine.statemachine.buildCreationArguments
import ru.nsk.kstatemachine.statemachine.createStateMachineBlocking
import ru.nsk.kstatemachine.statemachine.onStateEntry
import ru.nsk.kstatemachine.statemachine.onTransitionComplete
import ru.nsk.kstatemachine.transition.onTriggered

class StickManGameScreenModel : ScreenModel, MviModelHost<ModelData, ModelEffect> {
    override val model = MviModel<ModelData, ModelEffect>(
        screenModelScope, ModelData(
            INITIAL_AMMO, listOf(Standing)
        )
    )

    private val machine = createStateMachineBlocking(
        screenModelScope,
        "Hero",
        ChildMode.PARALLEL,
        creationArguments = buildCreationArguments { doNotThrowOnMultipleTransitionsMatch = true }
    ) {
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

            airAttacking {
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

        onTransitionComplete { activeStates, _ ->
            intent {
                val filteredStates = (activeStates as? Iterable<*>)?.filterIsInstance<HeroState>()
                if (filteredStates != null) {
                    state { copy(activeStates = filteredStates) }
                }
            }
        }
        onStateEntry { state, _ ->
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
