# Packeta v2 Status Overview

> Status: draft (2026-04-15)
> Purpose: show what is recommended, what needs validation, and what still needs external input.

## Legend

| Level | Meaning |
|---|---|
| Level A - Signoff | Recommended architecture direction; team signoff is needed, but the document does not treat it as an open-ended question. |
| Level B - Spike / validation | Preferred candidate exists; validate it with a spike, tool comparison, or owner confirmation. |
| Level C - Input required | Cannot be decided from architecture work alone; needs backend/product/security/release/team input. |
| Deferred | Documented for planning, but outside repository creation and first onboarding/auth scope. |

## Existing Outputs

| Output | Status | Note |
|---|---|---|
| Documentation entry point | Documented | [README.md](README.md) explains the review order and deferred topics. |
| Presentation brief | Documented | [packeta-v2-presentation-brief.md](packeta-v2-presentation-brief.md) is the short meeting input. |
| Full technical analysis | Documented | [packeta-v2-brainstorm.md](packeta-v2-brainstorm.md) keeps wider context, decision levels, and open inputs. |
| ADR index | Documented | [adr/README.md](adr/README.md) lists ADR-0001 to ADR-0014 with decision level. |
| Architecture diagrams | Documented | [architecture-diagrams.md](architecture-diagrams.md) is the source of truth; `architecture-preview/` contains generated PNG/HTML output. |
| KMP ViewModel analysis | Documented | [kmp-viewmodel-sharing-analysis.md](kmp-viewmodel-sharing-analysis.md) compares native and shared ViewModels. |
| Resources/MOKO analysis | Documented | [resources-localization-moko-deep-dive.md](resources-localization-moko-deep-dive.md) compares native resource export and MOKO. |

## Technical Topics

| Topic | Level | Current Direction | Next Action | Priority |
|---|---|---|---|---|
| KMP scope | Level A - Signoff | Share data/domain only; do not share UI by default. | Team signoff. | Before repository creation |
| Native UI | Level A - Signoff | Android Compose + iOS SwiftUI. Compose Multiplatform is not the target UI stack. | Team signoff; reopen only if a concrete constraint appears. | Before repository creation |
| ViewModels | Level A - Signoff | Native ViewModels by default. Shared ViewModel is an exception path, not the baseline. | Team signoff; optional feature spike only if a candidate flow is identified. | Before repository creation |
| iOS facade API | Level A/B | Per-feature facade pattern is the direction; bridge tooling is pending. | Sign off facade boundary; spike SKIE vs KMP-NativeCoroutines and iOS DI flow. | Before repository creation |
| Build/config | Level A - Signoff | Shared KMP modules use runtime `AppConfig`; environment flavors stay in app modules. | Implement and verify dev/stage/prod in Orbis. | Before repository creation |
| Orbis scaffold | Level A - Template blocker | Current scaffold does not match the target structure. | Fix `:shared`, Android app module, iOS build, and Android UI module separation. | Before repository creation |
| Backend API contract | Level A/C | Shared data layer requires an explicit mobile contract: OpenAPI, DTOs, error envelope, mock server. | Confirm OpenAPI coverage, auth/token refresh, OTP flow, error model, contract tests, and staging data. | Blocks onboarding/auth |
| Auth/onboarding foundation | Level C - Input required | First flow handles login, OTP, tokens, and errors. | Write concrete auth flow after backend contract and security minimum are confirmed. | First implementation |
| Resources/localization/assets | Level B - Validation | Native Android/iOS export is preferred; MOKO is conditional. | Pick translation tool; validate export; run MOKO spike only if shared resource runtime remains required. | Before repository creation |
| Production app ID / bundle ID | Level A / Level C | Primary scenario (per team input Jan 2026-04-14): same as v1 (`cz.zasilkovna.zasilkovna`), in-place upgrade — see [ADR-0014](adr/0014-in-place-migration-from-v1.md). Alternative (new app ID) — see [ADR-0005](adr/0005-transition-from-v1.md). | Team confirms which scenario is final; one ADR becomes `Accepted`, the other `Superseded`. | Decide early; finish before release |
| Security minimum | Level C - Owner required | Threat model defines token, log, transport, backup, pinning, and attestation questions. | Assign owner; confirm token storage, log redaction, backup policy, pinning, and attestation scope. | Before auth implementation |
| Design system parity | Level A/C | Native UI requires two platform implementations from shared design tokens/copy rules. | Sign off parity model; assign token owner, Figma workflow, and accessibility checklist. | Before wider UI work |
| Offline sync | Deferred | Per-entity cache/write rules are documented. | Fill concrete entities when each feature starts. | Deferred |
| Payments | Deferred | Architecture draft covers PayU, Google Pay, Apple Pay, and online-only rules. | Confirm merchant IDs, 3DS UX, refunds, backend reconciliation, and compliance owner. | Deferred |
| BLE Z-Box | Deferred / Level C | Architecture frame exists, but real protocol is missing. | Provide protocol owner, crypto scheme, token format, firmware matrix, and test lab matrix. | Deferred |
| Release/CI/CD | Deferred / Level C | Release readiness draft covers gates, rollout, and hotfix flow. | Set up CI, signing, TestFlight/Firebase distribution, release roles, and release cadence. | Deferred |

## Short Conclusion

| Question | Answer |
|---|---|
| What is already a recommended direction? | KMP data/domain, native UI, native ViewModels, iOS facades, runtime config, and native platform SDK ownership. |
| What still needs a spike? | Swift bridge tooling, iOS framework distribution, resource export/MOKO decision, and storage foundation. |
| What is truly open? | App identity, min OS versions, backend/auth details, security ownership, BLE protocol, release ownership, and v1/v2 transition details. |
| Main blockers | Orbis scaffold, iOS bridge spike, runtime config proof, backend/auth contract, and resource export decision. |
| Recommended review set | `README.md`, `packeta-v2-status-overview.md`, `packeta-v2-presentation-brief.md`, diagrams, and priority ADRs. |
