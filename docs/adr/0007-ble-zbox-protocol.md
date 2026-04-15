# ADR 0007: BLE Z-Box Protocol and Test Matrix

## Status

**Draft - Level C: real Z-Box protocol input required** (2026-04-15)

## Context

Z-Box interaction is platform-sensitive and hardware-dependent. Android and iOS have different BLE APIs, permission models, background behavior, and reliability characteristics.

The architecture can be proposed now, but the protocol cannot be approved without the real specification.

## Decision

Use shared KMP code for business-level Z-Box flow and state machine. Keep BLE transport native.

Shared code may own:

- Z-Box domain models,
- open/pairing state machine,
- backend API calls,
- command validation,
- error mapping,
- retry policy,
- audit event model.

Native platform code owns:

- Android BluetoothGatt implementation,
- iOS CoreBluetooth implementation,
- permission UX,
- scanning behavior,
- connection lifecycle,
- platform-specific timeouts and background constraints.

## Architecture

```text
Native UI
  -> Native ViewModel
  -> ZBoxFacade / use case
  -> Shared Z-Box state machine
  -> Native BLE transport
  -> Z-Box hardware
```

The shared layer defines commands and expected states. The native transport performs the actual BLE read/write/notify operations.

## Protocol Status

This section is proposed architecture, not an approved specification of the existing Z-Box protocol. Before this ADR can become `Accepted`, the team must provide:

| Area | Status |
|---|---|
| Current protocol version | Required input |
| Firmware versions in the field | Required input |
| Crypto scheme | Required input |
| Pickup/open token format | Required input |
| Backward compatibility policy | Required input |
| Test lab coverage | Required input |

## Protocol Design Principles

- Backend issues the authorization/token for an operation.
- The app does not invent permission to open a box locally.
- BLE command format must be versioned.
- Device response must be mapped to typed domain errors.
- Sensitive data must not be logged.
- Failed operations must be auditable.
- Retry policy must avoid duplicate open commands.

## Crypto and Security

The team must confirm:

- whether v1 uses encryption/signature for BLE commands,
- whether tokens are short-lived,
- whether tokens are bound to user/package/box,
- whether replay protection exists,
- whether firmware supports protocol version negotiation,
- whether external security review is required.

Until these answers exist, the crypto section is not a decision.

## Platform Implementation

Android:

- runtime Bluetooth permissions,
- location/Bluetooth behavior by Android version,
- BluetoothGatt connection lifecycle,
- foreground/background limitations,
- device-specific BLE instability.

Android manifest permissions (inherited from v1, verified present):

- `BLUETOOTH_SCAN` with `neverForLocation` flag (Android 12+),
- `BLUETOOTH_CONNECT` (Android 12+),
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` (Android < 12 legacy scan requirement).

iOS:

- CoreBluetooth central manager lifecycle,
- permission UX,
- background behavior,
- state restoration policy,
- device discovery behavior.

iOS Info.plist requirements:

- `NSBluetoothAlwaysUsageDescription` is required; app submission is rejected without it.

## Test Matrix

Minimum test dimensions:

| Area | Examples |
|---|---|
| Devices | several Android vendors, iPhone models, supported OS versions |
| Hardware | Z-Box Gen 1/2/3 if applicable |
| Network | online, slow network, backend timeout |
| BLE state | Bluetooth off, permission denied, device not found, connection lost |
| Operation state | success, already opened, expired token, invalid token, backend rejects |
| App lifecycle | background, foreground, kill/resume during flow |
| Nearby boxes | multiple boxes in range, choose correct ID |

## Fallback Scenarios

The UX must define what happens when:

- BLE is unavailable,
- the box cannot be found,
- the operation times out,
- token expires,
- backend state differs from box/device state,
- user needs support.

## Consequences

### Positive

- Business behavior is shared and testable.
- Platform BLE complexity stays native.
- Protocol/state-machine decisions are explicit.

### Negative

- Requires native BLE implementation on both platforms.
- Requires real hardware and test lab access.
- Shared state machine cannot be finalized without protocol details.

### Risks

- Missing protocol specification can block implementation.
- Device-specific BLE behavior can cause inconsistent UX.
- Incorrect retry logic can trigger unsafe or duplicate operations.

## Open Questions

- [ ] Who owns the real Z-Box protocol specification?
- [ ] Which firmware versions must v2 support?
- [ ] What is the crypto scheme?
- [ ] What is the token format and lifetime?
- [ ] Is a security audit required?
- [ ] What hardware is available for testing?

## Related

- [ADR-0008: Threat model](0008-threat-model.md)
- [ADR-0009: Offline sync matrix](0009-offline-sync-matrix.md)
