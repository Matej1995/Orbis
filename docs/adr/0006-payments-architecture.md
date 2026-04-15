# ADR 0006: Payments Architecture

## Status

**Proposed - Deferred / Level C: provider and compliance input required** (2026-04-15)

## Context

Packeta v2 must support payment flows such as:

- PayU,
- Google Pay,
- Apple Pay.

Payments are high-risk because they involve money, compliance, user trust, and backend reconciliation.

## Decision

Payments are online-only and backend-authoritative.

Principles:

1. The app never stores raw card data.
2. The app does not decide final payment state.
3. Backend is the source of truth.
4. Payment operations are idempotent.
5. All payment events are auditable without logging sensitive data.

## Architecture

High-level flow:

```text
Native UI
  -> Native ViewModel
  -> PaymentFacade / use case
  -> Backend creates payment intent/session
  -> Native payment SDK handles user interaction
  -> App receives SDK result
  -> Backend confirms final state
  -> App renders backend-confirmed state
```

KMP shared code may own:

- payment use cases,
- backend API calls,
- DTO/domain mapping,
- error mapping,
- idempotency keys,
- state machine for app-level payment flow.

Native platform code owns:

- Google Pay SDK integration,
- Apple Pay integration,
- PayU native SDK/browser handoff if platform-specific,
- platform permission/UI handling.

## Server Authority

If the app thinks a payment succeeded but backend does not confirm it, the app must not mark the order as paid.

The app may show:

- pending verification,
- failed payment,
- retry action,
- support contact flow.

It must not unlock paid-only actions based only on local SDK result.

## Idempotency

Every payment operation that can be retried must use an idempotency key.

Examples:

- create payment,
- confirm payment,
- retry payment,
- cancel payment.

Retries must not create duplicate charges.

## Error Handling

Payment errors must distinguish:

- user cancelled,
- network failure,
- SDK failure,
- backend rejected,
- payment pending,
- payment failed,
- payment state unknown.

Unknown state must trigger backend reconciliation, not local guessing.

## Logging and Monitoring

Log:

- payment flow step,
- non-sensitive error code,
- backend correlation ID,
- idempotency key hash when available,
- app version and platform.

Do not log:

- card number,
- payment tokens,
- full user PII,
- raw SDK payloads if they contain sensitive data.

## Testing

Required test areas:

- backend contract tests,
- retry/idempotency tests,
- network failure during payment,
- app kill/resume during payment,
- SDK cancel/failure,
- backend pending/success/failure reconciliation,
- Android and iOS sandbox payments.

## Consequences

### Positive

- Avoids local payment-state authority.
- Reduces duplicate-charge risk.
- Keeps compliance boundary clear.
- Makes support and reconciliation possible.

### Negative

- Requires backend support.
- Requires more explicit state handling.
- Native payment SDK flows must be tested separately per platform.

### Risks

- Bad error handling can show incorrect payment status.
- Missing idempotency can cause duplicate payments.
- Missing reconciliation can leave users in unknown state.

## Open Questions

- [ ] Which PayU integration mode will v2 use?
- [ ] Are merchant IDs already available for v2?
- [ ] What is the required 3DS UX?
- [ ] How are refunds represented in the API?
- [ ] What is the backend reconciliation contract?
- [ ] Is external security/compliance review required?

## Related

- [ADR-0004: KMP/Swift API contract](0004-kmp-swift-api-contract.md)
- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0009: Offline sync matrix](0009-offline-sync-matrix.md)
