# KMP ViewModel Sharing Analysis

> Status: draft (2026-04-15)
> Goal: answer the team question: "If KMP can share ViewModels, why are native ViewModels the proposed default?"

## Short Answer

Yes, a shared KMP ViewModel is technically possible.

AndroidX Lifecycle 2.8+ exposes `ViewModel`, `ViewModelStore`, `ViewModelStoreOwner`, and `ViewModelProvider` as Kotlin Multiplatform APIs. Compose Multiplatform can also use lifecycle/ViewModel APIs from common code.

Proposed Packeta v2 default:

> Share data/domain, use cases, repositories, and facades. Keep ViewModels native by default.

Consider a shared ViewModel only for a specific feature after a spike.

## What "Shared ViewModel" Can Mean

The term often mixes three different options:

| Option | Shared Part | Note |
|---|---|---|
| Shared AndroidX/KMP ViewModel | `class TrackingViewModel : ViewModel` in `commonMain` | Technically possible, but SwiftUI must consume Kotlin state/event APIs. |
| Shared state holder / presenter | Pure Kotlin class with `StateFlow`, no AndroidX `ViewModel` | Less lifecycle coupling, but platforms still need wrappers. |
| Native ViewModel + shared facade | Android/iOS ViewModels are native; shared code exposes use cases/facades | Proposed Packeta v2 default. |

The important boundary change: sharing a ViewModel moves KMP from business logic into presentation state.

## Comparison

| Criterion | Native ViewModels | Shared KMP ViewModels |
|---|---|---|
| Native UX | Android and iOS use natural platform patterns. | SwiftUI may adapt to Kotlin state conventions. |
| Shared logic | Data/domain/use cases are shared; screen orchestration is written per platform. | Screen state, event handling, and part of orchestration are shared. |
| SwiftUI ergonomics | Good: `@Observable`, `@MainActor`, `NavigationStack`, Swift wrappers. | Harder without wrappers: `StateFlow`, sealed classes, generics, cancellation bridging. |
| Android ergonomics | Good: AndroidX ViewModel, `viewModelScope`, Compose idioms. | Good if AndroidX Lifecycle KMP is used carefully. |
| Lifecycle | Each platform owns lifecycle natively. | One model must fit Android config changes and SwiftUI ownership/scoping. |
| Navigation | Platform navigation stays platform-native. | Shared VM may emit navigation intents that do not fit both platforms. |
| SDK/permission flows | Works well for maps, BLE, PayU, Apple Pay, Google Pay, push. | Often needs platform-specific escape hatches. |
| Testing | Some duplicated VM tests, but platform behavior is explicit. | More common tests, but more bridge/lifecycle tests. |
| Build complexity | Lower; shared modules stay outside presentation dependencies. | Higher; lifecycle/UI-aware dependencies affect iOS framework build. |
| Debugging | Platform bugs usually stay on one platform. | Shared VM bugs can affect both platforms and cross the bridge. |
| Blast radius | UI-state changes are less likely to break both platforms. | State-contract changes can break Android and iOS together. |

## When to Use a Shared ViewModel

A shared ViewModel can be reasonable if most of these are true:

- the UI flow is almost identical on Android and iOS,
- the state machine is more complex than the UI rendering,
- navigation is simple and identical,
- the feature has no heavy platform SDK, permissions, payment, or BLE flow,
- the iOS team accepts Kotlin-facing state APIs,
- the spike proves SwiftUI code remains readable and testable.

Possible candidates:

- simple onboarding step,
- tracking detail without complex platform navigation,
- settings screen with identical form state,
- pure business state machine with no UI dependencies.

## When Native ViewModels Are the Better Default

Native ViewModels are the better default when the feature includes:

- SwiftUI-specific or Compose-specific UX,
- platform navigation stack / sheet / deep-link behavior,
- permission flow,
- Apple Pay / Google Pay / PayU,
- BLE / CoreBluetooth / Android BluetoothGatt,
- maps, clustering, location permission UX,
- notification permission and notification tap routing,
- different accessibility or Dynamic Type behavior.

## Recommendation for Packeta v2

Default:

- Android: native Compose ViewModel.
- iOS: native SwiftUI ViewModel.
- Shared: use cases, repositories, DTO mapping, error mapping, cache, sync, feature facades.

Allowed compromise:

1. Start with a pure shared `StateMachine` / `Reducer`.
2. Wrap it in native platform ViewModels.
3. Consider shared `ViewModel` only if the spike proves real value without hurting SwiftUI ergonomics.

This keeps the presentation boundary explicit while still allowing selective sharing.

## Recommended Spike

Choose one small feature and implement two versions:

1. Native ViewModel + shared facade.
2. Shared KMP ViewModel.

Measure:

- amount of duplicated code,
- SwiftUI readability,
- cancellation/lifecycle behavior,
- testability of state/event flow,
- debug stack traces,
- iOS build stability,
- impact of state-contract changes on Swift ergonomics.

## External References

- AndroidX Lifecycle release notes: <https://developer.android.com/jetpack/androidx/releases/lifecycle>
- JetBrains KMP lifecycle docs: <https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-lifecycle.html>
- Kotlin Multiplatform platform-specific UI behavior: <https://kotlinlang.org/docs/multiplatform/compose-platform-specifics.html>
