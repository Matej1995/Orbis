# ADR 0013: Assets, Resources, and Localization Strategy

## Status

**Proposed - Level B: native export preferred, validation needed** (2026-04-15)

## Context

Packeta v2 uses KMP for data/domain and native UI/ViewModels:

- Android: Compose UI and Android resources.
- iOS: SwiftUI and iOS resources.
- KMP shared code should not own user-facing UI copy by default.

Resources affect:

- localization workflow,
- Android/iOS native tooling,
- accessibility,
- design system parity,
- build complexity,
- SwiftUI ergonomics,
- asset variants and dark mode.

The original idea of putting all strings into `moko-resources` moves the resource runtime into KMP shared code. MOKO and JetBrains Compose Multiplatform resources are valid tools, but the proposed default for Packeta v2 is platform-native resource workflow.

See [Resources, Localization and MOKO Deep Dive](../resources-localization-moko-deep-dive.md).

## Decision

Use platform-native resources by default.

- Android gets `strings.xml`, `plurals.xml`, drawables, and Compose resource access.
- iOS gets String Catalogs / `Localizable.strings` and `Assets.xcassets`.
- Translation tool is the source of truth for localized copy.
- KMP returns semantic keys or typed errors, not localized text.
- KMP resource library is opt-in, not default.

## User-Facing Strings

User-facing UI strings live in platform-native resources.

Shared code can expose:

- domain enum,
- typed error,
- semantic key.

Example:

```kotlin
enum class PackageStatus {
    Delivered,
    InTransit,
    ReadyForPickup
}
```

Platform maps it to native resources:

```kotlin
PackageStatus.Delivered -> R.string.package_status_delivered
```

```swift
PackageStatus.delivered -> String(localized: "package_status_delivered")
```

## `core:resources`

`core:resources` is a small shared contract module, not a full UI resource runtime.

It may contain:

- typed resource keys,
- enum-to-key mapping,
- error-to-key mapping,
- optional brand asset metadata.

It should not contain all UI strings by default.

## Image Assets

Image assets are platform-native build artifacts by default.

Source:

- Figma/design system,
- brand asset repository,
- translation/localization workflow if the asset is localized.

Outputs:

- Android drawables / vector drawables / density assets.
- iOS `Assets.xcassets`.

Rules:

- Avoid text inside images.
- If text in an image is unavoidable, localize/regenerate the image per locale.
- Dark variants must be explicit.
- Brand-critical assets must be reviewed on both platforms.

## Icons

Icons are not automatically shared between Android and iOS.

Default:

- use platform-native icon assets,
- keep naming consistent,
- generate from the same design source.

Pixel-identical brand icons can be shared from the same source asset repository, but still exported into native platform formats.

## KMP Resource Library

Use a KMP resource library only if common code needs to load resources directly.

Examples where it may be valid:

- shared UI,
- shared component library,
- common code that must return formatted text,
- existing MOKO workflow accepted by Android and iOS teams.

Options:

- JetBrains Compose Multiplatform resources,
- MOKO Resources.

This must be decided by a separate spike because MOKO adds Gradle/Xcode resource setup for iOS.

Rule:

> Do not use a KMP resource library for user-facing UI copy by default.

## Translation Workflow

Required decisions:

- translation tool: Crowdin, Lokalise, Phrase, or internal tool,
- source key naming convention,
- export format for Android and iOS,
- owner of copy keys,
- review flow for placeholders/plurals,
- support for Czech/Slovak/English and other locales.

Recommended key pattern:

```text
feature_context_element_state
```

Examples:

```text
auth_otp_error_expired
package_detail_status_delivered
payment_result_button_retry
```

## Testing

Minimum tests/checks:

- Android default resources are complete.
- iOS base localization is complete.
- Shared `ResourceKey` values exist on both platforms.
- Placeholder/plural format is valid.
- Screens support long translations.
- Dark-mode assets exist where required.

## Consequences

### Positive

- Android and iOS keep native tooling.
- iOS can use String Catalogs / asset catalogs.
- KMP stays focused on business semantics.
- Translation workflow can be owned outside KMP build logic.

### Negative

- Native resource files exist on both platforms.
- Requires a strong translation export workflow to avoid drift.
- Shared code cannot directly format localized UI strings by default.

### Risks

- Without a translation tool/owner, resources can diverge.
- Semantic keys can become too generic if naming is not enforced.
- If shared UI is added, this ADR must be revisited.

## Implementation Checklist

- [ ] Define resource key naming convention.
- [ ] Create `core:resources` as typed key contract.
- [ ] Select translation tool/export workflow.
- [ ] Add validation that shared keys exist in Android and iOS resources.
- [ ] Define asset export workflow from design source.
- [ ] Run MOKO spike only if the architecture requires a shared resource runtime.

## Open Questions

- [ ] Which translation tool will Packeta use?
- [ ] Does the tool export Android XML and iOS String Catalogs?
- [ ] Does iOS want String Catalogs as the primary workflow?
- [ ] Is any shared UI or shared resource runtime required?
- [ ] If KMP resource library is needed: JetBrains resources or MOKO?

## Related

- [ADR-0001: KMP shared data/domain](0001-kmp-shared-data-domain.md)
- [ADR-0002: Native UI per platform](0002-native-ui-per-platform.md)
- [ADR-0011: Design system parity](0011-design-system-parity.md)
- [Resources, Localization and MOKO Deep Dive](../resources-localization-moko-deep-dive.md)

## External References

- Android localization docs: <https://developer.android.com/guide/topics/resources/localization>
- Android Compose resources docs: <https://developer.android.com/develop/ui/compose/resources>
- Apple localization overview: <https://developer.apple.com/localization/>
- Apple String Catalog docs: <https://developer.apple.com/documentation/xcode/localizing-and-varying-text-with-a-string-catalog>
- Apple Asset Catalog docs: <https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/>
- JetBrains Compose Multiplatform resources docs: <https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html>
- MOKO Resources docs: <https://github.com/icerockdev/moko-resources>
