# ADR 0014: In-Place Migration from v1 to v2 (same production app ID)

## Status

**Proposed - Level A: recommended direction, team confirmation required on app ID** (2026-04-16)

This ADR is the **primary scenario** for v1 -> v2 transition. It assumes the team confirmation that Packeta v2 keeps the same production Android `applicationId` and iOS bundle ID as v1.

See also [ADR-0005: Transition from v1](0005-transition-from-v1.md) for the alternative scenario (new production app ID, fresh install, coexistence).

## Context

Team input (Jan, 2026-04-14):

> "Application ID (on iOS bundle ID) will have to be the same for prod. It's the same application. So `cz.zasilkovna.zasilkovna`. It's not the best, but we won't change this anymore. For dev/stage, a new suffix to separate from current dev/stage builds."

If confirmed, this means:

- Production Android `applicationId` stays `cz.zasilkovna.zasilkovna`.
- Production iOS bundle ID stays the same as v1.
- Google Play and App Store treat v2 as an **update of the existing app**, not a new app.
- Dev/stage get distinct suffixes (e.g. `cz.zasilkovna.zasilkovna.dev`, `.stage`) so all three builds can coexist on one device.

That is an **in-place upgrade** scenario, not a new-app transition. Millions of existing users will receive v2 as a standard Play Store / App Store update.

The consequences are very different from ADR-0005:

- v2 inherits v1 local storage (OS sandbox is the same).
- v1 Room/SQLite databases, DataStore/SharedPreferences, Android Keystore entries, iOS Keychain entries all remain on the device after the update.
- Users are still logged in after upgrade (unless token format changed incompatibly).
- Old deep links from SMS, email, and push are still directed to the same app bundle, so v2 must understand them.
- Rollback is technically impossible; hotfix is the only recovery path.

## Decision

Treat v2 as an in-place upgrade of v1.

Design v2 to:

1. **Inherit** existing session where possible; never force unnecessary re-login.
2. **Interpret** v1 on-device data safely, even when the v2 schema differs.
3. **Preserve** all v1 deep-link schemes, hosts, and path patterns.
4. **Re-register** FCM and APNs subscriptions explicitly on first v2 launch.
5. **Roll out** via Google Play and App Store phased release with rollout gates.
6. **Provide** a server-side kill switch (`/config/minSupportedVersion`) because mobile rollback is not possible.

This ADR **supersedes** ADR-0005 if the team confirms the same-app-ID scenario. Until confirmed, both ADRs remain proposed and cover the two possible paths.

## Local Data and Session

After in-place upgrade, the OS keeps the app sandbox intact. v2 can read whatever v1 wrote.

### Auth tokens

Tokens were stored by v1 in Android Keystore / iOS Keychain.

v2 first-launch flow:

1. Read existing token entries using v1 key names.
2. Call `/auth/validate` (or equivalent) with the access token.
3. If valid: rewrite the token into the v2 storage layout, delete v1 entries, continue authenticated.
4. If only refresh token is valid: refresh, rewrite, continue.
5. If nothing is valid: force re-login with a user-facing message ("We refreshed the app, please sign in again").

v1 storage key names and crypto format **must be documented** by the team before v2 code writes the migration. Unknown format at this point.

### Local database

Two options. The team should pick one; **option A is the recommended default**.

#### Option A - Cache-first re-fetch (recommended)

- v2 starts with an empty Room database.
- After login (or session reuse), the repository re-fetches data from the backend.
- Tracking history, addresses, Z-Boxes, notifications are loaded from the server on demand.
- No v1 -> v2 SQL migration code is required.

Pros:

- no risk of schema conflict,
- no dependency on reading v1 Room internals,
- simpler to test,
- aligns with [ADR-0009](0009-offline-sync-matrix.md) "DB is cache, backend is truth".

Cons:

- first v2 launch requires network; if offline, screens show loading/empty states until connectivity returns,
- purely local-only content (drafts, user notes) is lost unless explicitly migrated.

Required work:

- verify that the backend exposes all historical data v1 users expect to see (tracking history, saved addresses, payment methods, etc.),
- identify any purely local-only content and decide per-item whether to migrate or discard.

#### Option B - Schema migration

- v2 opens the v1 Room/SQLite database in read mode.
- Data is copied or transformed into the v2 schema.
- After successful migration, the v1 database file is deleted.

Pros:

- first v2 launch works offline,
- tracking history appears instantly without network.

Cons:

- requires the team to know v1 Room schemas across all previously shipped versions,
- schema mismatch at any level blocks login,
- higher testing surface (many v1 versions x devices x data shapes),
- partial-migration failure states are complex to recover from.

### Preferences (DataStore / SharedPreferences)

Preferences survive in-place upgrade automatically. v2 should:

- define an explicit allow-list of preferences to read from v1,
- ignore unknown/deprecated keys,
- write v2 preferences alongside, then remove the v1 keys once successfully migrated.

## Deep Links and Universal Links

Because it is the same app bundle:

- v1 SMS/email links continue to open v2 on user devices.
- v2 **must** declare intent filters and Associated Domains for **every** scheme, host, and path pattern present in the v1 Android manifest and iOS entitlements.

v1 schemes/hosts to preserve (inventoried from the v1 Android manifest):

- `packeta-app://login` (auth callback, `autoVerify=true`),
- custom scheme `packeta-app://` with all v1 paths,
- legacy `zasilkovna-*` scheme for backwards compatibility,
- `https://mobile.api.packeta.com/...`,
- `https://sysdev.packetery.com/...` (dev/test host),
- default production host/variant `https://<packeta host>/...`,
- path prefixes: generic tracking, `package_share`, `rating` (and variant), `auth` (and variant).

v2 `DeepLinkParser` in `:core:domain` must:

- accept all v1 URL shapes,
- map them to v2 semantic destinations,
- fall back gracefully to a "link not recognized" screen rather than crashing.

## Push Notifications

v2 should treat push channels as **requiring re-registration**:

- clear FCM topic subscriptions from v1 and re-subscribe with v2 topic names,
- request a fresh FCM token and send it to the backend with `X-App-Generation: v2` header,
- on iOS, request a new APNs token on first v2 launch.

Backend should be prepared for a burst of re-registrations during rollout.

## Backend Compatibility

Because some users will receive v2 while others remain on v1 (rollout takes days), the backend must:

- accept v1 and v2 request shapes in parallel during the rollout window,
- identify the client through `X-App-Generation: v1|v2` and `User-Agent` / version headers,
- expose `/config/minSupportedVersion` so v2 can be forced to update if a bad v2 build ships,
- keep backwards-compatible DTOs until v1 adoption falls below an agreed threshold.

Full backend contract is covered by [ADR-0012](0012-backend-api-contract.md).

## Rollout

Mobile rollback is technically impossible - once a user updates, the Store cannot push them back to v1.

### Staged rollout

Google Play:

- Halted production rollout at 1% -> 5% -> 25% -> 50% -> 100%.
- Each step held for at least 48-72 hours and gated on metrics.

App Store:

- Built-in 7-day phased release (automatic ramp-up), pausable at any time.

### Rollout gates

Do not advance the rollout if any of these regress against the v1 baseline:

- crash-free session rate,
- ANR rate (Android),
- first-launch success rate,
- login success rate (sensitive to token migration),
- API 5xx rate,
- support ticket volume,
- Store rating in the last 24h.

Exact thresholds must be agreed with product/QA/ops.

### Kill switch

If v2 ships with a critical bug:

1. Pause rollout in Play Console / App Store Connect.
2. Bump `/config/minSupportedVersion` so affected v2 builds are forced to update once a hotfix ships.
3. Prepare and release hotfix via the usual release flow.

## Store Listing and Brand

Store listing stays:

- same Play Console entry,
- same App Store Connect entry,
- same public name (or updated name, but the listing itself is preserved),
- updated screenshots and description for v2.

Users see v2 as a standard update, not as a "new app". Marketing communication is optional but recommended to highlight major UX changes.

## Analytics Continuity

Because it is the same app, analytics user IDs can be preserved.

- Add a property `app_generation: v1|v2` on every event to allow funnel splitting.
- Keep stable user ID mapping where consented.
- Monitor key funnels (onboarding, login, tracking, payment) split by `app_generation` to detect regressions.

## Consequences

### Positive

- No forced re-login for most users.
- No lost data where backend is the source of truth.
- No coexistence problems: it is one app.
- No store communication campaign is strictly required.
- Marketing and user narrative is simpler.

### Negative

- v1 implementation knowledge becomes a hard dependency (token format, deep-link inventory, preferences keys).
- Bad v2 build cannot be rolled back via the Store.
- Rollout must be genuinely staged and monitored.
- Support volume spike is possible during first 48-72 hours of each rollout step.

### Risks

- Unknown v1 token/crypto formats cause silent auth failure after upgrade.
- Missed v1 deep-link schemes cause broken SMS/email/push links.
- FCM/APNs topic schema mismatch causes missed or duplicated notifications.
- Preferences key drift causes unexpected settings states.

## Implementation Checklist

- [ ] Confirm prod Android `applicationId` and iOS bundle ID with the team.
- [ ] Inventory v1 token storage: keys, crypto format, Android Keystore alias, iOS Keychain service/account.
- [ ] Decide DB strategy: option A (cache-first re-fetch) vs option B (schema migration).
- [ ] Inventory v1 preferences keys and agree the allow-list for v2.
- [ ] Inventory v1 deep-link schemes and path patterns in both Android manifest and iOS Associated Domains.
- [ ] Add `app_generation` dimension to analytics.
- [ ] Add `X-App-Generation` header to all backend requests.
- [ ] Implement FCM/APNs re-subscription on first v2 launch.
- [ ] Implement `/config/minSupportedVersion` check at app start.
- [ ] Define rollout gates and monitoring dashboards.
- [ ] Prepare support FAQ and internal incident playbook.
- [ ] Beta test v2 on devices where v1 is installed (upgrade path test, not fresh install).

## Open Questions

- [ ] **CONFIRM**: will v2 use the same production app ID (`cz.zasilkovna.zasilkovna`) as v1? (If no, ADR-0005 applies instead.)
- [ ] Who documents the v1 token storage format?
- [ ] DB strategy - option A or option B?
- [ ] Which backend endpoints must support both v1 and v2 shapes, and for how long?
- [ ] Exact rollout gate thresholds.
- [ ] Who owns the kill switch decision and rollout pause authority?
- [ ] Support capacity plan for the first rollout week.

## Relationship to ADR-0005

- This ADR (0014) applies **if the team confirms the same production app ID as v1**.
- [ADR-0005](0005-transition-from-v1.md) applies **if the team decides to ship v2 under a new production app ID**.
- Only one scenario can be accepted. The other will be marked `Superseded` once the team decides.

## Related

- [ADR-0005: Transition from v1 (alternative scenario)](0005-transition-from-v1.md)
- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0009: Offline sync matrix](0009-offline-sync-matrix.md)
- [ADR-0010: Release readiness](0010-release-readiness.md)
- [ADR-0012: Backend API contract](0012-backend-api-contract.md)
