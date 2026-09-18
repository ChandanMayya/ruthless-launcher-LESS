# LESS

Deliberately boring text-only Android launcher (display name **LESS**).

Black `#000000` / white `#FFFFFF` only. No icons in the UI, no ads, no analytics, no network.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer, or JDK 17+
- Android SDK Platform 34 and Build-Tools 34.0.0
- minSdk 26

## Open in Android Studio

1. File → Open → select this repository root
2. Let Gradle sync
3. Run the `app` configuration on a device/emulator
4. Complete first-run setup (default launcher, usage access, optional accessibility)

## Build from CLI

```bash
# Optional: point at your SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

./gradlew test
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Features

- Home: clock, date, screen time, phone temperature, pickups, last unlock, boredom messages, home apps
- Swipe up for text app drawer + search (CONFUSE ME apps hidden from search)
- Long-press apps: pin, limits, confuse, confirmation, delay, exclusions, home, app info
- Settings (long-press clock): home/pins, policy lists, screen time history, text size/font, data, permissions
- Quotas with one 5-minute extension per day; optional Accessibility enforcement
- Policies survive target-app uninstall/reinstall by package name

## Design constraints

No colors, icons, cards, engagement features, or network calls.
