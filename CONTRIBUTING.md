# Contributing to LocalAI

This project is intentionally developed in small, reviewable tasks. Each task
should teach one concept, change one area of behavior, and end in a verified
commit.

## Development setup

Required tools:

- Git
- JDK 17 or newer
- Android SDK 36
- Android NDK `29.0.13113456`
- Android SDK CMake `3.31.6`
- Xcode for iOS development

After cloning, run:

```shell
./scripts/bootstrap.sh
```

The bootstrap script initializes the pinned llama.cpp submodule, applies the
project's compatibility patch, validates the available toolchains, and reports
whether local model files exist. It does not install packages or download model
weights.

For terminal Android builds, configure the local SDK in the ignored
`local.properties` file:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

## Repository boundaries

The root repository owns the application code, documentation, built iOS
llama.cpp XCFramework, compatibility patch, and the llama.cpp Git pointer.

`third_party/llama.cpp` is a separate pinned Git repository. After bootstrap,
the root status normally reports it as modified because
`patches/llama-mobile.patch` is applied locally:

```text
 m third_party/llama.cpp
```

That expected state must contain exactly the patch's four changes. Run
`./scripts/bootstrap.sh` to verify it. Do not commit application work inside the
upstream llama.cpp repository.

## Task workflow

1. Start from an up-to-date `main` branch.
2. Choose one unchecked task from `docs/ROADMAP.md`.
3. Create a focused branch such as `task/android-model-metadata`.
4. Write down the expected behavior before implementation.
5. Make the smallest complete change.
6. Run checks appropriate to the affected platform.
7. Review both the root diff and the nested llama.cpp diff.
8. Commit only files belonging to the task.
9. Push the branch and record verification results.

Avoid mixing architecture refactors, native runtime changes, UI redesign, and
model delivery in one task.

## Verification commands

Validate repository setup:

```shell
./scripts/bootstrap.sh
```

Build the model-free Android application:

```shell
./gradlew :androidApp:assembleDebug
```

Build all shared iOS framework variants:

```shell
./gradlew \
  :shared:linkDebugFrameworkIosArm64 \
  :shared:linkDebugFrameworkIosSimulatorArm64 \
  :shared:linkDebugFrameworkIosX64
```

Inspect both Git working trees before committing:

```shell
git status --short
git diff --check
git -C third_party/llama.cpp status --short
git -C third_party/llama.cpp diff --check
```

Generated build caches can occasionally contain duplicate copied files. If a
build reports a generated class with a name such as `File 2.class`, clean the
affected generated modules and rebuild:

```shell
./gradlew :shared:clean :androidApp:clean
./gradlew :androidApp:assembleDebug
```

## Model files

Model weights are local development artifacts and must never be committed.
The repository ignores GGUF, LiteRT, safetensors, and related weight formats.

The current development locations are:

```text
android/app/src/main/assets/models/model.gguf
ios/LocalAI/Models/model.gguf
```

Before using a model:

1. Record its source repository and exact source filename.
2. Review and accept its license.
3. Calculate and record its SHA-256 checksum.
4. Verify the copied file against that checksum.
5. Confirm `git status` does not list the model.

Do not treat bundled development assets as the production delivery design.
Production model installation and licensing are separate roadmap tasks.

## Updating llama.cpp

Do not update the submodule casually. A llama.cpp update is its own task and
requires:

1. Recording the old and proposed commits.
2. Reviewing upstream API and model-support changes.
3. Checking whether the compatibility patch is still necessary.
4. Rebuilding the Android native library.
5. Rebuilding the iOS XCFramework.
6. Running real inference on physical Android and iOS devices.
7. Recording performance and memory comparisons.
8. Updating the submodule pointer and patch together.

Never force-apply an incompatible patch or discard nested repository changes
without first understanding them.

## Definition of done

A task is complete only when:

- Its expected behavior is implemented.
- Relevant automated or manual checks pass.
- No model weights, credentials, local SDK paths, or generated outputs are
  staged.
- Root and submodule diffs are understood.
- Documentation reflects any changed workflow.
- The task has one focused commit with recorded verification.
