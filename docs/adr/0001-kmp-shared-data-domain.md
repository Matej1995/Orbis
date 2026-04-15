# ADR 0001: KMP for Shared Data and Domain Layers

## Status

**Proposed - Level A: recommended direction, pending team signoff** (2026-04-15)

## Context

Packeta v2 is a rewrite of the existing production app. The backend is expected to remain the same, so Android and iOS will integrate with the same API.

The cross-platform strategy affects:

- team productivity,
- UX consistency,
- long-term maintenance,
- performance and native feel,
- hiring and onboarding.

Considered options:

| Option | Pros | Cons |
|---|---|---|
| Kotlin Multiplatform with shared data/domain and native UI | Native UX, shared business logic, gradual adoption | Kotlin/Swift bridge, build complexity |
| Compose Multiplatform shared UI | Highest code sharing | iOS UI is not Apple-native; higher product risk for a large app |
| Flutter | Mature shared UI | Dart stack, separate ecosystem, no existing project fit |
| React Native | Large JS ecosystem | Bridge cost and friction with BLE/payments/native SDKs |
| Duplicate native apps | Full native control | Duplicate business logic, duplicate bugs, platform drift |

## Decision

Use Kotlin Multiplatform for the shared data and domain layers. Keep UI native on each platform.

Shared:

- `core:domain`: pure Kotlin types, result/error types, use cases, repository interfaces.
- `core:data`: Ktor, Room, repository implementations, DTOs, mappers.
- `feature:*:domain` and `feature:*:data`: per-feature shared modules.
- `core:resources`: typed semantic keys / enum mapping only, no UI resource runtime by default. See [ADR-0013](0013-assets-localization-strategy.md).

Not shared:

- UI: Jetpack Compose on Android, SwiftUI on iOS.
- ViewModels: native by default. See [ADR-0002](0002-native-ui-per-platform.md).
- Navigation: Android Navigation / SwiftUI NavigationStack.
- Platform-specific integrations: BLE APIs, maps, payment SDKs, push, scanner.

## Consequences

### Positive

- Business logic is implemented once: validation, state transitions, error mapping.
- API integration is shared, reducing Android/iOS interpretation drift.
- UX remains native on both platforms.
- KMP can be adopted gradually by feature.
- Room KMP, Ktor, and the KMP ecosystem are mature enough for this scope.

### Negative

- Kotlin/Swift interop requires a deliberate API boundary. See [ADR-0004](0004-kmp-swift-api-contract.md).
- iOS developers need to read and debug some Kotlin.
- Gradle + Xcode build integration adds complexity. See [ADR-0003](0003-android-kmp-plugin-single-variant.md).
- Debugging shared code from iOS is less direct than native Swift-only code.

### Neutral

- Tooling remains split between Android Studio/IntelliJ and Xcode.
- Hiring remains possible, but onboarding must cover KMP basics for both platforms.

## Open Questions

- [ ] Confirm with the iOS team that the facade approach is ergonomic after a KMP/Swift API spike.
- [ ] Confirm with the backend team that API contract/OpenAPI coverage is sufficient for the shared data layer.

## Related

- [ADR-0002: Native UI per platform](0002-native-ui-per-platform.md)
- [ADR-0003: Android-KMP plugin single-variant](0003-android-kmp-plugin-single-variant.md)
- [ADR-0004: KMP/Swift API contract](0004-kmp-swift-api-contract.md)
- [ADR-0013: Assets, resources, and localization strategy](0013-assets-localization-strategy.md)
