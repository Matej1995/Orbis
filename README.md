# Orbis

Kotlin Multiplatform project targeting **Android** (Jetpack Compose) and **iOS** (SwiftUI).

Architecture: shared ViewModels and business logic in KMP, platform-specific UI.

## Project Structure

```
Orbis/
├── composeApp/          # KMP application module (Android + iOS entry points)
├── core/
│   ├── data/            # Data layer - networking (Ktor), database (Room), logging
│   ├── domain/          # Domain layer - business logic, error handling, interfaces
│   └── presentation/    # Presentation layer - UI utilities, ViewModel support
├── iosApp/              # Native iOS app (Xcode project, SwiftUI)
└── build-logic/         # Gradle convention plugins
```

## Tech Stack

| Category | Library | Version |
|----------|---------|---------|
| Language | Kotlin Multiplatform | 2.3.0 |
| DI | Koin | 4.1.1 |
| Networking | Ktor | 3.4.0 |
| Database | Room | 2.8.4 |
| Preferences | DataStore | 1.2.0 |
| Serialization | kotlinx-serialization | 1.9.0 |
| Image Loading | Coil | 3.3.0 |
| Logging | Kermit | 2.0.8 |
| Permissions | Moko Permissions | 0.20.1 |
| Build Config | BuildKonfig | 0.17.1 |
| Android UI | Jetpack Compose (BOM 2026.01.00) | |
| iOS UI | SwiftUI | |

## Build & Run

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Or use the run configuration in Android Studio / Fleet.

### iOS

Open `iosApp/` in Xcode and run, or use the KMP run configuration in Fleet.

## Architecture

```
┌──────────────────────────────────┐
│  UI Layer (platform-specific)    │
│  Android: Jetpack Compose        │
│  iOS: SwiftUI                    │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│  core:presentation               │
│  Shared ViewModels, UiText       │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│  core:domain                     │
│  Interfaces, Result<T,E>, Errors │
│  No external dependencies        │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│  core:data                       │
│  Ktor, Room, DataStore, Kermit   │
└──────────────────────────────────┘
```
