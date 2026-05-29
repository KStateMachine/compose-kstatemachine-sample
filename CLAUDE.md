# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Kotlin Multiplatform (Android + iOS) sample app demonstrating [KStateMachine](https://github.com/kstatemachine/kstatemachine) in a Compose UI context. It simulates a 2D hero character with states (Standing, Jumping, Ducking, AirAttacking, Shooting) controlled via on-screen buttons.

## Build Commands

```bash
# Build all targets
./gradlew build

# Android debug APK
./gradlew :composeApp:assembleDebug

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest
```

iOS builds require Xcode — open `iosApp/iosApp.xcodeproj` or use `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.

## Architecture

The app uses **MVI** with **KStateMachine** for state logic, **Voyager** for navigation/screen models, and **Koin** for DI.

### Data flow

```
UI (StickManGameScreen)
  → sendEvent(ControlEvent) → StickManGameScreenModel
    → machine.processEvent(event)  [KStateMachine]
      → onTransitionComplete / onStateEntry callbacks
        → intent { state { ... } / sendEffect(...) }
          → MviModel.stateFlow / effectFlow
            → UI observes and redraws
```

### Key files

- **`StateControl.kt`** — defines all domain types: `ControlEvent` (sealed interface of input events) and `HeroState` (sealed class extending KStateMachine's `DefaultState`). `HeroState` instances are used directly as KStateMachine state nodes.

- **`StickmanGameScreenModel.kt`** — Voyager `ScreenModel` that constructs the KStateMachine with `createStateMachineBlocking`. The machine has `ChildMode.PARALLEL` at the root with two parallel regions: `"Movement"` (Standing/Jumping/Ducking/AirAttacking) and `"Fire"` (NotShooting/Shooting). Transition callbacks call `intent { }` to update `MviModel` state and emit effects.

- **`Mvi.kt`** — Generic MVI infrastructure. `MviModel<State, Effect>` holds a `StateFlow` for state and a `Channel`-backed flow for one-shot effects. `MviModelHost` provides `intent { }` (launches coroutine on model scope) and `state` shorthand. The `observe()` extension binds both flows to a `LifecycleOwner`.

- **`ModelConst.kt`** — Game constants (`JUMP_DURATION_MS`, `SHOOTING_INTERVAL_MS`, `INITIAL_AMMO`) and data types: `ModelData` (state snapshot) and `ModelEffect` (sealed interface of effects).

- **`Timers.kt`** — `singleShotTimer` and `tickerFlow` coroutine utilities used inside state machine `onEntry`/`onExit` blocks.

- **`StickManGameScreen.kt`** — Compose `Screen` (Voyager). Uses `MutableInteractionSource` to detect press/release for Duck and Fire buttons, which map to paired `ControlEvent`s. Hero sprite is selected by checking the active state list for combinations of movement + fire states via the `List<HeroState>.hasState<T>()` extension.

- **`App.kt`** — Root `@Composable` that wraps `StickManGameScreen` in a Voyager `Navigator`.

- **`KoinModule.kt`** (androidMain) — Koin module registering `StickManGameScreenModel` as a singleton.

### KStateMachine usage pattern

States are declared as `HeroState` subclass objects/classes and added with `addState`/`addInitialState`. Transitions are configured with `transition<EventType>` and `transitionOn<EventType>` (for conditional targets). The machine is polled via `machine.processEvent(event)` called from the screen model.
