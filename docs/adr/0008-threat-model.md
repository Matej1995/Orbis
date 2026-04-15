# ADR 0008: Threat Model and Data Classification

## Status

**Proposed - Level C: security owner and policy input required** (2026-04-15)

## Context

Security must be defined before implementing auth, token storage, logging, payments, and BLE flows. A checklist is not enough; the app needs explicit data classification and rules.

Relevant attacker examples:

- passive network attacker,
- stolen or shared device,
- malicious app on the same device,
- rooted/jailbroken device,
- internal support/debug misuse,
- accidental PII leakage through logs, analytics, crash reports, or push payloads.

## Decision

Use data classification and enforce storage/logging/transport rules per class.

## Data Classification

| Class | Examples | Rule |
|---|---|---|
| Public | app version, public config | Can be logged. |
| Internal | feature flags, non-sensitive diagnostics | Log only if useful and non-identifying. |
| PII | name, email, phone, address, parcel ownership | Do not log; store only when needed. |
| Sensitive | auth tokens, OTP, payment tokens, BLE open tokens | Secure storage only; never log. |
| Regulated | payment/card data | Do not store in the app unless a compliant SDK owns it. |

## Storage Rules

- Auth tokens: Android Keystore-backed storage / iOS Keychain.
- OTP values: memory only; do not persist.
- Payment tokens: handled by payment SDK/backend contract; do not log or persist raw values.
- BLE open tokens: short-lived; store only if required for an active operation.
- PII cache: Room/DataStore only when needed; define TTL/invalidation.
- Debug logs: no PII or tokens.

## Token Management

Required decisions:

- access token lifetime,
- refresh token lifetime,
- refresh policy,
- logout and token revocation,
- behavior after token refresh failure,
- behavior across v1/v2 fresh-login transition.

Token refresh must be centralized in the shared data layer, not duplicated per feature.

## Transport Security

Default:

- HTTPS only,
- no cleartext traffic in production,
- strict production config,
- environment-specific API base URL from `AppConfig`.

Open decision:

- certificate pinning policy,
- pin rotation,
- staging/dev behavior,
- emergency disable mechanism.

## Log Redaction

Logs, analytics, and crash reports must not contain:

- tokens,
- OTP codes,
- full names,
- phone numbers,
- email addresses,
- addresses,
- raw API payloads with PII,
- payment SDK payloads.

Use explicit redaction helpers for app errors and network diagnostics.

## Push Notification Payloads

Push payloads should avoid PII.

Allowed pattern:

```json
{
  "type": "package_update",
  "ref": "opaque-id"
}
```

The app fetches details after opening the notification.

## Analytics and Crash Identifiers

Use stable but non-sensitive identifiers:

- app generation,
- app version,
- platform,
- backend user ID only if allowed by privacy policy and consent rules,
- non-PII install/session ID.

Do not use email/phone as analytics identifiers.

## App Attestation

Play Integrity / DeviceCheck can be considered for high-risk operations:

- payments,
- Z-Box open,
- account takeover-sensitive actions.

Decide this before production release for high-risk flows.

## Root/Jailbreak Detection

Do not block all rooted/jailbroken users by default without product/security decision.

Possible policy:

- collect risk signal,
- block only high-risk operations,
- show clear support/error copy.

## Backup and Restore

Decide what can be backed up/restored:

- tokens should not be restored across devices,
- caches can usually be rebuilt from backend,
- PII cache backup must match privacy rules.

## Consequences

### Positive

- Security behavior is explicit before implementation.
- Auth, payments, BLE, and logging use the same rules.
- Reduces accidental PII leakage.

### Negative

- Requires owners and review.
- Adds work to logging, analytics, and storage implementation.
- Some decisions depend on backend/security teams.

### Remaining Risks

- Backend may return sensitive data in error bodies.
- Third-party SDKs may collect data; privacy review is required.
- Debug builds can still leak data if developers bypass redaction.

## Open Questions

- [ ] Who owns mobile security decisions?
- [ ] Is external security review required?
- [ ] What is the token lifetime and refresh policy?
- [ ] Is certificate pinning required?
- [ ] What is the backup policy?
- [ ] What is the app attestation policy for payments/Z-Box?

## Related

- [ADR-0005: Transition from v1](0005-transition-from-v1.md)
- [ADR-0006: Payments architecture](0006-payments-architecture.md)
- [ADR-0007: BLE Z-Box protocol](0007-ble-zbox-protocol.md)
- [ADR-0009: Offline sync matrix](0009-offline-sync-matrix.md)
