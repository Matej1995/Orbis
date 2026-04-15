# Packeta v2 Documentation

> Status: draft (2026-04-15)
> Purpose: navigation for the technical analysis before creating the Packeta v2 repository.

## Scope

This documentation describes the proposed Packeta v2 mobile architecture in a monorepo:

- KMP shares the data and domain layers.
- Android presentation stays native with Jetpack Compose.
- iOS presentation stays native with SwiftUI.
- iOS calls KMP through per-feature facade APIs.
- Platform SDK integrations stay native: payments, BLE, maps, push (and scanner if confirmed for v2).
- Resources and localization default to native Android/iOS outputs from a translation tool; KMP returns semantic keys.

## Decision Model

The documents separate decision maturity into three levels:

| Level | Meaning | Examples |
|---|---|---|
| Level A - Recommended direction | Architecture direction is selected; team signoff is needed before repository creation. | KMP data/domain, native UI, native ViewModels, iOS facades, runtime `AppConfig`, monorepo template. |
| Level B - Preferred candidate | Direction has a preferred candidate, but a spike or tool comparison must validate it. | SKIE vs KMP-NativeCoroutines, iOS framework distribution, native resources vs MOKO, crash reporting, translation tool. |
| Level C - Input required | The decision cannot be made without team/backend/product/security input. | Project name, base package, final app IDs, min OS versions, BLE protocol, backend capabilities, security owner, release ownership. |

## Before Repository Creation

These topics affect the project structure and should be resolved as Level A signoffs or Level B spikes:

1. Level A: KMP shares data/domain only.
2. Level A: UI stays native: Android Compose and iOS SwiftUI.
3. Level A: ViewModels are native by default.
4. Level A: iOS calls shared code through per-feature facades.
5. Level A: shared KMP modules receive config through runtime `AppConfig`; no environment flavors inside shared modules.
6. Level A: Android navigation uses Jetpack Navigation 3.
7. Level A: crash reporting uses Firebase Crashlytics; analytics uses Firebase Analytics (consent gated); performance uses Firebase Performance.
8. Level A: Git workflow `main` / `dev` / `feature/*` with squash merge.
9. Level A: v1 SDKs not ported to v2 - no Facebook SDK, no AdMob, no Sentry, no Fabric.
10. Level A: launch languages CS, EN, SK, HU, RO.
11. Level B: select the Swift bridge tooling after a spike: SKIE vs KMP-NativeCoroutines.
12. Level B: confirm resources workflow: native export by default; MOKO only if shared resource runtime is required.
13. Level B: confirm translation tool (Crowdin / Lokalise / Phrase / internal).
14. Level C: confirm final app ID / bundle ID, base package, and minimum OS versions.
15. Level C: confirm backend/auth contract: OpenAPI, error envelope, OTP, token refresh, mock server.
16. Level C: confirm whether scanner (QR/barcode) is used in v2.
17. Template work: fix `:shared`, Android app module, iOS build, and Android UI module boundaries in Orbis.

## Deferred From Initial Scope

These topics are documented for planning, but they are not required for repository creation or the first onboarding/auth flow:

- Payments: keep the architecture draft; resolve details before the first payment feature.
- BLE Z-Box: keep as draft until the team provides the real protocol specification.
- Offline sync matrix: use it when a feature actually writes or caches critical data.
- Release readiness: required before internal distribution and store release, not before scaffolding.
- Full threat model: define token storage, log redaction, and transport rules now; complete the rest before release.

## Reading Order

| Document | Role |
|---|---|
| [packeta-v2-presentation-brief.md](packeta-v2-presentation-brief.md) | Short meeting outline. |
| [packeta-v2-status-overview.md](packeta-v2-status-overview.md) | Status table: topic level, current direction, next action. |
| [packeta-v2-brainstorm.md](packeta-v2-brainstorm.md) | Full technical analysis, decision levels, and input backlog. |
| [architecture-diagrams.md](architecture-diagrams.md) | Mermaid source for architecture diagrams. |
| [architecture-preview/](architecture-preview/) | Generated PNG/HTML diagram preview for quick visual review. |
| [adr/README.md](adr/README.md) | ADR index. |
| [kmp-viewmodel-sharing-analysis.md](kmp-viewmodel-sharing-analysis.md) | Native vs shared ViewModel analysis. |
| [resources-localization-moko-deep-dive.md](resources-localization-moko-deep-dive.md) | Native translation-tool output vs MOKO Resources. |

## ADR Priority

| Priority | ADR |
|---|---|
| Before repository creation | ADR-0001, ADR-0002, ADR-0003, ADR-0004, ADR-0012, ADR-0013, ADR-0014 |
| Before first production integration | ADR-0008, ADR-0011 |
| Before a specific feature or release | ADR-0006, ADR-0007, ADR-0009, ADR-0010 |
| Alternative scenario (only if new prod app ID is chosen) | ADR-0005 |

ADR-0014 is the **primary** v1 -> v2 migration scenario (same production app ID). ADR-0005 documents the alternative scenario (new production app ID). Exactly one of the two will be `Accepted`; the other will be marked `Superseded` once the team confirms the app ID decision.

## Review Rules

- `packeta-v2-brainstorm.md` contains the full analysis; the shorter entry documents contain the review summary.
- `architecture-diagrams.md` is the source of truth for diagrams; `architecture-preview/` contains generated preview output.
- ADRs remain formally `Proposed` / `Draft` until team signoff, but each ADR has a decision level to show whether it is signoff, spike, or input-driven.
