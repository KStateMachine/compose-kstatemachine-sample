package com.sample.kstatemachine_compose_sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.stickman.hasState
import com.stickman.observe
//import io.kamel.core.Resource
//import io.kamel.image.asyncPainterResource
//import io.kamel.image.lazyPainterResource
import kotlinx.coroutines.flow.collectLatest
import kstatemachine_compose_sample.composeapp.generated.resources.Res
import kstatemachine_compose_sample.composeapp.generated.resources.airattacking
import kstatemachine_compose_sample.composeapp.generated.resources.airattacking_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.compose_multiplatform
import kstatemachine_compose_sample.composeapp.generated.resources.ducking
import kstatemachine_compose_sample.composeapp.generated.resources.ducking_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.jumping
import kstatemachine_compose_sample.composeapp.generated.resources.jumping_shooting
import kstatemachine_compose_sample.composeapp.generated.resources.standing
import kstatemachine_compose_sample.composeapp.generated.resources.standing_shooting
import org.example.project.Greeting
import org.example.project.StickManGameScreenModel
import org.example.project.XkcdClient
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
    onDrawableChange: (org.jetbrains.compose.resources.DrawableResource) -> Unit,  // Update to use DrawableResource
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
    val xkcdClient = remember { XkcdClient() }
    val coroutineScope = rememberCoroutineScope()
    var imageUrl by remember { mutableStateOf<String?>(null) }
    // Observe the state from the ViewModel
    val uiState by viewModel.model.stateFlow.collectAsState()
    var showContent by remember { mutableStateOf(false) }

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
            imageUrl = xkcdClient.getCurrentXkcdImageUrl()
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
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageUrl?.let { url ->
                        CoilImage(
                            modifier = Modifier.fillMaxWidth()
                            ,
                            imageModel = { imageUrl },
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
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clickable { showContent = !showContent }
        )
    }
}
