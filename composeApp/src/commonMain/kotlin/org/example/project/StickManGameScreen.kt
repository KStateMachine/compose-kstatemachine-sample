package com.sample.kstatemachine_compose_sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.*
import com.stickman.ControlEvent.*
import com.stickman.HeroState.*
import com.stickman.ControlEvent
import com.stickman.HeroState
import com.stickman.INITIAL_AMMO
import com.stickman.JUMP_DURATION_MS
import com.stickman.singleShotTimer
import com.stickman.tickerFlow
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.koin.getScreenModel
import co.touchlab.kermit.Logger
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import com.stickman.ModelData
import com.stickman.ModelEffect
import com.stickman.MviModel
import com.stickman.MviModelHost
import com.stickman.OutOfAmmoEvent
import com.stickman.SHOOTING_INTERVAL_MS
import com.stickman.MviModel.*
//import io.kamel.core.Resource
//import io.kamel.image.asyncPainterResource
//import io.kamel.image.lazyPainterResource
import kotlinx.coroutines.flow.collectLatest
import kstatemachine_compose_sample.composeapp.generated.resources.Res
import kstatemachine_compose_sample.composeapp.generated.resources.airattacking
import kstatemachine_compose_sample.composeapp.generated.resources.compose_multiplatform
import kstatemachine_compose_sample.composeapp.generated.resources.ducking
import kstatemachine_compose_sample.composeapp.generated.resources.jumping
import kstatemachine_compose_sample.composeapp.generated.resources.standing
import kstatemachine_compose_sample.composeapp.generated.resources.standing_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.stickman
import org.example.project.Greeting
import org.example.project.XkcdClient
import org.jetbrains.compose.resources.painterResource
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
    override val model = MviModel<ModelData, ModelEffect>(screenModelScope, ModelData(INITIAL_AMMO, listOf(Standing)))

    private val machine = createStateMachineBlocking(screenModelScope, "Hero", ChildMode.PARALLEL) {
//        logger = StateMachine.Logger { Log.d(this@StickManGameScreenModel::class.simpleName, it()) }
        val airAttacking = addState(AirAttacking())

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

        state("Movement") {

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


        onTransitionComplete { transitionParams, activeStates ->
//            Log.d("StickManGameScreenModel", buildString {
//                appendLine("Transition Complete")
//                appendLine("Event: ${transitionParams.toString()}")
////                appendLine("From State: ${transitionParams.transition().name}")
////                appendLine("To State: ${transitionParams.stream()}")
//                appendLine("Active States: ${activeStates().joinToString { it.name.toString() }}")
//            })
            intent {
                val filteredStates = (activeStates as? Iterable<*>)?.filterIsInstance<HeroState>()
                if (filteredStates != null) {
                    state { copy(activeStates = filteredStates) }
                }
            }
        }
        onStateEntry { state, transitionParams  ->
//            Log.d("StickManGameScreenModel", """
//            Entering State: ${state.name}
//            Previous State: ${transitionParams.transition.name}
//            Event Triggered: ${transitionParams.event}
//            Active States: ${this.activeStates().map { it.name }}
//            """.trimIndent())
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

class StickManGameScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<StickManGameScreenModel>()
        StickManGameScreenContent(viewModel)
    }
}

@Composable
fun StickManGameScreenContent(viewModel: StickManGameScreenModel) {
    val xkcdClient = remember { XkcdClient() }
    val coroutineScope = rememberCoroutineScope()
    var imageUrl by remember { mutableStateOf<String?>(null) }
    // Observe the state from the ViewModel
    val uiState by viewModel.model.stateFlow.collectAsState()
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            imageUrl = xkcdClient.getCurrentXkcdImageUrl()
        }
    }

    LaunchedEffect(uiState) {
        Logger.i {"State updated: $uiState"}
    }

    LaunchedEffect(viewModel.model.effectFlow) {
        viewModel.model.effectFlow.collectLatest { effect ->
            when (effect) {
                is ModelEffect.StateEntered -> {
                    Logger.i {"State Entered: ${effect.state}"}
                }
                is ModelEffect.ControlEventSent -> {
                    Logger.i {"Control Event Sent: ${effect.event}"}
                }
                is ModelEffect.AmmoDecremented -> {
                    Logger.i {"Ammo Decremented"}
                }
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
//        verticalArrangement = Arrangement.SpaceBetween
            verticalArrangement = Arrangement.spacedBy(16.dp), // Add spacing between items
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageUrl?.let { url ->
                        CoilImage(
                            modifier = Modifier.fillMaxWidth()//.size(200.dp),
                            ,
                            imageModel = { imageUrl }, // loading a network image or local resource using an URL.
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            )
                        )
                    } ?: CircularProgressIndicator()
                    Image(
                        painterResource(Res.drawable.compose_multiplatform),
                        null,
                        modifier = Modifier.size(100.dp)
                    )
                    Text("Compose: $greeting")
                }
            }
            // Ammo TextView
            Text(
                text = "Ammo: ${uiState.ammoLeft}", // Dynamically displaying the ammo count
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            val imageRes = when {
                uiState.activeStates.contains(Standing) -> Res.drawable.standing
                uiState.activeStates.contains(Ducking) -> Res.drawable.ducking
                uiState.activeStates.contains(Jumping) -> Res.drawable.jumping
                uiState.activeStates.contains(Shooting()) -> Res.drawable.standing_shooting
                uiState.activeStates.contains(AirAttacking()) -> Res.drawable.airattacking
                else -> Res.drawable.standing
            }
            Image(painterResource(imageRes), null)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    viewModel.sendEvent(DuckPressEvent)
                }
                )
                {
                    Text(text = "Duck")
                }
                Button(onClick = {
                    viewModel.sendEvent(JumpPressEvent)
                }) {
                    Text(text = "Jump")
                }
                Button(onClick = {
                    viewModel.sendEvent(FirePressEvent)
                }) {
                    Text(text = "Fire")
                }
                Button(onClick = { viewModel.reloadAmmo() }) {
                    Text(text = "Reload")
                }
            }
        }
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)  // Set the size of the image
                .align(Alignment.TopEnd) // Align the image to the top-right corner
                .padding(8.dp)  // Optional padding to avoid touching the screen edges
                .clickable { showContent = !showContent }
        )
    }
}
