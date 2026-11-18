# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Kotlin Multiplatform (Android + iOS) sample app demonstrating [KStateMachine](https://github.com/kstatemachine/kstatemachine) in a Compose UI context. It simulates a 2D hero character with states (Standing, Jumping, Ducking, AirAttacking, Shooting) controlled via on-screen buttons.

## Build Commands

```bash
# Build all targets
./gradlew build

# Android debug APK
./gradlew :androidApp:assembleDebug
```

iOS builds require Xcode — open `iosApp/iosApp.xcodeproj` or use `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.

## Architecture

The app uses **MVI** with **KStateMachine** for state logic and the multiplatform **Lifecycle ViewModel** (`org.jetbrains.androidx.lifecycle`). There is no DI framework and no navigation library — a single screen creates its ViewModel via `viewModel { }`.

### Modules

- **`:composeApp`** — KMP shared module (Android + iOS) with all app logic and UI. Uses AGP 9's `com.android.kotlin.multiplatform.library` plugin (`kotlin { android { } }` block, no legacy `android { }`). `androidResources { enable = true }` is required, otherwise Compose resources (`Res.drawable.*`) are not packaged into the APK. Produces the `ComposeApp` iOS framework.
- **`:androidApp`** — thin `com.android.application` module (`MainActivity`, manifest, launcher icons). Uses AGP 9 built-in Kotlin, so the `kotlin-android` plugin is not applied. AGP 9 forbids applying KMP and `com.android.application` in one module, which is why this split exists.

### Data flow

```
UI (StickManGameScreen)
  → sendEvent(ControlEvent) → StickManGameViewModel
    → machine.processEvent(event)  [KStateMachine]
      → onTransitionComplete / onStateEntry callbacks
        → intent { state { ... } / sendEffect(...) }
          → MviModel.stateFlow / effectFlow
            → UI observes and redraws
```

### Key files

- **`StateControl.kt`** — defines all domain types: `ControlEvent` (sealed interface of input events) and `HeroState` (sealed class extending KStateMachine's `DefaultState`). `HeroState` instances are used directly as KStateMachine state nodes.

- **`StickManGameViewModel.kt`** — `ViewModel` that constructs the KStateMachine with `createStateMachineBlocking`. The machine has `ChildMode.PARALLEL` at the root with two parallel regions: `"Movement"` (Standing/Jumping/Ducking/AirAttacking) and `"Fire"` (NotShooting/Shooting). Transition callbacks call `intent { }` to update `MviModel` state and emit effects.

- **`Mvi.kt`** — Generic MVI infrastructure. `MviModel<State, Effect>` holds a `StateFlow` for state and a `Channel`-backed flow for one-shot effects. `MviModelHost` provides `intent { }` (launches coroutine on model scope) and `state` shorthand.

- **`ModelConst.kt`** — Game constants (`JUMP_DURATION_MS`, `SHOOTING_INTERVAL_MS`, `INITIAL_AMMO`) and data types: `ModelData` (state snapshot) and `ModelEffect` (sealed interface of effects).

- **`Timers.kt`** — `singleShotTimer` and `tickerFlow` coroutine utilities used inside state machine `onEntry`/`onExit` blocks.

- **`StickManGameScreen.kt`** — `@Composable` screen (Material 3) that obtains its ViewModel with `viewModel { StickManGameViewModel() }`. Uses `MutableInteractionSource` to detect press/release for Duck and Fire buttons, which map to paired `ControlEvent`s. Hero sprite is selected by checking the active state list for combinations of movement + fire states via the `List<HeroState>.hasState<T>()` extension.

- **`App.kt`** — Root `@Composable`: Material 3 theme around `StickManGameScreen`.

- **`MainActivity.kt`** (`androidApp`) — enables edge-to-edge and sets `App()` as content.

### KStateMachine usage pattern

States are declared as `HeroState` subclass objects/classes and added with `addState`/`addInitialState`. Transitions are configured with `transition<EventType>` and `transitionOn<EventType>` (for conditional targets). The machine is polled via `machine.processEvent(event)` called from the ViewModel.

`buildCreationArguments { doNotThrowOnMultipleTransitionsMatch = true }` is set because both parallel regions can match the same event simultaneously.

### Important design decisions

**`class` vs `object` for states:** `AirAttacking` and `Shooting` are `class` (not `object`) because they hold mutable instance state — `isDuckPressed: Boolean` and `shootingTimer: Job` respectively. All other `HeroState` subclasses are `object` singletons.

**`activeStates` always has exactly 2 entries:** Because the machine root uses `ChildMode.PARALLEL`, `ModelData.activeStates` always contains one state from the `"Movement"` region and one from the `"Fire"` region. The UI's `heroDrawable()` function checks combinations of both to pick the correct sprite.
