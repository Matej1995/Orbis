# ADR 0010: Release Readiness, Rollout, and Hotfix Flow

## Status

**Proposed - Deferred / Level C: release ownership input required** (2026-04-15)

## Context

Release process is outside repository scaffolding scope, but it must exist before internal distribution and store release.

Packeta v2 needs a repeatable release path for:

- Android and iOS builds,
- signing,
- crash symbolication,
- staged rollout,
- hotfixes,
- forced update,
- rollback communication.

## Decision

Define release readiness before the first public release. Keep the initial scaffold focused on the minimum CI/build foundation.

## Versioning

Android:

```text
versionName: 2.x.y
versionCode: monotonically increasing integer
```

iOS:

```text
CFBundleShortVersionString: 2.x.y
CFBundleVersion: monotonically increasing build number
```

Both platforms must expose version/build in debug/support diagnostics.

## Pre-Release Checklist

- [ ] All required ADRs accepted or explicitly deferred.
- [ ] No `TODO-BEFORE-RELEASE` comments.
- [ ] Crash reporting configured.
- [ ] Symbolication configured: Android mapping files, iOS dSYM.
- [ ] Privacy manifests / Play Data Safety reviewed.
- [ ] App icons, names, bundle IDs, and store metadata confirmed.
- [ ] Backend compatibility confirmed.
- [ ] Feature flags configured.
- [ ] Forced update mechanism available if required.
- [ ] Support/debug diagnostics available.
- [ ] Release notes prepared.

## Rollout Strategy

Recommended order:

1. local/dev builds,
2. internal QA,
3. Firebase App Distribution / TestFlight internal,
4. closed testing,
5. staged rollout,
6. full rollout.

Open testing is optional.

## Rollout Gates

Do not increase rollout if:

- crash-free sessions are below agreed threshold,
- auth/login errors spike,
- payment errors spike,
- Z-Box critical errors spike,
- backend error rate increases,
- support reports a release blocker.

Exact thresholds must be defined with product/QA/ops.

## Forced Update

Forced update should be controlled by backend/remote config:

- minimum supported app version,
- recommended version,
- hard block vs soft prompt,
- platform-specific store link,
- localized copy.

Use only for critical compatibility/security issues.

## Rollback Strategy

Mobile rollback usually means:

- stop rollout,
- disable feature flag,
- publish hotfix,
- update backend compatibility,
- communicate through support/release channels.

Do not rely on users downgrading the app.

## Hotfix Flow

Hotfix must define:

- branch source,
- owner,
- review requirement,
- QA scope,
- signing/distribution path,
- release notes,
- post-release monitoring window.

## Post-Release Review

After release, review:

- crash rate,
- key funnel metrics,
- support tickets,
- backend errors,
- feature flag behavior,
- rollout incidents.

## Consequences

### Positive

- Release risk is explicit.
- Rollout can stop before full blast radius.
- Hotfix path is known before an incident.

### Negative

- Requires CI, QA, ops, and product alignment.
- Adds process before release.
- Requires monitoring discipline.

### Risks

- False-positive monitoring can stop a healthy rollout.
- Missing symbols can make crash reports unusable.
- Store review timing can slow hotfixes.

## Open Questions

- [ ] Release cadence: weekly, bi-weekly, monthly?
- [ ] Who is release manager?
- [ ] Which crash/monitoring stack is final?
- [ ] What are rollout thresholds?
- [ ] Is forced update required for v2 launch?

## Related

- [ADR-0005: Transition from v1](0005-transition-from-v1.md)
- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0012: Backend API contract](0012-backend-api-contract.md)
