# ADR 0009: Offline Sync Matrix per Entity

## Status

**Proposed - Deferred / Level C: entities and cache policy input required** (2026-04-15)

## Context

"Offline-first" is too broad for Packeta v2. Different entities have different ownership, freshness, write, and conflict requirements.

Examples:

- tracking data is server-owned,
- payments are online-only,
- user profile can allow queued edits,
- Z-Box open must not be offline,
- consent/privacy state needs strict consistency.

## Decision

Use a per-entity sync matrix. Do not use one global cache/write strategy for everything.

## Matrix

| Entity / Operation | Owner | Read Strategy | Refresh Trigger | Offline Write | Conflict Policy | Retry | TTL / Invalidation |
|---|---|---|---|---|---|---|---|
| Auth token | Server | secure storage | app start / 401 | No | server wins | refresh once, then logout | token expiry |
| User profile | user + server | cache-first | app start / profile open | Queued edit if safe | server wins or explicit merge | exponential backoff | 24h or after edit |
| Package list | server | cache-first | app start / pull / push | No | server wins | yes | short TTL |
| Package detail | server | cache-first | screen open / push | No | server wins | yes | short TTL |
| Payment | server/payment provider | network-only | user action | No | backend wins | idempotent retry only | none |
| Z-Box open | server + hardware | network/BLE live | user action | No | backend/hardware state wins | controlled retry | none |
| Consent | user + server | network-first | app start/settings | No or strictly queued | latest server-confirmed | yes | immediate invalidation |
| Feature flags | server | cache-first | app start / TTL | No | server wins | yes | minutes/hours |
| Static config | app/server | cache-first | app start / version change | No | server wins | long TTL |

## Global Rules

- No offline writes for payments or Z-Box open.
- Optimistic UI is allowed only where rollback is clear and safe.
- Server-owned entities resolve conflicts in favor of server state.
- Queued writes must be idempotent.
- Sync queue must not store sensitive payloads without encryption and TTL.
- Push notifications are invalidation hints, not the source of truth.

## Sync Queue

Only use sync queue for operations that are safe to retry:

- profile update,
- settings change,
- non-critical preference sync.

Do not use sync queue for:

- payments,
- Z-Box open,
- OTP submit,
- token refresh as a business operation.

Queue items should contain:

- operation type,
- idempotency key,
- minimal payload,
- retry count,
- next retry timestamp,
- created timestamp,
- expiration timestamp.

## Push Invalidation

FCM/APNs payload should contain only non-PII identifiers:

```json
{
  "type": "package_update",
  "ref": "opaque-id"
}
```

The app uses the payload to refresh from backend.

## Consequences

### Positive

- Prevents unsafe "offline-first everywhere" behavior.
- Makes payment and Z-Box constraints explicit.
- Gives each feature a concrete caching contract.

### Negative

- Requires more design work per entity.
- Tests must cover multiple sync strategies.
- Backend support is needed for good invalidation and idempotency.

### Risks

- If the matrix is not maintained, implementation may drift.
- If TTLs are wrong, users may see stale state.
- If queue payloads are too large, privacy and storage risk increase.

## Open Questions

- [ ] Which exact entities exist in the first onboarding/auth flow?
- [ ] What backend invalidation signals are available?
- [ ] Which operations are idempotent?
- [ ] What TTLs are acceptable for package data?
- [ ] What data can be stored in the sync queue?

## Related

- [ADR-0005: Transition from v1](0005-transition-from-v1.md)
- [ADR-0006: Payments architecture](0006-payments-architecture.md)
- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0012: Backend API contract](0012-backend-api-contract.md)
