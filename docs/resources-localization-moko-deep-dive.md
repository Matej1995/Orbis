# Resources, Localization, and MOKO Deep Dive

> Status: draft (2026-04-15)
> Goal: describe the resources workflow for Packeta v2 and compare native translation-tool output with MOKO Resources.

## Launch Languages

Packeta v2 ships with **5 languages at launch**: **CS, EN, SK, HU, RO**.

Android locale folders:

```text
values/        - default (EN)
values-cs/
values-sk/
values-hu/
values-ro/
```

iOS `.xcstrings` supports all 5 locales as language variants of each key.

## Problem

Packeta v2 needs to avoid:

- writing the same string twice manually,
- Android/iOS resource drift,
- broken placeholders and plurals,
- inconsistent icons/assets,
- unnecessary Gradle/Xcode build coupling.

At the same time, the architecture keeps UI native:

- Android uses Compose and Android resources.
- iOS uses SwiftUI and iOS resources.
- KMP is primarily data/domain.

The key decision is whether resources are native platform outputs or a shared KMP runtime.

## Model A: Translation Tool Exports Native Resources

Source of truth:

```text
Crowdin / Lokalise / Phrase / internal translation tool
```

Outputs:

```text
Android:
  app/src/main/res/values/strings.xml
  app/src/main/res/values-cs/strings.xml
  app/src/main/res/values-sk/strings.xml

iOS:
  Localizable.xcstrings
  Assets.xcassets
```

Model A means:

- strings are stored once in the translation tool,
- Android receives Android-native resources,
- iOS receives iOS-native resources,
- no custom parser is required in the app,
- platform tooling stays native.

KMP does not return localized strings. It returns semantic values:

```kotlin
enum class PackageStatus {
    Delivered,
    InTransit,
    ReadyForPickup
}
```

Android/iOS map those values to native resources.

## Model B: MOKO Resources as Shared KMP Resource Runtime

Source of truth:

```text
commonMain/moko-resources
```

Example:

```text
core/resources/src/commonMain/moko-resources/base/strings.xml
core/resources/src/commonMain/moko-resources/cs/strings.xml
core/resources/src/commonMain/moko-resources/sk/strings.xml
core/resources/src/commonMain/moko-resources/images/logo.svg
core/resources/src/commonMain/moko-resources/images/logo-dark.svg
```

Model B means:

- resources physically live in the shared KMP module,
- MOKO generates typed `MR`,
- Android/iOS access resources through MOKO APIs/generated bridge,
- the KMP module becomes part of the resource runtime.

Example:

```kotlin
MR.strings.error_no_internet
MR.images.zbox_marker
```

## What MOKO Solves

MOKO directly addresses:

- one shared resource location,
- generated typed resource access,
- common string/image references,
- common resources for Android and iOS,
- useful API when common code or shared UI needs resources.

It is stronger when:

- there is shared UI,
- common code must format user-facing text,
- the team accepts generated `MR` as the resource API,
- iOS build integration is accepted.

## Why MOKO Is Not the Default Here

Packeta v2 currently proposes:

```text
KMP = data/domain
Presentation = native Compose + native SwiftUI
```

If all UI strings and assets move to MOKO, the resource boundary moves back into shared KMP code.

Consequences:

- iOS no longer primarily uses native String Catalog workflow.
- SwiftUI resource access goes through generated KMP/MOKO bridge.
- Xcode build must integrate copied/generated resources.
- MOKO documentation includes Xcode setup such as resource copy tasks and localization configuration.
- Translation tooling must export into the MOKO XML structure.

These are architectural costs.

## Build Impact of MOKO

MOKO changes iOS build flow; it is not only a dependency in `libs.versions.toml`.

Expected setup:

- Gradle plugin generates `MR` from `commonMain/moko-resources`.
- iOS executable/static XCFramework may need a build phase that copies resources into the app.
- Static XCFramework setup can require `copyResources...XCFrameworkToApp` and `configureCopyXCFrameworkResources(...)`.
- Xcode setup may require handling user script sandboxing.
- `Info.plist` must list localizations in `CFBundleLocalizations`.

Conclusion: selecting MOKO makes resource handling part of the iOS build architecture.

## Comparison

| Criterion | Translation tool -> native outputs | MOKO Resources |
|---|---|---|
| One source of strings | Yes, in translation tool | Yes, in shared KMP resources |
| Android native workflow | Yes | Partly, through MOKO/generated resources |
| iOS native workflow | Yes, String Catalogs / asset catalogs | Partly, through MOKO framework/bundle bridge |
| Custom parser | No, if the translation tool exports native formats | No, MOKO plugin generates `MR` |
| Shared code can access resources | Only semantic keys by default | Yes |
| SwiftUI ergonomics | Native | Must be validated |
| Build complexity | Lower | Higher |
| Plurals | Native Android/iOS | MOKO plural API |
| Image assets | Platform-native export | Shared MOKO images, including scale/dark variants |
| Fits native UI strategy | Strong fit | Needs justification |

## Practical Workflow with Translation Tool

Adding a new string:

1. Add key in translation tool.
2. Translation tool exports Android XML and iOS String Catalogs.
3. Android uses `R.string.key`.
4. iOS uses `String(localized: "key")`.
5. If shared code needs to reference the state, it returns a typed key/enum, not the final text.

Example keys:

```text
error_no_internet
package_status_delivered
button_cancel
button_done
payment_failed_try_again
```

Changing a translation:

1. Change the text in the translation tool.
2. Export native resources.
3. No KMP rebuild is conceptually required unless key contracts changed.

Assets:

```text
Figma / brand asset repository
  -> Android drawables
  -> iOS Assets.xcassets
```

## Practical Workflow with MOKO

Adding a string:

```text
core/resources/src/commonMain/moko-resources/base/strings.xml
core/resources/src/commonMain/moko-resources/cs/strings.xml
```

Generated usage:

```kotlin
MR.strings.error_no_internet
```

SwiftUI can use MOKO resource IDs/bundles according to MOKO documentation.

Adding an image:

```text
core/resources/src/commonMain/moko-resources/images/zbox_marker.svg
core/resources/src/commonMain/moko-resources/images/zbox_marker-dark.svg
```

MOKO can also support bitmap scale suffixes such as `@1x`, `@2x`, `@3x`.

## When to Use MOKO

MOKO is a candidate if:

- the project requires one KMP resource runtime,
- iOS is comfortable with generated `MR` / `StringDesc` flow,
- `.xcstrings` is not the primary iOS workflow,
- the project prefers less custom export tooling,
- shared UI or shared components are expected,
- common code needs to load strings/images,
- the translation tool exports well into the MOKO XML structure.

## When Not to Use MOKO by Default

MOKO is not the right default if:

- UI stays native and iOS wants clean SwiftUI/Xcode workflow,
- product/design/localization workflow expects iOS String Catalogs,
- assets should live in `Assets.xcassets`,
- the project requires minimal Gradle/Xcode coupling,
- shared code should not own UI copy,
- the first project template should avoid extra build risk.

## Recommendation for Packeta v2

Default:

```text
Translation tool as source of truth
        |
        v
Android native resources + iOS native resources
```

KMP shared:

```text
ResourceKey / enum / typed error
```

Not default:

```text
localized UI string returned from common code
```

MOKO status:

```text
MOKO = alternative after spike, not default without validation.
```

Use MOKO if a spike proves:

- good iOS ergonomics,
- stable Xcode/Gradle build,
- simple translation workflow into MOKO structure,
- readable SwiftUI usage,
- typed shared resources are worth losing native `.xcstrings` / `.xcassets` as the primary workflow.

## Recommended Spike

Prepare two small prototypes.

Prototype A: translation tool / native outputs

```text
5 strings:
- error_no_internet
- package_status_delivered
- button_cancel
- button_done
- payment_failed_try_again

2 plurals:
- packages_count
- zboxes_count

2 assets:
- logo
- zbox_marker
```

Outputs:

```text
Android strings.xml / plurals.xml / drawables
iOS Localizable.xcstrings / Assets.xcassets
```

Prototype B: MOKO

Put the same strings/assets into:

```text
commonMain/moko-resources
```

Verify:

- Android Compose usage,
- SwiftUI usage,
- plural handling,
- dark image variant,
- iOS build,
- resource copy into iOS app,
- localization on a real device,
- translation tool export into the required structure.

## Meeting Questions

1. Which translation tool does Packeta use today?
2. Can it export Android XML and iOS String Catalogs?
3. Does iOS want `.xcstrings` as the primary workflow, or is MOKO `MR` acceptable?
4. Will Packeta v2 include shared UI / Compose Multiplatform screens?
5. Does common KMP code need to localize text itself, or are semantic keys enough?
6. Who owns copy keys and translation workflow?
7. Can Android and iOS copy differ when platform tone requires it?
8. Are platform-native icons or pixel-identical brand icons required?
9. How much Gradle/Xcode coupling is acceptable in the resource layer?

## Short Team Summary

> MOKO removes resource duplication by making the KMP shared module the resource runtime for both platforms. The alternative is one translation source of truth with export into native Android/iOS resources. For native Compose + SwiftUI, the default proposal is native output through a translation tool. Validate MOKO through a spike only if shared resource runtime remains required.

## External References

- MOKO Resources: <https://github.com/icerockdev/moko-resources>
- JetBrains Compose Multiplatform resources: <https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html>
- Android localization: <https://developer.android.com/guide/topics/resources/localization>
- Android Compose resources: <https://developer.android.com/develop/ui/compose/resources>
- Apple localization: <https://developer.apple.com/localization/>
