# ADR 0005: Transition from Packeta v1 to v2 (alternative scenario - new production app ID)

## Status

**Proposed - Level C: alternative scenario, applies only if team confirms a new production app ID** (2026-04-15, annotated 2026-04-16)

This ADR covers the **alternative scenario** for v1 -> v2 transition. It applies only if the team decides to ship v2 under a new production Android `applicationId` and iOS bundle ID.

The **primary scenario** (same production app ID, in-place upgrade) is described in [ADR-0014: In-place migration from v1](0014-in-place-migration-from-v1.md). Team input as of 2026-04-14 (Jan) indicates that the production app ID stays the same as v1, which would make ADR-0014 the accepted path and this ADR 0005 superseded. **Team confirmation is required before either ADR is marked `Accepted`.**

## Context

This ADR applies only if Packeta v2 uses a different production application ID / iOS bundle ID than v1. If that scenario is chosen, the OS treats v2 as a new app, not an update.

That means v2 cannot access v1 local storage:

- local database,
- preferences,
- Android Keystore data,
- iOS Keychain data if the access group differs,
- auth tokens,
- cached user/session state.

Transition is therefore a product/distribution problem, not a local database migration.

## Decision

Assume v2 starts with a fresh install and fresh login.

The transition strategy must cover:

1. local data and session,
2. v1 -> v2 adoption,
3. backend coexistence,
4. push notifications,
5. deep links and universal links,
6. store listing and brand,
7. analytics continuity,
8. support and communication.

## Local Data and Session

Do not attempt local database migration from v1 to v2 if app IDs differ.

Instead:

- require fresh login,
- restore user data from backend after login,
- treat v2 local cache as empty on first launch,
- explain the login requirement in UX/support copy.

Fresh login is simpler than local migration, but has UX impact.

## v1 -> v2 Adoption

The team must decide:

- whether v1 remains available in stores,
- whether v1 shows an upgrade prompt,
- whether v2 is promoted as a new app,
- how long both apps are supported,
- whether any forced update path exists.

## Backend Coexistence

Backend must support v1 and v2 during the transition.

Required checks:

- API compatibility,
- app version/header identification,
- feature flag targeting,
- support diagnostics,
- metrics split by app generation.

API contract detail is covered by [ADR-0012](0012-backend-api-contract.md).

## Push Notifications

If v1 and v2 can be installed at the same time, push routing must be explicit.

Decisions needed:

- separate FCM/APNs registrations,
- backend ownership of active device/app generation,
- duplicate notification prevention,
- notification tap routing,
- support for users with both apps installed.

## Deep Links and Universal Links

Decisions needed:

- whether v1 and v2 share domains/schemes,
- whether legacy links open v1, v2, or a routing page,
- fallback to store listing,
- support for campaign links and parcel links.

## Store Listing and Brand

Decisions needed:

- new listing vs existing listing,
- app name,
- icon/brand distinction during coexistence,
- support/legal copy,
- screenshots and privacy labels.

## Analytics Continuity

Analytics must distinguish v1 and v2:

- app generation,
- app version,
- platform,
- install source,
- migration/adoption funnel.

Do not assume a continuous local user identity across app IDs. Backend user ID after login is the stable join point.

## Consequences

### Positive

- Avoids risky local migration.
- Keeps v2 clean and independent.
- Reduces coupling to v1 storage implementation.

### Negative

- Users must log in again.
- Support must handle two apps during transition.
- Backend and push systems must support coexistence.

### Risks

- Users may install both apps and receive confusing notifications.
- Deep links may route to the wrong app.
- Store/support messaging may be unclear.

## Implementation Checklist

- [ ] Confirm final Android application ID and iOS bundle ID.
- [ ] Define v1/v2 coexistence period.
- [ ] Define fresh-login UX.
- [ ] Add app generation to API headers/analytics.
- [ ] Define push device ownership rules.
- [ ] Define deep-link/universal-link routing.
- [ ] Prepare support FAQ and store messaging.

## Open Questions

- [ ] Will v2 definitely use a new production app ID / bundle ID?
- [ ] How long will v1 remain supported?
- [ ] Will v1 promote v2?
- [ ] Who owns store/support communication?

## Related

- [ADR-0014: In-place migration from v1 (primary scenario)](0014-in-place-migration-from-v1.md)
- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0009: Offline sync matrix](0009-offline-sync-matrix.md)
- [ADR-0010: Release readiness](0010-release-readiness.md)
- [ADR-0012: Backend API contract](0012-backend-api-contract.md)
