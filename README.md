# LocalAI — Compose Multiplatform Foundation

A shared Kotlin/Compose application for Android and iOS using llama.cpp for fully local inference.

## Structure

```text
shared/                 Shared Compose UI, state, and inference contract
  src/commonMain/       UI and runtime-neutral LocalLLMEngine
  src/androidMain/      Android engine factory
  src/iosMain/          iOS Compose UIViewController entry point
android/app/            Native Android launcher and packaged model assets
ios/                    Native Xcode launcher embedding the shared framework
```

The UI and generation flow are shared. Model loading and inference remain platform-specific because Android and iOS have different accelerators, runtime APIs, and asset delivery mechanisms.

Android uses the official llama.cpp Android JNI binding. iOS uses the official
llama.cpp Apple XCFramework with Metal and Accelerate through a Swift-to-Kotlin
callback adapter. Without a model, either platform keeps the streaming mock
active so UI development remains fast.

## Clone setup

The project pins llama.cpp as a Git submodule and carries a small mobile
compatibility patch. A development machine needs:

- JDK 17 or newer
- Android SDK 36
- Android NDK `29.0.13113456`
- CMake `3.31.6` from the Android SDK
- Xcode for iOS builds

Clone the repository and run the repeatable bootstrap command before building:

```shell
git clone git@github.com:nizamiqarayev/Mobile-Local-LLM-Implementation.git
cd Mobile-Local-LLM-Implementation
./scripts/bootstrap.sh
```

The bootstrap command initializes the pinned dependency, applies the patch only
when needed, validates Java and available platform tools, and reports whether
development models are installed. It never downloads model weights or installs
system packages. The equivalent manual dependency setup is:

```shell
git submodule update --init --recursive
git -C third_party/llama.cpp apply ../../patches/llama-mobile.patch
```

The patch aligns the upstream Android library with this Gradle build, retains
API 26 logging compatibility, resets reusable iOS generation state, and works
around AppleClang 21 detection while producing the iOS XCFramework.

For terminal Android builds, either export `ANDROID_HOME` or create the local,
ignored `local.properties` file with the absolute SDK path:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

Verify the model-free foundation with:

```shell
./gradlew :androidApp:assembleDebug
./gradlew \
  :shared:linkDebugFrameworkIosArm64 \
  :shared:linkDebugFrameworkIosSimulatorArm64 \
  :shared:linkDebugFrameworkIosX64
```

These builds do not require model weights. Until `model.gguf` is installed,
both launchers deliberately use the streaming mock engine.

Development conventions and safe submodule/model workflows are documented in
[`CONTRIBUTING.md`](CONTRIBUTING.md). Implementation is tracked one task at a
time in [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Run Android

Install Android Studio (including its bundled JDK and Android SDK 36), open the repository root, and run the `androidApp` configuration. The included Gradle launcher automatically detects Android Studio's bundled JDK on macOS. From a terminal, use:

```shell
./gradlew :androidApp:assembleDebug
```

Place a development GGUF model at
`android/app/src/main/assets/models/model.gguf`. Large production models should
use an install-time Play Asset Delivery pack.

## Run iOS

Open `ios/LocalAI.xcodeproj` in Xcode. The Gradle launcher uses Android Studio's bundled JDK on macOS; on other setups, install JDK 17 or newer. The `Compile Kotlin Framework` build phase invokes:

```shell
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Select a development team before running on a physical device. A production iOS model should be added to the app target's Copy Bundle Resources phase or delivered using Apple's supported on-demand resource mechanism.

For local development, place the same renamed model at:

```text
ios/LocalAI/Models/model.gguf
```

The Xcode `Bundle GGUF Model` phase copies it into the app automatically. The
lean iOS-only XCFramework can be reproduced using the commands documented in
`ios/Frameworks/README.md`.

## Model files

Model weights are excluded from Git. The recommended starter is
`gemma-3-1b-it-Q4_K_M.gguf` (about 806 MB); rename it to `model.gguf` in each
platform folder. Bundling the file makes first launch fully offline. For store
distribution, move it to Play Asset Delivery / Apple-hosted on-demand resources
to avoid oversized base packages.
