# ADR 0012: Backend API Contract, OpenAPI, and Mock Server

## Status

**Proposed - Level A/C: contract direction, backend/auth input required** (2026-04-15)

## Context

The shared KMP data layer integrates with backend APIs once for both Android and iOS. That increases the value of a clear backend contract, but also increases the impact of contract mistakes.

Without a contract:

- Android and iOS can model errors differently,
- mock data can drift from reality,
- token refresh behavior can be duplicated incorrectly,
- UI development is blocked by backend availability,
- API changes can break both platforms at once.

## Decision

Use an explicit backend contract for mobile:

1. OpenAPI as the source of truth for endpoints covered by Packeta v2.
2. DTOs separated from domain models.
3. Standard error envelope.
4. API versioning and compatibility rules.
5. Contract tests.
6. Mock mode / mock server for UI development.
7. Stable staging data for QA.

## Source of Truth

Preferred:

- OpenAPI spec in backend or shared contract repository,
- generated or validated clients/tests from the spec,
- CI check that mock/server behavior matches the contract.

If OpenAPI is incomplete, start with the endpoints needed for onboarding/auth.

## DTO Strategy

Keep DTOs separate from domain models.

```text
API DTO -> mapper -> domain model -> UI state
```

Do not expose raw DTOs to UI or Swift screens.

Codegen decision:

| Variant | Proposal |
|---|---|
| Generate DTOs/API interfaces from OpenAPI | Candidate if the spec is stable and high quality |
| Write DTOs manually based on OpenAPI | MVP default; explicit code and mappers |
| Generate domain models | No; domain should reflect app needs |
| Generate use cases/repositories | No; couples architecture too tightly to backend |

MVP proposal: write DTOs and mappers manually, but run contract tests against OpenAPI. Consider codegen after a spike on one feature.

## Error Envelope

Backend should provide one error response shape.

Example:

```json
{
  "code": "OTP_EXPIRED",
  "message": "OTP has expired",
  "correlationId": "abc-123",
  "details": {
    "retryAfterSeconds": 30
  }
}
```

Mobile maps this to typed errors:

```kotlin
sealed interface AuthError : AppError {
    data object OtpExpired : AuthError
    data object TooManyAttempts : AuthError
    data class Unknown(val correlationId: String?) : AuthError
}
```

Rules:

- UI copy is not taken directly from backend `message`.
- Backend `code` drives error mapping.
- Correlation ID is shown/logged for support when safe.
- Unknown errors have a safe fallback.

## Auth and Token Refresh

The contract must define:

- login/OTP request,
- OTP submit,
- token refresh,
- refresh token expiry,
- logout/revoke,
- 401 behavior,
- device/session model,
- rate limiting and lockout errors.

Shared data layer should centralize token refresh behavior.

## API Versioning

Required decisions:

- app version header,
- API version header or URL strategy,
- compatibility window for v1 and v2,
- deprecation policy,
- feature flag interaction.

## Contract Tests

Minimum tests:

- DTO parsing for success and error bodies,
- mapper tests,
- error-code mapping,
- token refresh flow,
- mock server responses matching OpenAPI,
- compatibility test for critical endpoints.

## Mock Mode

Mock mode should support:

- local UI development without backend,
- deterministic onboarding/auth states,
- error scenarios,
- empty states,
- slow network simulation.

Mock data must be generated from or validated against the same contract for covered endpoints.

## Staging Data

QA needs stable data:

- known user accounts,
- OTP/test login path,
- package states,
- payment sandbox data,
- Z-Box test data if applicable.

## Consequences

### Positive

- Shared data layer has a stable contract.
- Android and iOS use the same error mapping.
- UI can be developed before all backend states are manually prepared.
- Contract changes are visible in CI.

### Negative

- Requires backend ownership.
- OpenAPI/codegen quality must be managed.
- Mock mode adds maintenance.

### Risks

- Incomplete OpenAPI can create false confidence.
- Backend `message` can accidentally leak user-facing copy decisions.
- Contract tests can lag behind real behavior if not wired into CI.

## Implementation Checklist

- [ ] Confirm OpenAPI location and owner.
- [ ] Define standard error envelope.
- [ ] Define auth/token refresh contract.
- [ ] Create DTO + mapper pattern in shared data.
- [ ] Add Ktor MockEngine tests.
- [ ] Add contract test pipeline.
- [ ] Add mock mode for onboarding/auth.

## Open Questions

- [ ] Does the backend already have OpenAPI for required endpoints?
- [ ] Who owns mobile-specific error codes?
- [ ] How is OTP tested in dev/stage?
- [ ] Is mock server owned by backend, mobile, or both?
- [ ] What is the API compatibility window for v1/v2?

## Related

- [ADR-0001: KMP shared data/domain](0001-kmp-shared-data-domain.md)
- [ADR-0003: Config strategy](0003-android-kmp-plugin-single-variant.md)
- [ADR-0004: KMP/Swift API contract](0004-kmp-swift-api-contract.md)
- [ADR-0005: Transition from v1](0005-transition-from-v1.md)
- [ADR-0009: Offline sync matrix](0009-offline-sync-matrix.md)
