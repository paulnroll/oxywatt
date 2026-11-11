# T02a — Karoo diagnostic host

The owner approved T02a after PR #7 merged. Scope covers Android toolchain selection, a minimal debug APK and installation evidence for R02/R09/R11 and the setup portion of V02/V11/V13. T01 provides the canonical contract and replay fixtures. T02b owns stream mapping and lifecycle probes.

Use the existing Gradle 9.1.0 wrapper and JDK 25 with AGP 9.0.1, built-in Kotlin and Java 17 bytecode. Install Android platform 36 and Build Tools 36.0.0 for the diagnostic build. Use target SDK 34 and min SDK 23 as candidates based on the official sample. The device OS and host build require independent verification. See the [AGP compatibility matrix](https://developer.android.com/build/releases/agp-9-0-0-release-notes) and [pinned sample](https://github.com/hammerheadnav/karoo-ext/blob/1.1.9/app/build.gradle.kts).

The app uses the public Hammerhead GitHub Packages coordinate `io.hammerhead:karoo-ext:1.1.9` and an exported service with the official discovery action and XML metadata. The app ID is `com.oxywatt.diagnostic`. The extension ID is `oxywatt-diagnostic`. The original T02a APK declared an empty capability set. The [T02b probe](t02b-karoo-probe.md) adds FIT binding observation. The launcher shows a diagnostic label, and the service uses the SDK's default behavior. No sensor readings, profile or location data are accessed. This is an installable host for later work on the probe.

The change owns `settings.gradle.kts`, AndroidX build configuration, formatting coverage for `app`, and the app's build, service, activity and resources. The JVM build remains the default. Android dependencies are resolved only for `-PkarooDiagnostic=true`. Credentials are supplied locally through environment variables. The package repository is scoped to karoo-ext.

## Verification

Set `ANDROID_HOME` to an SDK with platform 36 and Build Tools 36.0.0, or set `sdk.dir` in ignored `local.properties`. Supply `KAROO_PACKAGES_USERNAME` and `KAROO_PACKAGES_TOKEN` for GitHub Packages read access. Keep the token out of commands, files and logs.

```sh
./gradlew :core:check --console=plain
./gradlew -PkarooDiagnostic=true :app:assembleDebug :app:lintDebug --console=plain
```

On an identified Karoo 3 with ADB enabled, verify the target and run:

```sh
adb devices
adb -s DEVICE shell getprop ro.build.version.sdk
adb -s DEVICE shell getprop ro.build.display.id
adb -s DEVICE install -r app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE shell am start -n com.oxywatt.diagnostic/.MainActivity
```

Record the Karoo software version from the device settings as well. APK installation and launch confirm the setup portion only. Host discovery, callback timing and whole-ride residency require T02b/T02c evidence.

## Evidence and code review

- Baseline `e09cfca` includes PR #7. The JVM build, formatting checks and 17 tests pass on Temurin 25. Android platform 36 and Build Tools 36.0.0 are installed locally.
- Review moved AGP to the shared root classpath for the opt-in build so AGP and Kotlin can see each other's types. The Android build now configures and reaches dependency resolution. GitHub Packages returns HTTP 401 without authentication. The repository build could not resolve the published AAR because package credentials were absent.
- An isolated build substituted the official SDK sources at tag 1.1.9, commit `26e1d1c5c86e4d49922b2e2cc0474e62fc3b6eed`, for the Maven dependency and used the SDK's runtime dependencies. App assembly and lint passed after increasing the Gradle daemon's Metaspace for that source build. Lint reported two warnings for the sample's target SDK 34 and the backup rules on Android 12 or later. The diagnostic host stores no ride data.
- APK inspection confirmed the package, launcher, extension discovery action and XML metadata. Signature verification passed. The merged APK requests only the AndroidX signature permission for nonexported receivers. Local source-build APK: `app/build/outputs/apk/source-check/oxywatt-diagnostic-debug.apk`. SHA-256: `73c2c345f144ec3fe7023ee135a765f8742e41594c7304c1a344d864fae31c3a`.
- Initial ADB check showed no attached device. After the owner connected the Karoo 3 via USB, ADB identified model `k24` with Android 12, API 32 and build `SKQ1.230210.001 dev-keys`. The Hammerhead ride app was `4.217.1-ad98202524` (version code `27885909`).
- The source-build APK installed successfully with `adb install -r`. A cold launch returned `Status: ok` and Activity Manager reported `com.oxywatt.diagnostic/.MainActivity` as resumed. Android Package Manager resolved exactly one service in the diagnostic package for `io.hammerhead.karooext.KAROO_EXTENSION`, namely `DiagnosticExtension`. Host binding and ride callbacks remain for T02b/T02c.
- T02a install and launch are verified on this device with the source-build APK. Assembly against the published AAR still requires GitHub Packages credentials.


2026-11-11 preview follow-up: the same model/OS/ride-host versions were re-observed; original diagnostic host 0.2.0/code 2 remained installed. Only `.preview` and its instrumentation companion were installed for T03p. [Preview evidence](../../evidence/t03p-karoo-2026-11-11/README.md) does not upgrade the original host or close sustained/live T02 acceptance.
