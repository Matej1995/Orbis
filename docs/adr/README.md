# Architecture Decision Records

This directory contains Architecture Decision Records for the Packeta v2 KMP project.

## What Is an ADR?

An ADR records one important technical decision: the context, the decision, considered trade-offs, and consequences. It is the team's decision memory.

## ADR Rules

- One ADR covers one decision.
- File format: `NNNN-decision-name.md`.
- Required sections: Status, Context, Decision, Consequences, Open questions, Related.
- Status flow: `Proposed` -> `Accepted` -> optionally `Deprecated` / `Superseded by ADR-XXXX`.
- If a decision changes, create a new ADR and mark the old one as superseded.

## Decision Levels

ADR status says whether the team has formally accepted the record. Decision level says how mature the decision is:

| Level | Meaning |
|---|---|
| Level A - Signoff | Recommended direction; team signoff is needed, but the document treats the direction as the baseline. |
| Level B - Validation | Preferred candidate exists; spike or tool comparison is required before final signoff. |
| Level C - Input required | Decision requires missing backend/product/security/release/protocol input. |
| Deferred | Relevant later, but outside repository creation and first onboarding/auth scope. |

## Index

| # | Title | ADR Status | Decision Level |
|---|---|---|---|
| [0001](0001-kmp-shared-data-domain.md) | KMP for shared data and domain layers | Proposed | Level A - signoff |
| [0002](0002-native-ui-per-platform.md) | Native UI per platform | Proposed | Level A - signoff |
| [0003](0003-android-kmp-plugin-single-variant.md) | Config strategy for shared KMP modules with Android-KMP plugin | Proposed | Level A - signoff |
| [0004](0004-kmp-swift-api-contract.md) | KMP to Swift public API contract | Proposed | Level A/B - facade signoff, bridge validation |
| [0005](0005-transition-from-v1.md) | Transition from Packeta v1 to v2 (alternative scenario, new app ID) | Proposed | Level C - applies only if new app ID is chosen |
| [0006](0006-payments-architecture.md) | Payments architecture | Proposed | Deferred / Level C |
| [0007](0007-ble-zbox-protocol.md) | BLE Z-Box protocol and test matrix | Draft | Level C - real protocol input |
| [0008](0008-threat-model.md) | Threat model and data classification | Proposed | Level C - security owner and policy input |
| [0009](0009-offline-sync-matrix.md) | Offline sync matrix per entity | Proposed | Deferred / Level C |
| [0010](0010-release-readiness.md) | Release readiness, rollout, and hotfix flow | Proposed | Deferred / Level C |
| [0011](0011-design-system-parity.md) | Design system parity, accessibility, and dark mode | Proposed | Level A/C - parity direction, owner input |
| [0012](0012-backend-api-contract.md) | Backend API contract, OpenAPI, and mock server | Proposed | Level A/C - contract direction, backend input |
| [0013](0013-assets-localization-strategy.md) | Assets, resources, and localization strategy | Proposed | Level B - native export preferred, validation needed |
| [0014](0014-in-place-migration-from-v1.md) | In-place migration from v1 to v2 (primary scenario, same app ID) | Proposed | Level A - recommended direction, team confirmation on app ID required |

## Related

- [Main technical analysis](../packeta-v2-brainstorm.md)
- [Status overview](../packeta-v2-status-overview.md)
