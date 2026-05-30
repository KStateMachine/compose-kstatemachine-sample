package com.github.nsk90.composesample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.github.nsk90.composesample.ControlEvent.*
import com.github.nsk90.composesample.HeroState.*
import kstatemachine_compose_sample.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private enum class LogTag { STATE, TRANSITION, EVENT }
private data class LogEntry(val tag: LogTag, val text: String)

class StickManGameScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<StickManGameScreenModel>()
        StickManGameScreenContent(viewModel)
    }
}

private fun heroDrawable(states: List<HeroState>): DrawableResource = when {
    states.hasState<Shooting>() && states.hasState<Standing>()     -> Res.drawable.standing_shooting
    states.hasState<Shooting>() && states.hasState<AirAttacking>() -> Res.drawable.airattacking_shooting
    states.hasState<Shooting>() && states.hasState<Ducking>()      -> Res.drawable.ducking_shooting
    states.hasState<Shooting>() && states.hasState<Jumping>()      -> Res.drawable.jumping_shooting
    states.hasState<Standing>()     -> Res.drawable.standing
    states.hasState<AirAttacking>() -> Res.drawable.airattacking
    states.hasState<Ducking>()      -> Res.drawable.ducking
    states.hasState<Jumping>()      -> Res.drawable.jumping
    else                            -> Res.drawable.standing
}

@Composable
fun StickManGameScreenContent(viewModel: StickManGameScreenModel) {
    val uiState by viewModel.model.stateFlow.collectAsState()
    val logEntries = remember { mutableStateListOf<LogEntry>() }
    val heroDrawableRes = remember(uiState.activeStates) { heroDrawable(uiState.activeStates) }

    val duckInteractionSource = remember { MutableInteractionSource() }
    val isDuckPressed by duckInteractionSource.collectIsPressedAsState()
    val fireInteractionSource = remember { MutableInteractionSource() }
    val isFirePressed by fireInteractionSource.collectIsPressedAsState()

    LaunchedEffect(Unit) {
        viewModel.model.effectFlow.collect { effect ->
            val entry = when (effect) {
                is ModelEffect.StateEntered ->
                    LogEntry(LogTag.STATE, "→ ${effect.state::class.simpleName}")
                is ModelEffect.TransitionTriggered ->
                    LogEntry(LogTag.TRANSITION, "⇒ ${effect.name ?: "<unnamed>"}")
                is ModelEffect.ControlEventSent ->
                    LogEntry(LogTag.EVENT, "· ${effect.event::class.simpleName}")
                ModelEffect.AmmoDecremented -> return@collect
            }
            logEntries.add(0, entry)
            if (logEntries.size > 40) logEntries.removeAt(logEntries.lastIndex)
        }
    }

    LaunchedEffect(isDuckPressed) {
        viewModel.sendEvent(if (isDuckPressed) DuckPressEvent else DuckReleaseEvent)
    }
    LaunchedEffect(isFirePressed) {
        viewModel.sendEvent(if (isFirePressed) FirePressEvent else FireReleaseEvent)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (maxWidth > maxHeight) {
                // ── Landscape: hero on left, log on right ────────────────────────
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        AmmoHeader(uiState)
                        Spacer(Modifier.height(6.dp))
                        AmmoBar(uiState)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(heroDrawableRes),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ControlButtonRow(duckInteractionSource, fireInteractionSource, viewModel)
                    }
                    EventLog(
                        entries = logEntries,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                // ── Portrait: stacked ────────────────────────────────────────────
                Column(modifier = Modifier.fillMaxSize()) {
                    AmmoHeader(uiState)
                    Spacer(Modifier.height(6.dp))
                    AmmoBar(uiState)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(heroDrawableRes),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    EventLog(
                        entries = logEntries,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Spacer(Modifier.height(12.dp))
                    ControlButtonRow(duckInteractionSource, fireInteractionSource, viewModel)
                }
            }
        }
    }
}

@Composable
private fun AmmoHeader(uiState: ModelData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AMMO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
            )
            Text(
                text = "${uiState.ammoLeft}",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            uiState.activeStates.forEach { state ->
                Surface(
                    modifier = Modifier.width(105.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = state::class.simpleName ?: "",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AmmoBar(uiState: ModelData) {
    val ammoFraction = uiState.ammoLeft.toFloat() / INITIAL_AMMO.toFloat()
    LinearProgressIndicator(
        progress = ammoFraction,
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = when {
            ammoFraction > 0.5f -> Color(0xFF4CAF50)
            ammoFraction > 0.2f -> Color(0xFFFF9800)
            else                -> Color(0xFFF44336)
        },
        backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.10f)
    )
}

@Composable
private fun EventLog(entries: List<LogEntry>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(entries) { entry ->
                Text(
                    text = entry.text,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = when (entry.tag) {
                        LogTag.STATE      -> Color(0xFF66BB6A)
                        LogTag.TRANSITION -> Color(0xFF42A5F5)
                        LogTag.EVENT      -> Color(0xFFFFA726)
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlButtonRow(
    duckInteractionSource: MutableInteractionSource,
    fireInteractionSource: MutableInteractionSource,
    viewModel: StickManGameScreenModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {},
            interactionSource = duckInteractionSource,
            modifier = Modifier.weight(1f)
        ) { Text("Duck", fontSize = 13.sp) }

        Button(
            onClick = { viewModel.sendEvent(JumpPressEvent) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary
            )
        ) { Text("Jump", fontSize = 13.sp) }

        Button(
            onClick = {},
            interactionSource = fireInteractionSource,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFB71C1C),
                contentColor = Color.White
            )
        ) { Text("Fire", fontSize = 13.sp) }

        OutlinedButton(
            onClick = { viewModel.reloadAmmo() },
            modifier = Modifier.weight(1f)
        ) { Text("Reload", fontSize = 13.sp) }
    }
}
