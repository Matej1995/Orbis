# ADR 0003: Config Strategy for Shared KMP Modules with Android-KMP Plugin

## Status

**Proposed - Level A: recommended direction, pending team signoff** (2026-04-15)

## Context

Packeta v2 needs multiple environments:

- dev,
- stage,
- prod.

The Android app module can use product flavors/build types. Shared KMP library modules should not rely on Android flavors. The Android Kotlin Multiplatform Library plugin is effectively single-variant for shared libraries, so per-environment values must not be hardcoded in shared modules.

The shared data layer still needs environment-dependent values:

- API base URL,
- analytics flags,
- feature flag defaults,
- logging level,
- certificate/pinning policy,
- app version metadata.

## Decision

Use runtime config injection.

Define a shared config contract:

```kotlin
interface AppConfig {
    val environment: Environment
    val apiBaseUrl: String
    val appVersion: String
    val buildNumber: String
    val isDebug: Boolean
}

enum class Environment {
    Dev,
    Stage,
    Prod
}
```

Android implements the contract from `BuildConfig` / flavor values:

```kotlin
class AndroidAppConfig : AppConfig {
    override val environment = BuildConfig.ENVIRONMENT.toEnvironment()
    override val apiBaseUrl = BuildConfig.API_BASE_URL
    override val appVersion = BuildConfig.VERSION_NAME
    override val buildNumber = BuildConfig.VERSION_CODE.toString()
    override val isDebug = BuildConfig.DEBUG
}
```

iOS implements the same contract from `xcconfig` / `Info.plist` values:

```swift
final class IosAppConfig: AppConfig {
    let environment: Environment
    let apiBaseUrl: String
    let appVersion: String
    let buildNumber: String
    let isDebug: Bool
}
```

The app module wires `AppConfig` into the shared DI graph.

## Rules

- Shared modules do not read Android `BuildConfig` directly.
- Shared modules do not contain dev/stage/prod source sets.
- Environment-specific values enter shared code only through `AppConfig`.
- Secrets do not live in Git.
- Build-time constants in shared modules are allowed only for values that are not environment-specific.

## BuildKonfig Usage

Use BuildKonfig only for shared defaults that do not vary by environment, for example:

- API version,
- compile-time feature flag default,
- schema version.

Do not use BuildKonfig for API base URLs, credentials, or production/stage switching.

## Consequences

### Positive

- The shared module remains deterministic and testable.
- Android and iOS use the same config contract.
- Dev/stage/prod behavior is controlled by app-level build configuration.
- The approach works with KMP libraries that do not support Android flavors.

### Negative

- App startup must initialize config before shared services.
- Tests need explicit fake config.
- Misconfigured app-level values can affect shared code at runtime.

### Rejected Alternatives

| Alternative | Reason |
|---|---|
| Android flavors inside shared KMP modules | Not supported as a reliable shared-library strategy. |
| Hardcoded URLs in shared code | Unsafe and hard to test. |
| Separate shared module per environment | Increases build and dependency complexity. |
| Reading platform config from common code directly | Breaks common-code boundaries. |

## Implementation Checklist

- [ ] Create `:core:config`.
- [ ] Add `AppConfig` and `Environment`.
- [ ] Implement `AndroidAppConfig`.
- [ ] Implement `IosAppConfig`.
- [ ] Inject `AppConfig` into Koin/shared DI.
- [ ] Add tests with fake config.
- [ ] Verify dev/stage/prod for Android and iOS.

## Open Questions

- [ ] Which values must be runtime config and which can be build-time constants?
- [ ] How will secrets be provided in CI?
- [ ] Is certificate pinning environment-specific?

## Related

- [ADR-0001: KMP shared data/domain](0001-kmp-shared-data-domain.md)
- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0012: Backend API contract](0012-backend-api-contract.md)
