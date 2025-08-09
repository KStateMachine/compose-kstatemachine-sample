package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.launch
import org.example.project.ControlEvent.*
import org.example.project.HeroState.*
import cafe.adriel.voyager.koin.getScreenModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.collectLatest
import kstatemachine_compose_sample.composeapp.generated.resources.Res
import kstatemachine_compose_sample.composeapp.generated.resources.airattacking
import kstatemachine_compose_sample.composeapp.generated.resources.airattacking_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.ducking
import kstatemachine_compose_sample.composeapp.generated.resources.ducking_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.jumping
import kstatemachine_compose_sample.composeapp.generated.resources.jumping_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.standing
import kstatemachine_compose_sample.composeapp.generated.resources.standing_shooting
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

class StickManGameScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<StickManGameScreenModel>()
        StickManGameScreenContent(viewModel)
    }
}

private fun onStateChanged(
    state: ModelData,
    onDrawableChange: (DrawableResource) -> Unit,  // Update to use DrawableResource
    onAmmoChange: (Int) -> Unit
) {
    state.activeStates.let {
        val drawableRes = when {
            it.hasState<Shooting>() && it.hasState<Standing>() -> Res.drawable.standing_shooting
            it.hasState<Shooting>() && it.hasState<AirAttacking>() -> Res.drawable.airattacking_shooting
            it.hasState<Shooting>() && it.hasState<Ducking>() -> Res.drawable.ducking_shooting
            it.hasState<Shooting>() && it.hasState<Jumping>() -> Res.drawable.jumping_shooting
            it.hasState<Standing>() -> Res.drawable.standing
            it.hasState<AirAttacking>() -> Res.drawable.airattacking
            it.hasState<Ducking>() -> Res.drawable.ducking
            it.hasState<Jumping>() -> Res.drawable.jumping
            else -> Res.drawable.standing // Default drawable if no specific state is found
        }
        onDrawableChange(drawableRes)
    }

    // Update ammo count state
    onAmmoChange(state.ammoLeft.toInt())
}

private fun onEffect(effect: ModelEffect) {
    when (effect) {
        ModelEffect.AmmoDecremented -> Logger.i { "*" }
        is ModelEffect.StateEntered -> Logger.i { effect.state::class.simpleName.toString() }
        is ModelEffect.ControlEventSent -> Logger.i { effect.event::class.simpleName.toString() }
    }
}

@Composable
fun StickManGameScreenContent(viewModel: StickManGameScreenModel) {
    val coroutineScope = rememberCoroutineScope()
    // Observe the state from the ViewModel
    val uiState by viewModel.model.stateFlow.collectAsState()

    // State variables for drawable and ammo count
    var heroDrawableRes by remember { mutableStateOf(Res.drawable.standing) }
    var ammoCount by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val duckInteractionSource = remember { MutableInteractionSource() }
    val isDuckPressed by duckInteractionSource.collectIsPressedAsState()

    val fireInteractionSource = remember { MutableInteractionSource() }
    val isFirePressed by fireInteractionSource.collectIsPressedAsState()
    val heroDrawable = painterResource(heroDrawableRes)

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            viewModel.observe(
                lifecycleOwner,
                { state ->
                    onStateChanged(
                        state,
                        onDrawableChange = { newDrawableRes  -> heroDrawableRes  = newDrawableRes  },
                        onAmmoChange = { newAmmo -> ammoCount = newAmmo }
                    )
                },
                ::onEffect
            )
        }
    }

    // Detect Duck Button State
    LaunchedEffect(isDuckPressed) {
        if (isDuckPressed) {
            viewModel.sendEvent(DuckPressEvent)
        } else {
            viewModel.sendEvent(DuckReleaseEvent)
        }
    }

    // Detect Fire Button State
    LaunchedEffect(isFirePressed) {
        if (isFirePressed) {
            viewModel.sendEvent(FirePressEvent)
        } else {
            viewModel.sendEvent(FireReleaseEvent)
        }
    }

    LaunchedEffect(uiState) {
        Logger.i { "State updated: $uiState" }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ammo: ${uiState.ammoLeft}",
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Image(
                painter = heroDrawable,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {},
                    interactionSource = duckInteractionSource,
                ) {
                    Text(text = "Duck")
                }
                Button(onClick = {
                    viewModel.sendEvent(JumpPressEvent)
                }) {
                    Text(text = "Jump")
                }
                Button(
                    onClick = {},
                    interactionSource = fireInteractionSource,
                ) {
                    Text(text = "Fire")
                }
                Button(onClick = { viewModel.reloadAmmo() }) {
                    Text(text = "Reload")
                }
            }
        }
    }
}
