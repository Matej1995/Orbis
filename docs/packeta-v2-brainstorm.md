# Packeta v2.0 - KMP Technical Analysis

> Status: draft (2026-04-15)
> Author: Matej Sandera
> Goal: document technical decisions for a new Packeta v2 mobile app, separate signoff/spike/input items, and prepare an Orbis-based project template.

## Reading Map

This document contains the full technical analysis, decision levels, and input backlog. Recommended entry points:

1. [README.md](README.md)
2. [packeta-v2-status-overview.md](packeta-v2-status-overview.md)
3. [packeta-v2-presentation-brief.md](packeta-v2-presentation-brief.md)
4. [architecture-diagrams.md](architecture-diagrams.md)
5. Priority ADRs: 0001, 0002, 0003, 0004, 0012, 0013.

Deep decisions live in ADRs:

| # | ADR | Decision Level | Topic |
|---|---|---|---|
| [0001](adr/0001-kmp-shared-data-domain.md) | KMP shared data/domain | Level A - signoff | What is shared and why |
| [0002](adr/0002-native-ui-per-platform.md) | Native UI per platform | Level A - signoff | Compose + SwiftUI instead of shared UI |
| [0003](adr/0003-android-kmp-plugin-single-variant.md) | Android-KMP single variant | Level A - signoff | Runtime config strategy |
| [0004](adr/0004-kmp-swift-api-contract.md) | KMP <-> Swift API contract | Level A/B | Facades, SKIE, API boundary |
| [0005](adr/0005-transition-from-v1.md) | v1 -> v2 transition (alternative: new app ID) | Level C - alternative scenario | Applies only if new app ID is chosen |
| [0014](adr/0014-in-place-migration-from-v1.md) | v1 -> v2 in-place migration (primary: same app ID) | Level A - recommended direction | Same app ID, OS upgrade, token/deep-link/push carryover |
| [0006](adr/0006-payments-architecture.md) | Payments | Deferred / Level C | PayU, Google Pay, Apple Pay |
| [0007](adr/0007-ble-zbox-protocol.md) | BLE Z-Box protocol | Level C - input required | Shared state machine, native BLE transport |
| [0008](adr/0008-threat-model.md) | Threat model | Level C - owner required | Data classification, token storage, logging |
| [0009](adr/0009-offline-sync-matrix.md) | Offline sync matrix | Deferred / Level C | Per-entity cache/write rules |
| [0010](adr/0010-release-readiness.md) | Release readiness | Deferred / Level C | Gates, rollout, hotfix flow |
| [0011](adr/0011-design-system-parity.md) | Design system parity | Level A/C | Tokens, a11y, dark mode |
| [0012](adr/0012-backend-api-contract.md) | Backend API contract | Level A/C | OpenAPI, error envelope, mock server |
| [0013](adr/0013-assets-localization-strategy.md) | Assets & localization | Level B - validation | Strings, assets, translation workflow |

Related analysis:

- [KMP ViewModel sharing analysis](kmp-viewmodel-sharing-analysis.md)
- [Resources, Localization, and MOKO Deep Dive](resources-localization-moko-deep-dive.md)

## 1. Context

| Item | Value |
|---|---|
| Project | Packeta app v2.0 - rewrite of the current production app |
| Backend | Existing backend, API contract must be clarified |
| Repository | Monorepo: Android + iOS + shared KMP |
| Project name | Decision required |
| Base package | Decision required |
| Production application ID / bundle ID | **Primary scenario**: same as v1 (`cz.zasilkovna.zasilkovna`) — team input Jan 2026-04-14. See [ADR-0014](adr/0014-in-place-migration-from-v1.md). Alternative scenario (new ID) covered by [ADR-0005](adr/0005-transition-from-v1.md). |
| Dev/stage IDs | Production ID + `.dev` / `.stage` style suffix |

### 1.1 Removed from v1 (do not port to v2)

v1 manifest contains integrations and SDKs that v2 must NOT carry over:

- **Facebook SDK** (login, analytics auto-logging, ad attribution) - removed.
- **AdMob / ad services** (`AD_SERVICES_CONFIG`, `com.google.android.gms.ads.AD_MANAGER_APP`) - removed (no ads in v2).
- **Sentry** - replaced by Firebase Crashlytics.
- **Fabric** (legacy Crashlytics key `io.fabric.ApiKey`) - replaced by Firebase Crashlytics.

Rationale: scope confirmed by team. If the Orbis template or a developer copies v1 manifest fragments, these entries must be stripped.

## 2. Architecture

### 2.1 Main Approach

- **Shared**: KMP data + domain layers. See [ADR-0001](adr/0001-kmp-shared-data-domain.md).
- **Native UI**: Jetpack Compose on Android, SwiftUI on iOS. See [ADR-0002](adr/0002-native-ui-per-platform.md).
- **ViewModels**: native by default; shared KMP ViewModel only after a feature-specific spike.
- **Clean Architecture direction**: `presentation -> domain <- data`.
- **Runtime data flow**: `user -> presentation -> domain use case -> data -> API/DB -> back`.
- **Use cases**: default access point for domain behavior.
- **iOS API boundary**: per-feature facades. See [ADR-0004](adr/0004-kmp-swift-api-contract.md).

### 2.2 Target Module Structure

```text
Packeta/
  core/
    domain/       KMP: base Result/Error types, common domain contracts
    data/         KMP: Ktor, Room base, DataStore base, mappers
    config/       KMP: AppConfig contract
    resources/    KMP: typed semantic keys, not full UI resource runtime
    ui/           Android-only: Compose design system

  feature/
    onboarding/
      domain/     KMP: use cases, repository interfaces
      data/       KMP: repository impl, DTOs, mappers
      ui/         Android-only: Compose screens and ViewModels
    tracking/
      domain/
      data/
      ui/
    auth/
    profile/
    payments/
    zbox/

  shared/         KMP umbrella module exported to iOS as XCFramework
  androidApp/     Android application module
  iosApp/         Xcode project, SwiftUI screens, iOS design system
```

Rules:

- Android UI modules are not exported to iOS.
- iOS owns SwiftUI screens and ViewModels.
- `:shared` depends on KMP domain/data modules and exports one framework to iOS.
- Platform SDKs stay platform-specific.

### 2.3 Layer Contract

```kotlin
// feature:tracking:domain
interface PackageRepository {
    suspend fun getPackage(id: String): OperationResult<Package, TrackingError>
    fun observePackages(): Flow<List<Package>>
}

class GetPackageUseCase(
    private val repository: PackageRepository
) {
    suspend operator fun invoke(id: String) = repository.getPackage(id)
}
```

```kotlin
// feature:tracking:data
class PackageRepositoryImpl(
    private val api: PackageApi,
    private val dao: PackageDao
) : PackageRepository
```

## 3. Tech Stack

### 3.1 Shared KMP

| Area | Candidate |
|---|---|
| Language | Kotlin Multiplatform |
| Networking | Ktor Client |
| Serialization | kotlinx.serialization |
| Database | Room KMP |
| Key-value storage | DataStore KMP where suitable |
| DI | Koin |
| Async | Coroutines + Flow |
| Testing | kotlin-test, Turbine, MockK, Ktor MockEngine |
| Coverage | Kover |
| iOS bridge | SKIE candidate, validate against KMP-NativeCoroutines |
| Resources | `ResourceKey` contract; KMP resource library only after spike |

### 3.2 Android

| Area | Candidate |
|---|---|
| UI | Jetpack Compose |
| ViewModel | AndroidX ViewModel |
| Navigation | Jetpack Navigation 3 |
| Permissions | AndroidX Activity Result APIs |
| Maps | Google Maps |
| Push | Firebase Cloud Messaging |
| Crash reporting | Firebase Crashlytics |
| Payments | Google Pay + PayU |

### 3.3 iOS

| Area | Candidate |
|---|---|
| UI | SwiftUI |
| ViewModel | `@Observable` / `ObservableObject` depending on min iOS |
| Navigation | SwiftUI NavigationStack |
| Maps | MapKit |
| Push | APNs |
| Payments | Apple Pay + PayU |
| KMP consumption | Decision required: direct framework, XCFramework + SPM, or another flow |

### 3.4 Infrastructure

| Area | Candidate |
|---|---|
| CI | GitLab CI |
| Distribution | Firebase App Distribution for Android, TestFlight for iOS |
| Analytics | Firebase Analytics |
| Performance | Firebase Performance |
| Translations | Crowdin / Lokalise / Phrase / internal tool (decision pending) |
| Release automation | Fastlane decision after iOS release-flow review |

## 4. Build and Infrastructure

### 4.1 Build Variants

Android:

- `dev`,
- `stage`,
- `prod`.

iOS:

- xcconfig per environment,
- bundle ID per environment,
- matching backend base URL and app metadata.

Shared KMP modules do not own environment variants. They receive config at runtime through `AppConfig`. See [ADR-0003](adr/0003-android-kmp-plugin-single-variant.md).

### 4.2 Secrets

Rules:

- no secrets in Git,
- CI injects signing and API secrets,
- production config cannot be overridden accidentally by debug values,
- shared code never hardcodes secrets or environment URLs.

### 4.3 Build Logic

Use convention plugins for:

- Android app/module defaults,
- KMP module defaults,
- Kotlin compiler options,
- Detekt/KtLint,
- test/coverage setup,
- KMP iOS framework build setup.

### 4.4 Version Catalog

Use `libs.versions.toml` for dependency versions. Avoid version drift across modules.

## 5. Networking and Data

### 5.1 HTTP Layer

Shared KMP owns:

- Ktor client setup,
- auth headers,
- token refresh,
- error mapping,
- correlation IDs,
- request logging with redaction,
- DTO parsing.

Do not log PII or tokens.

### 5.2 Offline Strategy

Do not use one blanket "offline-first" rule.

Use the per-entity matrix from [ADR-0009](adr/0009-offline-sync-matrix.md):

- package list/detail: cache-first read,
- payments: online-only,
- Z-Box open: online/live BLE only,
- auth/OTP: online-only,
- feature flags: cache with TTL,
- profile/settings: possible queued writes if safe.

### 5.3 Secure Storage

Auth tokens:

- Android: Keystore-backed storage,
- iOS: Keychain,
- shared access through platform abstraction.

OTP:

- memory only,
- never log,
- do not persist.

## 6. Resources and Localization

Supported languages at launch: **CS, EN, SK, HU, RO** (5 languages).

Decision summary:

- user-facing UI strings are platform-native,
- translation tool is the source of truth,
- KMP returns semantic keys / typed errors,
- MOKO is not default; validate it only if a shared KMP resource runtime is required.

See [ADR-0013](adr/0013-assets-localization-strategy.md) and [Resources/MOKO deep dive](resources-localization-moko-deep-dive.md).

## 7. Navigation and Deep Links

Android:

- Jetpack Navigation 3 (stable since 2025-11),
- prefer typed routes for app-owned routes,
- deep-link routing must preserve backwards compatibility with v1 deep-link schemes (see v1 manifest inventory).

iOS:

- SwiftUI NavigationStack,
- deep-link routing inside the iOS app,
- universal link behavior must be confirmed.

Shared:

- no shared navigation stack,
- shared code exposes semantic route targets only for features that need cross-platform routing state.

## 8. Authentication and Security

First onboarding/auth flow must clarify:

- OTP request,
- OTP submit,
- token refresh,
- logout/revoke,
- rate limiting,
- account lockout,
- error envelope,
- mock mode and staging data.

Security minimum before auth implementation:

- token storage policy,
- log redaction,
- transport security,
- backup policy,
- safe push payload format.

See [ADR-0008](adr/0008-threat-model.md) and [ADR-0012](adr/0012-backend-api-contract.md).

## 9. Platform-Specific Features

### 9.1 Payments

Payments are online-only and backend-authoritative.

Native owns:

- Google Pay,
- Apple Pay,
- PayU SDK/browser handoff where platform-specific.

Shared owns:

- payment use cases,
- API integration,
- state/error mapping,
- idempotency handling.

See [ADR-0006](adr/0006-payments-architecture.md).

### 9.2 BLE Z-Box

Native owns:

- Android BluetoothGatt,
- iOS CoreBluetooth,
- permissions and platform lifecycle.

Shared owns:

- state machine,
- command validation,
- backend contract,
- error mapping.

Permission scaffold inherited from v1:

- Android: `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` with `neverForLocation` flag (Android 12+); `ACCESS_FINE_LOCATION` for Android < 12. Already present in v1 manifest.
- iOS: `NSBluetoothAlwaysUsageDescription` required in `Info.plist`.

Protocol cannot be accepted until the real specification is provided. See [ADR-0007](adr/0007-ble-zbox-protocol.md).

### 9.3 Maps

Maps stay native:

- Android: Google Maps,
- iOS: MapKit unless product requires another provider.

Shared code can provide domain data, filtering, and clustering input, but not the map UI runtime.

### 9.4 Push Notifications

Push is platform-specific:

- Android: FCM,
- iOS: APNs.

Shared code can process semantic notification types after platform routing.

Payloads should avoid PII.

### 9.5 Scanner

**Verify with product team whether Packeta v2 actually uses a scanner (QR / barcode) feature.** v1 manifest does not clearly indicate scanner integration, and team recollection is uncertain. Treat as `Level C - Input required` until confirmed.

If scanner is used: integration stays native (Android CameraX + ML Kit, iOS AVFoundation / VisionKit). Shared code can parse/validate scanned values.

## 10. Monitoring and Analytics

Recommended stack (Level A):

- Firebase Crashlytics for crash reporting on both platforms.
- Firebase Analytics for event tracking, consent gated (off by default).
- Firebase Performance for runtime metrics (cold start, screen render, network).

Needed decisions:

- analytics event taxonomy (owned by product/marketing),
- consent UX and copy,
- PII redaction rules in logs/analytics/crash,
- app generation dimension: `v1` / `v2` as user property,
- correlation ID usage between client and backend.

Do not log PII or raw backend payloads.

## 11. Feature Flags and Remote Config

Feature flags should support:

- app generation targeting,
- environment targeting,
- staged rollout,
- kill switch for high-risk features,
- backend compatibility.

Forced update is part of release readiness, not a replacement for feature flags.

## 12. Testing Strategy

Minimum coverage:

- shared domain/use case tests,
- DTO mapper tests,
- Ktor MockEngine API tests,
- error mapping tests,
- Room/DataStore tests where applicable,
- Android ViewModel tests,
- iOS ViewModel tests,
- contract tests against OpenAPI/mock server,
- basic UI smoke tests.

Optional/phase-two:

- Android screenshot tests,
- iOS snapshot tests,
- accessibility smoke tests,
- performance tests for critical flows.

## 13. Code Quality

Required:

- KtLint/Detekt or equivalent Kotlin checks,
- SwiftLint or iOS equivalent after the iOS linting decision,
- binary/API compatibility checks for exported KMP API,
- dependency version catalog,
- CI build and tests,
- no PII logging in tests/examples.

Public shared API must be reviewed before it is exported to iOS.

## 14. Git Workflow

Recommended direction (Level A):

- **Branch strategy**: `main` / `dev` / `feature/*`. Each task gets its own feature branch off `dev`.
- **Merge policy**: squash merge feature branches into `dev`; merge `release/*` into `main` without squash.
- **Release branches**: `release/<version>` cut from `dev`, merged to `main` on release.
- **Protected branches**: `main` and `dev` require CI green and at least one reviewer.
- **Required reviews for shared KMP API**: changes to public facades/domain contracts require explicit review.

Open inputs (Level C):

- exact PR approval count (1 vs 2),
- PR template content,
- concrete CI gate thresholds (coverage %, lint severity),
- CODEOWNERS usage (team lead confirmed not required for initial team size).

## 15. CI/CD

Target CI gates:

- Android build,
- shared KMP tests,
- Android unit tests,
- iOS build,
- iOS tests if available,
- lint/static analysis,
- KMP framework build,
- artifact upload for internal testing.

Initial template can start with build + tests, then add signing/distribution.

## 16. Release Process

Release readiness is not a scaffold blocker, but must exist before public distribution.

See [ADR-0010](adr/0010-release-readiness.md) for:

- versioning,
- staged rollout,
- rollout gates,
- forced update,
- rollback,
- hotfix flow.

## 17. Compliance and Privacy

Needed before release:

- iOS privacy manifests,
- Google Play Data Safety,
- GDPR export/delete behavior,
- analytics consent,
- third-party SDK data review,
- support/legal links.

Data classification is defined in [ADR-0008](adr/0008-threat-model.md).

## 18. Developer Experience

Useful debug menu items:

- current environment,
- API base URL,
- feature flags,
- clear tokens,
- crash test button,
- current user/session info where safe,
- version/build/git SHA.

Mock mode:

- supports UI development,
- supports demo and error states,
- should follow backend contract.

Documentation required in the new repo:

- `README.md`,
- `CONTRIBUTING.md`,
- `docs/README.md`,
- `docs/architecture-diagrams.md`,
- `docs/adr/`.

## 19. v1 -> v2 Migration

Two scenarios are documented. The team must confirm which one applies.

### Primary scenario (team input Jan, 2026-04-14) - same production app ID

v2 keeps the same production Android `applicationId` and iOS bundle ID as v1 (`cz.zasilkovna.zasilkovna`). Dev and stage get distinct suffixes.

Impact:

- OS treats v2 as an **in-place upgrade** of v1,
- v1 local storage (DB, preferences, Keystore/Keychain) persists,
- tokens migrate/validate on first v2 launch - users do NOT have to re-login unless token format is incompatible,
- all v1 deep-link schemes/hosts/paths must keep working in v2,
- FCM/APNs re-subscription is explicit on first v2 launch,
- rollback is technically impossible - Google Play / App Store cannot downgrade users,
- server-side kill switch is required (`/config/minSupportedVersion`).

Full strategy, implementation checklist, DB options (cache-first vs schema migration) and rollout gates are covered by [ADR-0014: In-place migration from v1](adr/0014-in-place-migration-from-v1.md).

### Alternative scenario - new production app ID

If the team reverses the decision and ships v2 under a new app ID, the OS treats v2 as a new app. That scenario is covered by [ADR-0005: Transition from v1](adr/0005-transition-from-v1.md) and implies fresh login, no local data carryover, v1/v2 coexistence, separate push routing, and an active store/support communication plan.

### Open input

- [ ] Team to explicitly confirm the production app ID decision so that one ADR becomes `Accepted` and the other is marked `Superseded`.

## 20. KMP/Swift API Contract

iOS should consume shared code through per-feature facades.

Do:

- expose `TrackingFacade`, `AuthFacade`, etc.,
- use typed result/error models,
- validate SKIE vs KMP-NativeCoroutines,
- document public API.

Do not:

- expose repository implementations,
- expose internal use cases directly,
- leak DTOs to Swift UI.

See [ADR-0004](adr/0004-kmp-swift-api-contract.md).

## 21. Environment and Config Strategy

Shared modules do not have flavors.

Use:

- Android flavor/build config -> `AndroidAppConfig`,
- iOS xcconfig/Info.plist -> `IosAppConfig`,
- shared `AppConfig` interface.

See [ADR-0003](adr/0003-android-kmp-plugin-single-variant.md).

## 22. Design System and Accessibility

Native UI means two native design-system implementations.

Shared:

- tokens,
- naming,
- copy rules,
- accessibility checklist.

Not shared by default:

- UI components,
- screens,
- platform navigation.

See [ADR-0011](adr/0011-design-system-parity.md).

## 23. Store Compliance

Android:

- package name/app ID,
- Play Data Safety,
- permissions,
- signing,
- staged rollout.

iOS:

- bundle ID,
- privacy manifests,
- associated domains,
- APNs,
- TestFlight,
- App Store privacy labels.

Both:

- support URL,
- privacy policy,
- terms/legal links,
- analytics consent behavior.

## 24. Decision State and Open Inputs

This section separates signoff, validation, and missing inputs. Level A items are not phrased as open questions because they are the recommended architecture direction.

### 24.1 Level A - Recommended Direction, Team Signoff

- [ ] KMP shares data/domain only; UI is not shared by default.
- [ ] Android UI uses Jetpack Compose.
- [ ] iOS UI uses SwiftUI.
- [ ] ViewModels are native by default.
- [ ] Shared ViewModel is an exception path for a specific feature, not the baseline.
- [ ] iOS calls KMP through per-feature facades.
- [ ] Platform SDK integrations stay native: payments, BLE, maps, push. Scanner usage to be verified.
- [ ] Shared KMP modules receive runtime `AppConfig`; environment flavors stay in app modules.
- [ ] Monorepo structure contains Android app, iOS app, shared KMP modules, and `:shared` umbrella export.
- [ ] Android navigation uses Jetpack Navigation 3.
- [ ] Crash reporting uses Firebase Crashlytics on both platforms.
- [ ] Analytics uses Firebase Analytics with consent gating (off by default).
- [ ] Performance monitoring uses Firebase Performance.
- [ ] Git workflow: `main` / `dev` / `feature/*` with squash merge and protected branches.
- [ ] No Facebook SDK, no AdMob, no Sentry, no Fabric in v2 (removed from v1 inventory).
- [ ] Supported launch languages: CS, EN, SK, HU, RO.
- [ ] v1 -> v2 migration scenario is the primary scenario (same production app ID, in-place upgrade — see [ADR-0014](adr/0014-in-place-migration-from-v1.md)).

### 24.2 Level B - Preferred Direction, Validation Needed

- [ ] Swift bridge: validate SKIE against KMP-NativeCoroutines.
- [ ] iOS framework distribution: validate XCFramework + SPM against direct linking or another flow.
- [ ] Resources: validate native Android/iOS export from the selected translation tool.
- [ ] MOKO Resources: spike only if a shared KMP resource runtime remains required.
- [ ] Translation tool selection: Crowdin / Lokalise / Phrase / internal.
- [ ] Storage foundation: prove Room KMP and DataStore in Android + iOS build.
- [ ] iOS DI bridge: decide how Swift obtains facade instances.
- [ ] Scanner feature: verify with product whether v2 uses QR/barcode scanner at all.

### 24.3 Level C - Input Required

- [ ] Project name.
- [ ] Base package.
- [ ] **CONFIRM production app ID scenario**: same as v1 (primary, ADR-0014) or new app ID (alternative, ADR-0005).
- [ ] Store strategy for v2.
- [ ] Minimum Android version.
- [ ] Minimum iOS version.
- [ ] OpenAPI coverage for onboarding/auth.
- [ ] OTP request/submit contract.
- [ ] Token refresh/revoke contract.
- [ ] Standard error envelope.
- [ ] Mock server ownership.
- [ ] Staging test accounts.
- [ ] Backend capabilities: silent push, delta sync, feature flags, API compatibility guarantees.
- [ ] Security owner.
- [ ] Compliance/privacy owner.
- [ ] Token storage policy.
- [ ] Log redaction rules.
- [ ] Backup/restore policy.
- [ ] Certificate pinning policy.
- [ ] App attestation policy for high-risk operations.
- [ ] Z-Box protocol owner and real specification.
- [ ] Z-Box crypto scheme and token format.
- [ ] Z-Box firmware/test lab matrix.
- [ ] v1 support duration.
- [ ] v1 promotion to v2.
- [ ] Push routing between v1 and v2.
- [ ] Deep-link/universal-link routing between v1 and v2.
- [ ] Support/store communication for v2 adoption.
- [ ] Release cadence and release manager.

### 24.4 Deferred Feature Topics

- [ ] Payment provider details and merchant IDs.
- [ ] Payment 3DS UX, refunds, reconciliation, and compliance owner.
- [ ] Offline sync entities, TTLs, conflict rules, and retry policy.
- [ ] Full public release readiness and hotfix process.

## 25. Action Items for the Orbis Template

### 25.1 Scaffold Blockers

- [ ] Rename/structure Android app module to match target layout.
- [ ] Create `:shared` umbrella module for iOS export.
- [ ] Separate Android-only UI modules from KMP shared export.
- [ ] Make iOS build consume the generated framework reliably.
- [ ] Remove any README claims that the default is shared ViewModels.

### 25.2 Module Structure

- [ ] `:core:domain`
- [ ] `:core:data`
- [ ] `:core:config`
- [ ] `:core:resources`
- [ ] `:core:ui` Android-only
- [ ] `:feature:onboarding:domain`
- [ ] `:feature:onboarding:data`
- [ ] `:feature:onboarding:ui` Android-only
- [ ] `:shared`
- [ ] `iosApp`

### 25.3 Build Variants and Config

- [ ] Android dev/stage/prod.
- [ ] iOS dev/stage/prod.
- [ ] Runtime `AppConfig`.
- [ ] CI-safe environment config.

### 25.4 Shared Foundation

- [ ] `OperationResult`.
- [ ] `AppError` hierarchy.
- [ ] Ktor client.
- [ ] Error envelope mapper.
- [ ] Token refresh foundation.
- [ ] Room KMP proof.
- [ ] DataStore proof for key-value storage.
- [ ] Mock mode foundation.

### 25.5 iOS Bridge

- [ ] Feature facade skeleton.
- [ ] SKIE/KMP-NativeCoroutines spike.
- [ ] iOS sample ViewModel consuming facade.
- [ ] Binary/API compatibility check.

### 25.6 Testing

- [ ] Shared unit test setup.
- [ ] Ktor MockEngine tests.
- [ ] Mapper tests.
- [ ] Android ViewModel test example.
- [ ] iOS facade/ViewModel test example.
- [ ] Contract test approach.

### 25.7 Migration foundation (primary scenario - ADR-0014)

- [ ] v1 token storage inventory (Keystore aliases, Keychain service/account, crypto format).
- [ ] v1 deep-link scheme/host/path inventory (ported from v1 Android manifest + iOS Associated Domains).
- [ ] Token migration + validation flow on first v2 launch.
- [ ] DB strategy chosen (option A cache-first re-fetch recommended, option B schema migration alternative).
- [ ] FCM/APNs re-subscription on first v2 launch.
- [ ] `X-App-Generation: v2` header on backend requests.
- [ ] `/config/minSupportedVersion` check at app start (kill switch).
- [ ] Staged-rollout monitoring dashboards (crash-free, login success, first-launch success).

### 25.8 Documentation

- [ ] Repo `README.md`.
- [ ] `CONTRIBUTING.md`.
- [ ] Architecture diagrams.
- [ ] ADRs.
- [ ] PR template.
- [ ] Release runbook before internal distribution.

## 26. Summary

Current proposal:

- KMP shared data/domain.
- Native UI: Compose + SwiftUI.
- Native ViewModels by default.
- iOS uses per-feature facades.
- Runtime config through `AppConfig`.
- Backend contract is a blocker for shared data/auth.
- Resources default to native Android/iOS output from a translation tool.
- v2 likely uses a new app ID/bundle ID, so transition is not local DB migration.

Immediate blockers:

- Orbis scaffold structure,
- iOS API bridge spike,
- runtime config pattern,
- backend/auth contract,
- resources workflow decision.

Not immediate blockers:

- payments,
- BLE Z-Box,
- full offline sync matrix,
- public release readiness.
