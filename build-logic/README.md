# magisk-build-logic

Shared Gradle convention plugins for the Magisk317 Android app family.

Current plugin set:

- `magisk.distribution.policy`
- `magisk.android.distribution`
- `magisk.android.application`
- `magisk.android.common`
- `magisk.android.compose`
- `magisk.android.library`
- `magisk.android.room`
- `magisk.kotlin.library`
- `magisk.app.packaging`
- `magisk.app.signing`
- `magisk.maintenance`

## Consumers

- `XposedSmsCode`
- `xinyi-relay`
- `MiPush`
- `MiPushFramework`

## Build Dependencies

When this repository is included as `build-logic/` inside a consumer project, it
reads the parent project's `gradle/libs.versions.toml` so the convention plugin
classpath follows the consuming app's AGP, Kotlin, KSP, Hilt and Java bytecode
versions.

The consumer catalog is required; this repository does not carry fallback
toolchain or dependency versions.

Because the build reads the consumer catalog, verify plugin changes through a
consumer checkout with this repository mounted at `build-logic/`. The wrapper
under `shared/gradle/` is a synchronization source for consumers, not a
standalone wrapper for this repository.

The plugins themselves still read the target repository's `libs` catalog at
runtime for application-specific versions such as `compileSdk`, `versionName`,
and product flavor behavior.

Android library lint keeps AGP's blocking default. Consumers may suppress
specific, documented checks per module, but the shared convention never turns
off lint failures globally.

### Distribution policy

The distribution matrix is product data, so each consuming repository declares it in its
root build script and the shared convention only supplies the mechanism that turns the
declaration into AGP flavors.

Apply `magisk.distribution.policy` once in the root project and declare the channels
there; Android modules pick it up through `magisk.android.distribution`:

```kotlin
// root build.gradle.kts
plugins {
    id("magisk.distribution.policy")
}

magiskDistribution {
    flavor("play") {
        buildConfigBoolean("HAS_BILLING", true)
    }
    flavor("github") {
        buildConfigBoolean("HAS_BILLING", false)
        buildConfigVersion("MATRIX_PLUGIN_SDK_VERSION", "matrix-sdk-android")
    }
    dynamicFeatureFlavors.add("play")
    preferInjectedCommitHash = true
}
```

Defaults: `dimension` is `distribution`, `dynamicFeatureFlavors` is empty, and
`preferInjectedCommitHash` is false, so a repository that declares nothing gets no
flavors — and applying the module plugin without the root declaration fails the build
rather than silently producing an unflavored artifact.

Play and GitHub are the active channels. F-Droid stays paused, and the resume point now
lives in each consumer's declaration instead of being restated per convention; a paused
distribution remains a documented state (comment + resume point + this note), never a
silent disable. Dynamic-feature variants stay Play-only because Play Feature Delivery is
a store capability rather than a runtime mode.

Keeping the matrix in the consumer also removes the fallback it used to force: shared
library modules previously redeclared the flavor set, so an app that did not carry the
dimension resolved it through `missingDimensionStrategy`, which picked a channel without
ever reporting the mismatch.

`magisk.app.packaging` owns generic APK split, bundle, timestamp, and artifact-name behavior.
Product-specific output constraints stay with the product: each consumer declares its own
distribution flavor set in the root `magiskDistribution` block.

Runtime capability states such as Enhanced (Xposed active) and Standard (system APIs) belong
in the consuming application's runtime policy and must not be added here as `full` / `lite`
flavors. A separate binary distribution is justified only by a concrete packaging or
store-compliance requirement, not by the absence of Xposed at runtime.

### Test runtime policy

Applying `magisk.maintenance` to the root project standardizes JUnit Platform execution,
Java 26 native/dynamic-agent flags, worker heap/fork limits, and test logging for every
subproject. Android compile conventions do not mutate test workers.

### Signing contract

`magisk.app.signing` accepts the standard environment variables `KEYSTORE_FILE`,
`STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Relative keystore paths are resolved from
the root project. Local builds may instead use `-Pmagisk.signing.keystore=...` and
`-Pmagisk.signing.properties=...`; defaults are `release.jks`/`app/release.jks` and
`signature.properties`/`app/signature.properties`. Historical Tianma/Yuuta properties and
credential aliases are intentionally unsupported. Debug builds use local debug signing;
release build types require the configured release key.

## Shared Gradle Assets

This repository also carries a shared Gradle asset bundle under
`shared/gradle/` for wrapper scripts, wrapper metadata, and common
`gradle.properties` defaults.

- `scripts/sync_consumer_gradle_assets.sh <repo...>` syncs one or more consumer
  repositories to the shared wrapper and common properties.

The local templates use the bundled Gradle ZIP and offline repositories. Keep
`third_party/` with the project when synchronizing these assets.

`magisk.maintenance` standardizes test logging and exposes an explicit
`cleanupGradleCaches` task. Normal build, assemble, and clean tasks never delete
Gradle caches as a side effect.

## Usage

The build logic is ordinary local source under `build-logic/`. Keep:

```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

For `magisk.app.packaging`, consumers can override the artifact base name:

```kotlin
extra["artifactBaseName"] = "MiPushFramework"
```

Release APK names omit distribution flavor labels (`play`, `github` in
xinyi-relay) and preserve product-specific flavor dimensions such as `normal`
or `vc105`. Play App Bundles keep the Android Gradle Plugin output
path and filename; release pipelines should consume the `playRelease` bundle
directly instead of relying on a second renamed copy.
