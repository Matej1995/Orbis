# ADR 0011: Design System Parity, Accessibility, and Dark Mode

## Status

**Proposed - Level A/C: parity direction, owner input required** (2026-04-15)

## Context

Packeta v2 uses native UI per platform. That means design system implementation is duplicated:

- Android: Compose components and theme.
- iOS: SwiftUI components and theme.

Without explicit rules, the platforms can drift in spacing, typography, colors, states, copy, accessibility, and dark mode.

## Decision

Use shared design tokens as a source of truth, but implement components natively on each platform.

Do not share UI components through KMP by default.

## Token Source of Truth

Tokens should cover:

- colors,
- typography,
- spacing,
- radius,
- elevation/shadows where relevant,
- motion durations,
- icon sizes,
- component state names.

Recommended MVP workflow:

```text
Figma tokens / design documentation
  -> Android Compose theme
  -> iOS SwiftUI theme
```

Add automation when tokens change often. Manual sync is acceptable for MVP if ownership is clear.

## Platform Implementation

Android:

- `:core:ui` contains Compose theme and shared Android components.
- Feature UI modules depend on `:core:ui`.
- Android UI modules are not exported to iOS.

iOS:

- `iosApp/DesignSystem` contains SwiftUI theme and components.
- Feature screens use the iOS design system directly.
- iOS components are not generated from Android code.

## Dark Mode

Dark mode must be supported through tokens, not ad-hoc per-screen colors.

Rules:

- every semantic color has light and dark values,
- brand colors must be checked for contrast,
- icons/assets with dark variants are defined explicitly,
- screenshots should include both themes for critical screens.

## Accessibility

Accessibility is required from MVP.

Minimum checklist:

- text supports dynamic type / font scaling,
- touch targets meet platform guidelines,
- contrast meets accessibility requirements,
- loading and error states are announced,
- screen readers have meaningful labels,
- decorative images are not announced,
- forms expose validation errors clearly,
- focus order is reasonable.

Platform specifics:

- Android: TalkBack, content descriptions, semantics.
- iOS: VoiceOver, accessibility labels, traits, Dynamic Type.

## Motion

Motion should be platform-native unless product requires identical behavior.

Rules:

- respect reduced motion,
- avoid critical information hidden only in animation,
- keep state transitions understandable without animation.

## Copy and Tone

Copy must be consistent, but it does not have to be stored in KMP.

Rules:

- user-facing copy comes from platform-native resources by default,
- semantic keys can be shared,
- platform-specific phrasing is allowed when platform UX needs it,
- translation workflow owns the final localized text.

See [ADR-0013](0013-assets-localization-strategy.md).

## Figma to Implementation Sync

Required ownership:

- design token owner,
- Android implementation owner,
- iOS implementation owner,
- review process for token changes,
- screenshot/a11y review for critical screens.

## Consequences

### Positive

- UI stays native.
- Design decisions are still shared through tokens.
- Platform teams can use idiomatic components.
- Accessibility can follow platform expectations.

### Negative

- Components are implemented twice.
- Token drift is possible without ownership.
- Screenshot/a11y testing needs platform-specific setup.

### Risks

- Figma may diverge from implementation.
- Copy can drift if translation workflow is weak.
- Dark mode can regress without visual tests.

## Open Questions

- [ ] Who owns design tokens?
- [ ] Will tokens be exported automatically or copied manually for MVP?
- [ ] Which screens need screenshot tests first?
- [ ] What is the minimum supported dynamic type/font scale?
- [ ] Is a design system showcase app required?

## Related

- [ADR-0002: Native UI per platform](0002-native-ui-per-platform.md)
- [ADR-0004: KMP/Swift API contract](0004-kmp-swift-api-contract.md)
- [ADR-0013: Assets, resources, and localization strategy](0013-assets-localization-strategy.md)
