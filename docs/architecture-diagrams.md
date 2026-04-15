# Packeta v2.0 - Architecture Diagrams

> Status: draft (2026-04-15)
> Goal: visual map of how KMP shared code, native presentation layers, backend, config, resources, and v1 -> v2 transition fit together.

## 1. High-Level Architecture

```mermaid
flowchart TB
    subgraph Android["Android app"]
        AndroidUI["Jetpack Compose UI"]
        AndroidVM["Android ViewModels"]
        AndroidPlatform["Android platform services\nFCM, Google Maps, BLE, PayU, Google Pay"]
    end

    subgraph IOS["iOS app"]
        IOSUI["SwiftUI UI"]
        IOSVM["Swift ViewModels\n@Observable / ObservableObject"]
        IOSPlatform["iOS platform services\nAPNs, MapKit, CoreBluetooth, PayU, Apple Pay"]
    end

    subgraph Shared["KMP Shared"]
        Facades["Feature Facades\nTrackingFacade, AuthFacade, ZBoxFacade"]
        Domain["Domain\nUse cases, entities, errors, repository interfaces"]
        Data["Data\nRepository impl, Ktor APIs, Room DAOs, mappers"]
        Core["Core\nResult, AppError, AppConfig, logging, resources"]
    end

    Backend["Packeta Backend"]
    LocalStorage["Local storage\nRoom, DataStore, Keystore / Keychain"]

    AndroidUI --> AndroidVM
    AndroidVM --> Domain
    AndroidVM -. optional .-> Facades
    AndroidVM --> AndroidPlatform

    IOSUI --> IOSVM
    IOSVM --> Facades
    IOSVM --> IOSPlatform

    Facades --> Domain
    Domain --> Data
    Data --> Backend
    Data --> LocalStorage
    Data --> Core
    Domain --> Core
    Facades --> Core

    AndroidPlatform -. native transport / SDK callbacks .-> Data
    IOSPlatform -. native transport / SDK callbacks .-> Data
```

### What This Shows

This diagram shows the main ownership boundary. Android and iOS own their UI, ViewModels, navigation, and platform SDK integrations. KMP owns the shared feature API, domain rules, data access, API mapping, caching, and common error/config contracts.

The iOS app calls shared code through feature facades. Android can call domain use cases directly, but it may also use the same facade pattern where that makes feature boundaries clearer. Platform services such as BLE, maps, push, and payments stay native because their APIs, permissions, lifecycle, and UX differ per platform.

### Key Takeaways

- UI and ViewModels are native.
- iOS calls shared code through feature facades.
- Android can call use cases directly; it may also use facades to align feature boundaries.
- Data/domain logic, API mapping, error mapping, and cache live in KMP.
- Platform SDK integrations stay native.

## 2. Module Dependency Graph

```mermaid
flowchart LR
    AndroidApp[":androidApp"]
    Shared[":shared\nXCFramework umbrella"]

    CoreDomain[":core:domain"]
    CoreData[":core:data"]
    CoreConfig[":core:config"]
    CoreResources[":core:resources"]
    CoreUi[":core:ui\nAndroid only"]

    FeatureDomain[":feature:x:domain"]
    FeatureData[":feature:x:data"]
    FeatureUi[":feature:x:ui\nAndroid only"]

    IOSApp["iosApp\nSwiftUI"]

    AndroidApp --> CoreUi
    AndroidApp --> FeatureUi
    AndroidApp --> CoreConfig

    FeatureUi --> FeatureDomain
    FeatureUi --> CoreUi

    Shared --> FeatureDomain
    Shared --> FeatureData
    Shared --> CoreDomain
    Shared --> CoreData
    Shared --> CoreConfig
    Shared --> CoreResources

    IOSApp --> Shared

    FeatureData --> FeatureDomain
    FeatureData --> CoreData
    FeatureData --> CoreConfig

    CoreData --> CoreDomain
    CoreData --> CoreConfig
    CoreData --> CoreResources

    FeatureDomain --> CoreDomain
    FeatureDomain --> CoreResources

    CoreUi -. "forbidden in shared export" .-> Shared
    FeatureUi -. "forbidden in shared export" .-> Shared
```

### What This Shows

This diagram describes compile-time module dependencies, not runtime data flow. The Android app depends on Android UI modules and the shared KMP modules it needs. The iOS app consumes only the `:shared` umbrella framework.

The important rule is that Android-only UI modules are not exported to iOS. `:core:ui` and `:feature:x:ui` are Compose-only modules. KMP domain/data modules can be part of the iOS framework; Android UI modules cannot.

### Dependency Rules

- `domain` must not depend on `data`.
- `data` implements repository interfaces from `domain`.
- `ui` modules are Android-only and must not be part of the iOS `:shared` export.
- `:shared` is the umbrella module for the iOS XCFramework.
- `:core:config` is a shared contract; values are supplied by the app module through runtime injection.

## 3. Runtime Data Flow

```mermaid
sequenceDiagram
    participant User
    participant NativeUI as Native UI
    participant VM as Native ViewModel
    participant Facade as Feature Facade
    participant UseCase as Use Case
    participant Repo as Repository
    participant DB as Room cache
    participant API as Backend API

    User->>NativeUI: action / screen open
    NativeUI->>VM: event

    alt iOS
        VM->>Facade: loadPackage(id)
        Facade->>UseCase: invoke(id)
    else Android
        VM->>UseCase: invoke(id)
    end

    UseCase->>Repo: getPackage(id)
    Repo->>DB: read cached data
    DB-->>Repo: cached result
    Repo-->>UseCase: cached domain model
    UseCase-->>VM: state update
    VM-->>NativeUI: render cached state

    Repo->>API: refresh in background
    API-->>Repo: fresh DTO
    Repo->>Repo: map DTO -> domain
    Repo->>DB: persist fresh data
    DB-->>Repo: changed rows / Flow update
    Repo-->>UseCase: fresh domain model
    UseCase-->>VM: state update
    VM-->>NativeUI: render fresh state
```

### What This Shows

This sequence shows a typical cache-first read flow. A user action reaches a native screen and native ViewModel. iOS goes through a feature facade before calling the shared use case. Android can call the use case directly.

The repository first reads cached data and returns it to the UI so the screen can render quickly. Then it refreshes from backend, maps the DTO to a domain model, persists it, and emits the updated state. This flow is useful for package lists, package detail, and other server-owned read models.

### Exceptions

Payments, Z-Box open, and auth do not follow this flow blindly. Those flows are online-only or have stricter backend/device confirmation rules.

## 4. Environment / Config Flow

```mermaid
flowchart TB
    subgraph Android["Android"]
        Gradle["Gradle product flavor\ndev / stage / prod"]
        BuildConfig["Android BuildConfig"]
        AndroidConfig["AndroidAppConfig : AppConfig"]
    end

    subgraph IOS["iOS"]
        Xcconfig["xcconfig\ndev / stage / prod"]
        InfoPlist["Info.plist values"]
        IOSConfig["IosAppConfig : AppConfig"]
    end

    AppConfig["AppConfig interface\n:core:config"]
    Koin["DI graph / app scope"]
    HttpClient["HttpClientFactory"]
    Analytics["AnalyticsTracker"]
    FeatureFlags["FeatureFlagRepository"]

    Gradle --> BuildConfig --> AndroidConfig
    Xcconfig --> InfoPlist --> IOSConfig

    AndroidConfig --> AppConfig
    IOSConfig --> AppConfig

    AppConfig --> Koin
    Koin --> HttpClient
    Koin --> Analytics
    Koin --> FeatureFlags
```

### What This Shows

This diagram shows how environment-specific values enter shared KMP code. Android can use Gradle product flavors and `BuildConfig`. iOS can use `xcconfig` and `Info.plist`. Both platforms adapt those values into the same shared `AppConfig` interface.

Shared modules depend only on the `AppConfig` contract. They do not know whether the value came from Android Gradle, Xcode, CI, or local developer config.

### Why Runtime Config Injection

The Android-KMP library plugin is single-variant for shared libraries. Shared modules do not have product flavors. Environment values must not be hardcoded in shared code. The app module supplies them through `AppConfig`.

## 5. iOS Public API Boundary

```mermaid
flowchart LR
    SwiftVM["Swift ViewModel"]
    Facade["TrackingFacade\npublic KMP API"]
    UseCases["Internal use cases"]
    Repos["Repository interfaces"]
    DataImpl["Repository impl"]

    SwiftVM --> Facade
    Facade --> UseCases
    UseCases --> Repos
    DataImpl --> Repos

    SwiftVM -. should not depend on .-> UseCases
    SwiftVM -. should not depend on .-> Repos
    SwiftVM -. should not depend on .-> DataImpl
```

### What This Shows

This diagram defines the public API boundary for iOS. Swift ViewModels should call a stable feature facade, for example `TrackingFacade`, instead of reaching into individual use cases, repository interfaces, or data implementations.

The facade is not a ViewModel. It is a Swift-facing API layer that hides Kotlin implementation details and keeps the exported API smaller, more stable, and easier to review.

### Rule

iOS depends on feature facades, not internal use cases or repository implementations.

## 6. v1 -> v2 Transition

```mermaid
flowchart TB
    V1["Packeta v1\nexisting app ID / bundle ID"]
    V2["Packeta v2\nnew app ID / bundle ID"]
    Store["Store listing / adoption path"]
    Backend["Backend\nsupports v1 + v2"]
    Push["Push routing\nFCM/APNs registrations"]
    Links["Deep links / universal links"]
    Support["Support / communication"]

    V1 --> Backend
    V2 --> Backend
    Store --> V2
    Backend --> Push
    Push --> V1
    Push --> V2
    Links --> V1
    Links --> V2
    Support --> V1
    Support --> V2

    V1 -. no local storage migration if app IDs differ .-> V2
```

### What This Shows

This diagram explains why v1 -> v2 is not a local migration problem if v2 uses a new app ID / bundle ID. The OS treats v2 as a separate app, so it cannot see v1 local database, preferences, or tokens.

The transition therefore needs product and backend coordination: store adoption path, fresh login, backend support for both app generations, push routing, deep-link routing, and support communication.

### Impact

- v2 is treated as a new app if app IDs differ.
- Local v1 database/preferences/tokens are not visible to v2.
- User needs fresh login.
- Transition needs backend, push, deep-link, store, and support strategy.

## 7. First Template Scope

```mermaid
flowchart TB
    Template["Orbis -> Packeta v2 template"]

    Modules["Module structure\ncore + feature + shared + iosApp"]
    Config["Runtime config\nAppConfig dev/stage/prod"]
    SwiftBridge["iOS bridge\nfacades + SKIE spike"]
    Network["Network foundation\nKtor + error mapping + mock mode"]
    Storage["Storage foundation\nRoom KMP + DataStore"]
    Resources["Resources\nnative export + ResourceKey contract"]
    Tests["Basic tests\ncommonTest + Android unit"]
    CI["Basic CI\nbuild + tests"]

    Template --> Modules
    Template --> Config
    Template --> SwiftBridge
    Template --> Network
    Template --> Storage
    Template --> Resources
    Template --> Tests
    Template --> CI
```

### What This Shows

This diagram defines what the Orbis-based template should prove first. The template should establish the project structure, build wiring, runtime config, iOS bridge, networking foundation, storage foundation, resource contract, tests, and basic CI.

It keeps feature-specific topics out of the first scaffold. Payments, BLE, full release readiness, and the complete security program remain documented in ADRs, but they are outside the first onboarding/auth foundation.

### Build First

Build the project foundation before implementing product-heavy flows. Payments, BLE, and release readiness are important, but they are not required for the first onboarding/auth scaffold.

## 8. Resources / Localization Workflow

```mermaid
flowchart TB
    TranslationTool["Translation tool\nCrowdin / Lokalise / Phrase"]
    DesignSource["Design / brand source\nFigma, icon set, asset repository"]

    AndroidResources["Android native resources\nstrings.xml, plurals.xml,\ndrawables"]
    IOSResources["iOS native resources\nString Catalogs,\nAssets.xcassets"]

    KMPKeys["KMP core:resources\nResourceKey, typed errors,\nenum-to-key mapping"]

    AndroidUI["Android Compose UI\nuses R.string / resources"]
    IOSUI["iOS SwiftUI\nuses String(localized:) / asset catalogs"]

    SharedDomain["Shared domain/data\nreturns semantic values\nnot localized text"]

    MOKOSpike["Optional MOKO / KMP resources spike\nonly if shared resource runtime is needed"]

    TranslationTool --> AndroidResources
    TranslationTool --> IOSResources

    DesignSource --> AndroidResources
    DesignSource --> IOSResources

    SharedDomain --> KMPKeys
    KMPKeys --> AndroidUI
    KMPKeys --> IOSUI

    AndroidResources --> AndroidUI
    IOSResources --> IOSUI

    TranslationTool -. alternative export .-> MOKOSpike
    DesignSource -. alternative export .-> MOKOSpike
    MOKOSpike -. opt-in, not default .-> AndroidUI
    MOKOSpike -. opt-in, not default .-> IOSUI
```

### What This Shows

This diagram shows the proposed default for strings, plurals, icons, and image assets. The source of truth for translations is a translation tool. The source of truth for visual assets is the design/brand asset source. Those sources export into platform-native resource formats.

KMP does not return final localized UI text by default. Shared code returns semantic values: typed errors, enums, or `ResourceKey` values. Android and iOS map those semantic values to their own native resources.

MOKO is shown as a dashed alternative path because it is not the default. It becomes relevant only if the team decides it needs a shared KMP resource runtime.

### Key Takeaways

- Strings are not manually duplicated in Android and iOS; the translation tool owns the source.
- Android still gets Android-native resources.
- iOS still gets iOS-native String Catalogs and asset catalogs.
- KMP owns semantic keys, not user-facing localized text.
- MOKO requires a spike because it changes resource ownership and iOS build integration.
