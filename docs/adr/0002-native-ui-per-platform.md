# ADR 0002: Native UI per Platform

## Status

**Proposed - Level A: recommended direction, pending team signoff** (2026-04-15)

## Context

[ADR-0001](0001-kmp-shared-data-domain.md) chooses KMP for shared data/domain. The remaining question is the presentation layer.

Packeta v2 needs strong native behavior for:

- platform navigation,
- permissions,
- maps and location,
- payments,
- BLE Z-Box interaction,
- push permissions and notification routing,
- accessibility and dynamic text.

Shared UI is technically possible, but it increases risk in the most platform-sensitive part of the app.

## Decision

Use native presentation on each platform:

- Android: Jetpack Compose.
- iOS: SwiftUI.

Use native ViewModels by default:

- Android: AndroidX ViewModel.
- iOS: Swift `@Observable` / `ObservableObject` / `@MainActor` patterns.

KMP shared code provides use cases, repositories, domain models, errors, and per-feature facades. It does not own screens, navigation, or default ViewModels.

Shared ViewModels remain possible for narrow flows after a spike, but they are not the default architecture.

## Rationale

### Why Not Compose Multiplatform for Presentation

- iOS UX should follow Apple-native patterns.
- Complex platform SDKs still need native integration.
- Accessibility, dynamic type, navigation, and sheet behavior are platform-sensitive.
- Large-app risk is higher when the whole presentation layer depends on a shared UI runtime.

### Why Not Shared ViewModels by Default

- A shared ViewModel moves the KMP boundary into presentation state.
- SwiftUI then consumes Kotlin state/event/cancellation APIs.
- Platform navigation and permissions often leak back into shared state.
- A shared state contract can break both platforms at once.

See [KMP ViewModel sharing analysis](../kmp-viewmodel-sharing-analysis.md).

## Consequences

### Positive

- Android and iOS keep idiomatic UI and lifecycle patterns.
- Platform SDK flows stay close to the platform.
- The shared layer stays focused on business logic and data.
- iOS API surface can be designed intentionally through facades.

### Negative

- Screen orchestration and ViewModel code are written twice.
- Design parity requires discipline, design tokens, and review.
- Some UI state machines may be duplicated unless explicitly extracted.

### Neutral

- Shared pure Kotlin reducers/state machines can still be used when they have clear value.
- A shared ViewModel can be introduced for a specific feature through a separate ADR after a spike.

## Implementation Notes

Android ViewModel:

```kotlin
class TrackingViewModel(
    private val getPackage: GetPackageUseCase
) : ViewModel() {
    // Compose-facing state and events stay Android-native.
}
```

iOS ViewModel:

```swift
@MainActor
@Observable
final class TrackingViewModel {
    private let facade: TrackingFacade

    init(facade: TrackingFacade) {
        self.facade = facade
    }
}
```

For each feature, expose a Swift-friendly facade from shared code. iOS should not call internal use cases directly.

## Open Questions

- [ ] Which feature, if any, should be used for a shared ViewModel spike?
- [ ] Which iOS observation pattern is the default for minimum supported iOS versions?

## Related

- [ADR-0001: KMP shared data/domain](0001-kmp-shared-data-domain.md)
- [ADR-0004: KMP/Swift API contract](0004-kmp-swift-api-contract.md)
- [ADR-0011: Design system parity](0011-design-system-parity.md)
- [KMP ViewModel sharing analysis](../kmp-viewmodel-sharing-analysis.md)
