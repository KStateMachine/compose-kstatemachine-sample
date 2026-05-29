package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import kstatemachine_compose_sample.composeapp.generated.resources.*
import org.example.project.ControlEvent.*
import org.example.project.HeroState.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

class StickManGameScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<StickManGameScreenModel>()
        StickManGameScreenContent(viewModel)
    }
}

private fun heroDrawable(states: List<HeroState>): DrawableResource = when {
    states.hasState<Shooting>() && states.hasState<Standing>() -> Res.drawable.standing_shooting
    states.hasState<Shooting>() && states.hasState<AirAttacking>() -> Res.drawable.airattacking_shooting
    states.hasState<Shooting>() && states.hasState<Ducking>() -> Res.drawable.ducking_shooting
    states.hasState<Shooting>() && states.hasState<Jumping>() -> Res.drawable.jumping_shooting
    states.hasState<Standing>() -> Res.drawable.standing
    states.hasState<AirAttacking>() -> Res.drawable.airattacking
    states.hasState<Ducking>() -> Res.drawable.ducking
    states.hasState<Jumping>() -> Res.drawable.jumping
    else -> Res.drawable.standing
}

@Composable
fun StickManGameScreenContent(viewModel: StickManGameScreenModel) {
    val uiState by viewModel.model.stateFlow.collectAsState()
    val logMessages = remember { mutableStateListOf<String>() }

    val heroDrawableRes = remember(uiState.activeStates) { heroDrawable(uiState.activeStates) }
    val heroDrawable = painterResource(heroDrawableRes)

    val duckInteractionSource = remember { MutableInteractionSource() }
    val isDuckPressed by duckInteractionSource.collectIsPressedAsState()

    val fireInteractionSource = remember { MutableInteractionSource() }
    val isFirePressed by fireInteractionSource.collectIsPressedAsState()

    LaunchedEffect(Unit) {
        viewModel.model.effectFlow.collect { effect ->
            val msg = when (effect) {
                is ModelEffect.StateEntered -> "State Entered: ${effect.state::class.simpleName}"
                is ModelEffect.ControlEventSent -> "Control Event Sent: ${effect.event::class.simpleName}"
                is ModelEffect.TransitionTriggered -> "Transition: ${effect.name ?: "<unnamed>"}"
                ModelEffect.AmmoDecremented -> "Ammo Decremented"
            }
            logMessages.add(0, msg)
            if (logMessages.size > 30) logMessages.removeAt(logMessages.lastIndex)
        }
    }

    LaunchedEffect(isDuckPressed) {
        viewModel.sendEvent(if (isDuckPressed) DuckPressEvent else DuckReleaseEvent)
    }

    LaunchedEffect(isFirePressed) {
        viewModel.sendEvent(if (isFirePressed) FirePressEvent else FireReleaseEvent)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Ammo: ${uiState.ammoLeft}",
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = uiState.activeStates.joinToString(" | ") { it::class.simpleName!! },
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = heroDrawable,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Gray.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items(logMessages) { msg ->
                Text(text = msg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {}, interactionSource = duckInteractionSource) {
                Text(text = "Duck")
            }
            Button(onClick = { viewModel.sendEvent(JumpPressEvent) }) {
                Text(text = "Jump")
            }
            Button(onClick = {}, interactionSource = fireInteractionSource) {
                Text(text = "Fire")
            }
            Button(onClick = { viewModel.reloadAmmo() }) {
                Text(text = "Reload")
            }
        }
    }
}
