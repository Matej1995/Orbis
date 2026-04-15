# ADR 0004: KMP to Swift Public API Contract

## Status

**Proposed - Level A/B: facade direction, bridge tooling spike required** (2026-04-15)

## Context

iOS will consume shared KMP code. Raw Kotlin APIs are not automatically good Swift APIs. The exported API must be stable, small, and designed for Swift usage.

Interop risks:

- `Flow` and suspend functions need Swift-friendly bridging.
- Kotlin sealed classes/enums/generics can be awkward in Swift.
- Internal use cases can expose too much implementation detail.
- Binary compatibility matters once an XCFramework is distributed.
- Debugging across the Kotlin/Swift boundary is harder than native-only debugging.

## Decision

Each feature exposes a public KMP facade designed for iOS consumers.

Example:

```kotlin
class TrackingFacade(
    private val getPackage: GetPackageUseCase,
    private val observePackages: ObservePackagesUseCase
) {
    suspend fun getPackage(id: String): OperationResult<Package, TrackingError> =
        getPackage.invoke(id)

    fun observePackages(): Flow<List<Package>> =
        observePackages.invoke()
}
```

iOS calls the facade, not internal use cases, repository interfaces, or data implementations.

Use a bridge tool after a spike:

- preferred candidate: SKIE,
- alternative: KMP-NativeCoroutines.

## Public API Rules

- Expose per-feature facades, not one large `AppFacade`.
- Keep public shared APIs small and stable.
- Do not expose internal repository implementations to Swift.
- Prefer explicit result/error models over throwing arbitrary exceptions.
- Model expected failures as typed domain errors.
- Convert platform-specific concerns at the platform boundary.
- Avoid leaking low-level DTOs into Swift UI.

## Error and Result Contract

Use a shared result type:

```kotlin
sealed interface OperationResult<out D, out E> {
    data class Success<D>(val data: D) : OperationResult<D, Nothing>
    data class Failure<E>(val error: E) : OperationResult<Nothing, E>
}
```

Use typed errors:

```kotlin
sealed interface AppError

sealed interface NetworkError : AppError {
    data object NoInternet : NetworkError
    data object Timeout : NetworkError
    data class Server(val code: String) : NetworkError
}
```

Swift should receive errors that can be mapped to user-facing copy and UI state.

## Async and Flow Contract

Suspending functions are valid for one-shot operations:

- login,
- refresh,
- submit OTP,
- fetch detail.

Flows are valid for observable state:

- cached packages,
- sync status,
- feature flags.

The selected bridge must make cancellation, main-thread delivery, and memory ownership explicit enough for SwiftUI.

## Consequences

### Positive

- iOS gets a stable, documented API surface.
- Kotlin internals can change without rewriting Swift screens.
- API review is possible per feature.
- The same facade can be used by tests and mock/demo flows.

### Negative

- Facades add one layer of code.
- Some Android code may use use cases directly while iOS uses facades, unless Android also adopts the facade surface.
- Bridge tooling must be validated in CI and iOS builds.

### Neutral

- A facade is not a UI ViewModel. It is a public API boundary.
- A facade can aggregate multiple use cases when that makes Swift usage clearer.

## Implementation Checklist

- [ ] Create `OperationResult`.
- [ ] Create shared `AppError` hierarchy.
- [ ] Create one `XFeatureFacade` per feature.
- [ ] Run SKIE vs KMP-NativeCoroutines spike.
- [ ] Add API/binary compatibility checks for iOS-exported API.
- [ ] Document the public facade API in the template README.

## Open Questions

- [ ] Per-feature facades vs one large `AppFacade` -> proposed default: per-feature.
- [ ] How will iOS obtain facade instances from DI?
- [ ] Which bridge tool will the iOS team accept after the spike?

## Related

- [ADR-0001: KMP shared data/domain](0001-kmp-shared-data-domain.md)
- [ADR-0002: Native UI per platform](0002-native-ui-per-platform.md)
- [SKIE docs](https://skie.touchlab.co/)
