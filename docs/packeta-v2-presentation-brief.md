# Packeta v2.0 - Presentation Brief

> Status: draft (2026-04-15)
> Purpose: outline for the technical discussion before creating the Packeta v2 repository.

## Meeting Goal

1. Sign off the recommended architecture direction.
2. Identify which pending decisions need a spike.
3. Collect missing inputs from backend, product, security, release, and Z-Box owners.
4. Decide what must be fixed in Orbis before using it as a Packeta v2 template.

## Level A - Recommended Direction

These decisions are the proposed baseline. The meeting goal is signoff, not reopening them without a concrete constraint.

| Topic | Direction |
|---|---|
| KMP scope | Share data/domain only; do not share UI by default. |
| UI | Android Compose and iOS SwiftUI. |
| ViewModels | Native ViewModels by default; shared ViewModel only by exception after a feature spike. |
| iOS API boundary | Swift calls per-feature facades, not internal Kotlin use cases or repositories. |
| Platform SDKs | Payments, maps, BLE, push stay native. Scanner: verify whether v2 uses it. |
| Config | Shared KMP modules receive runtime `AppConfig`; environment flavors stay in app modules. |
| Repository shape | Monorepo with Android, iOS, shared KMP, and `:shared` iOS umbrella export. |
| Android navigation | Jetpack Navigation 3. |
| Crash reporting | Firebase Crashlytics on both platforms. |
| Analytics | Firebase Analytics with consent gating (off by default). |
| Performance | Firebase Performance. |
| Git workflow | `main` / `dev` / `feature/*` with squash merge and protected branches. |
| Removed from v1 | No Facebook SDK, no AdMob, no Sentry, no Fabric in v2. |
| Launch languages | CS, EN, SK, HU, RO (5 languages). |

Relevant diagrams:

- `architecture-preview/01-high-level-architecture.png`
- `architecture-preview/02-module-dependency-graph.png`
- `architecture-preview/05-ios-public-api-boundary.png`
- `architecture-preview/06-v1-v2-transition.png`
- `architecture-preview/08-resources-localization-workflow.png`

## Level B - Pending Decisions With Preferred Direction

These are not open-ended architecture questions. Each has a preferred direction, but needs validation or owner confirmation.

| Topic | Preferred direction | Required validation |
|---|---|---|
| Swift bridge | SKIE first, compare against KMP-NativeCoroutines. | Spike cancellation, `Flow`, suspend functions, errors, Swift readability. |
| iOS framework distribution | XCFramework + SPM if it fits CI and local development. | Verify build speed, resource handling, and Xcode ergonomics. |
| Resources/localization | Translation tool exports native Android/iOS resources. | Select tool and validate placeholder/plural/key export. |
| Translation tool | Crowdin / Lokalise / Phrase / internal. | Evaluate against Android XML and iOS String Catalog export. |
| MOKO Resources | Not default. | Spike only if shared KMP resource runtime is required. |
| Storage foundation | Room KMP for structured cache; DataStore for small key-value state. | Prove Android + iOS build and migration ergonomics in Orbis. |
| Scanner feature | Likely not used in v2. | Confirm with product before scaffolding scanner code. |

## Level C - Open Inputs

These items are genuinely unresolved because they require information outside this document.

| Area | Missing input |
|---|---|
| App identity | Project name, base package, **confirm production app ID scenario** (primary: same as v1 — [ADR-0014](adr/0014-in-place-migration-from-v1.md); alternative: new app ID — [ADR-0005](adr/0005-transition-from-v1.md)), store strategy. |
| Platform support | Minimum Android and iOS versions. |
| Backend/auth | OpenAPI coverage, OTP flow, token refresh/revoke, error envelope, staging data, mock server ownership. |
| Backend capabilities | Silent push, delta sync, feature flags, API compatibility guarantees. |
| Security/privacy | Security owner, compliance owner, token storage policy, log redaction rules, backup policy, pinning/attestation decision. |
| Z-Box BLE | Protocol owner, current protocol version, crypto scheme, token format, firmware matrix, test lab coverage. |
| Release | Release cadence, release manager, signing ownership, hotfix flow. |
| v1 -> v2 migration/transition | Confirm app ID scenario (primary: same as v1 = in-place migration, ADR-0014; alternative: new app ID = transition, ADR-0005); v1 token/DB/deep-link inventory; FCM/APNs re-registration; rollout gates; support communication. |

## Anticipated Team Questions

### Why not Compose Multiplatform for UI?

Packeta v2 needs native UX, native navigation, platform accessibility behavior, and native SDK integration for payments, maps, BLE, push, scanner, and permissions. KMP still removes duplication in data/domain, API mapping, caching, and error handling without forcing iOS to consume a shared UI runtime.

### Why native ViewModels if KMP ViewModel is technically possible?

A shared ViewModel moves KMP into presentation state, lifecycle, navigation events, cancellation, and SwiftUI consumption patterns. Native ViewModels keep Android and iOS idiomatic. Shared reducers/state machines remain possible for selected flows after a spike.

### Why not MOKO Resources for all strings and assets?

The proposed UI stack is native Compose + SwiftUI, so native resources fit both platforms better. A translation tool can still be the single source of truth and export Android/iOS resources. MOKO becomes relevant only if common code or shared UI needs a shared KMP resource runtime.

### Why define a backend API contract if both apps already use one API?

The shared data layer centralizes DTOs, error mapping, token refresh, mock data, and contract tests. Without an explicit mobile contract, Android and iOS can still interpret the same backend behavior differently.

### Why use iOS facades?

Facades give Swift a stable, feature-oriented API and hide Kotlin internals such as repository implementations, generic-heavy use cases, and data-layer details. They also contain bridge-tool decisions such as SKIE or KMP-NativeCoroutines in one place.

## Main Risks

- Backend/auth contract may not be ready for a shared data layer.
- The current Orbis scaffold does not match the target module structure.
- Swift bridge choice can affect iOS ergonomics and build stability.
- Resource workflow must be validated with Android, iOS, and product/localization owners.
- BLE documentation is an architectural frame, not an approved real protocol specification.
- Security decisions need named ownership before auth and high-risk flows.

## Next Step

1. Sign off Level A direction.
2. Run Level B spikes: Swift bridge, iOS framework distribution, resource export, storage foundation.
3. Collect Level C inputs from backend, security, release, product, and Z-Box owners.
4. Fix Orbis scaffold blockers before creating the Packeta v2 repository.
