# ConsentGatingLab

A developer tool demonstrating consent-gated initialization and late start/stop of vendor SDKs (specifically AppsFlyer), combined with simulated Google UMP signals.

## Overview

ConsentGatingLab is a reference implementation showing how to:
- Gate SDK initialization behind user consent
- Integrate with Google UMP (User Messaging Platform) signals
- Dynamically start/stop SDKs based on consent state
- Maintain a data-driven SDK policy configuration
- Provide deep observability into SDK lifecycle events

## Requirements

- Android minSdk 24+
- Kotlin
- Gradle 8.2+

## Architecture

### Core Components

- **ConsentManager**: Manages user consent state with DataStore persistence
- **UmpStateSource**: Simulates UMP signals for GDPR compliance testing
- **AnalyticsController**: Consent-gated orchestrator that swaps AppsFlyer sinks
- **AfCoordinator**: Orchestrates SDK lifecycle based on consent + UMP state
- **SdkRegistry**: Loads SDK policies from `assets/sdk_policy.json`

### Package Structure

```
com.example.consentgatinglab/
├── core/          # Core types and interfaces
├── consent/       # Consent management implementation
├── ump/           # UMP state simulation
├── analytics/     # Analytics controller + sinks
├── appsflyer/     # AppsFlyer consent mapping helpers
├── gate/          # Coordination logic
├── diag/          # Diagnostics (EventBus, Metrics, Tripwire)
├── ui/            # Compose debug dashboard
├── app/           # Application and Activity
└── di/            # Hilt dependency injection
```

## Key Features

### 1. Data-Driven SDK Policy

SDKs and their consent requirements are defined in `assets/sdk_policy.json`:

```json
{
  "version": "1.0",
  "sdks": [
    {
      "id": "APPSFLYER",
      "requiredConsent": ["ANALYTICS", "MARKETING"],
      "initOrder": 10,
      "thread": "BACKGROUND"
    }
  ]
}
```

### 2. Consent Categories

- **ANALYTICS**: Analytics and measurement
- **MARKETING**: Marketing and attribution
- **PERSONALIZATION**: Personalized content

### 3. Debug Features

- **Network Security Config**: Debug builds block `*.appsflyer.com` to catch pre-consent violations
- **Tripwire**: Warns if any code attempts to use gated APIs while SDK is disabled
- **Timber Logging**: Detailed logs with latency measurements

## Build Types

### Debug
- Verbose logging enabled
- Network security config blocks AppsFlyer domains
- Tripwire warnings active
- App ID suffix: `.debug`

### Release
- Minimal logging
- No network restrictions
- Production configuration

## Getting Started

1. **Clone the repository**
   ```bash
   cd android
   ```

2. **Open in Android Studio**
   - File > Open > Select the `android` directory

3. **Sync Gradle**
   - The project will automatically sync dependencies

4. **Run the app**
   - Select debug build variant
   - Run on emulator or device

## Usage

### Testing Consent Flow

1. Launch the app - AppsFlyer should NOT be started
2. Toggle consent switches (Analytics, Marketing)
3. Click "Apply app consent"
4. Toggle UMP switches (ready, TCF string)
5. Monitor Logcat for lifecycle events

### Acceptance Criteria

✅ App starts with AppsFlyer OFF
✅ Granting ANALYTICS + MARKETING + UMP ready + TCF → AppsFlyer starts
✅ Revoking MARKETING → AppsFlyer stops
✅ Granting only ANALYTICS → AppsFlyer stays OFF
✅ Consent persists across app restarts
✅ Debug network policy blocks pre-consent traffic

## Important Notes

- **Placeholder Dev Key**: `AF_DEV_KEY_PLACEHOLDER` - Replace with real key for production
- **No Auto-Init**: AppsFlyer manifest receivers/providers are intentionally excluded
- **Singleton Pattern**: All managers/controllers are Hilt singletons
- **Thread Safety**: Controller methods use `Dispatchers.Default`, UI uses Main

## Observability

### Logcat Tags
- `AF`: AppsFlyer operations
- `CONSENT`: Consent updates
- `UMP`: UMP state changes
- `PLAN`: Coordinator decisions

### Metrics
- Start/stop latencies logged in milliseconds
- Consent change timestamps
- Bootstrap timing

## Network Security (Debug Only)

Debug builds include a network security config that blocks AppsFlyer domains:

```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">appsflyer.com</domain>
    <trust-anchors>
        <!-- Empty = block all -->
    </trust-anchors>
</domain-config>
```

This ensures any accidental pre-consent network calls fail fast with clear errors.

## Dependencies

- Jetpack Compose (UI)
- Hilt (DI)
- DataStore (Preferences)
- Kotlinx Serialization (JSON)
- Timber (Logging)
- AppsFlyer SDK

## License

This is a developer tool/reference implementation for educational purposes.
