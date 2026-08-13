# LocalAI implementation roadmap

This roadmap keeps implementation sequential. Work on one unchecked task at a
time, verify it, commit it, and only then select the next task.

## Foundation

- [x] Create and publish the root repository.
- [x] Pin llama.cpp and preserve the mobile compatibility patch.
- [x] Verify model-free Android and shared iOS builds from a clean checkout.
- [x] Add repeatable dependency bootstrap and environment checks.
- [x] Document the development and contribution workflow.

The project foundation ends here. All tasks below are major learning and
implementation work intended to be completed manually, with review and
guidance as needed.

## Android real-inference milestone

- [ ] Select the development model and record source, license, size, and
  checksum.
- [ ] Download and independently verify one GGUF file.
- [ ] Package the verified model in the Android development build.
- [ ] Run the application on a physical ARM64 Android device while offline.
- [ ] Prove that output comes from llama.cpp rather than the mock engine.
- [ ] Add observable model copy, load, generation, cancellation, and failure
  stages.
- [ ] Add versioned and checksummed private model installation.
- [ ] Harden cancellation, cleanup, backgrounding, and repeated generation.

## iOS real-inference milestone

- [ ] Package the same verified model in the iOS development build.
- [ ] Run the application on a physical iPhone while offline.
- [ ] Prove that output comes from llama.cpp with Metal rather than the mock
  engine.
- [ ] Add comparable iOS load and generation diagnostics.
- [ ] Guarantee exactly one completion event across the Swift/Kotlin bridge.
- [ ] Harden cancellation, disposal, backgrounding, and memory-pressure
  behavior.

## Shared engine foundation

- [ ] Replace UI status strings and booleans with a typed engine state model.
- [ ] Define platform-neutral generation configuration.
- [ ] Replace raw prompt/token APIs with request and generation-event types.
- [ ] Define typed, recoverable engine failures.
- [ ] Build a deterministic fake engine for tests and previews.
- [ ] Add engine state-transition and cancellation tests.

## Chat application

- [ ] Move generation coordination into a shared state holder.
- [ ] Define conversation and message domain entities.
- [ ] Render user, assistant, streaming, cancelled, and failed messages.
- [ ] Implement a tested single-conversation flow.
- [ ] Move Gemma control tokens behind a chat-template abstraction.
- [ ] Implement multi-turn prompting consistently on Android and iOS.
- [ ] Add context-window budgeting and deterministic truncation.

## Persistence

- [ ] Define conversation, settings, and model repository contracts.
- [ ] Persist generation settings with a versioned schema.
- [ ] Persist conversation lists and messages.
- [ ] Add migration, corruption, and deletion tests.

## Model management

- [ ] Define versioned model metadata shared across platforms.
- [ ] Separate model downloading, installation, loading, and generation.
- [ ] Add storage checks, temporary downloads, checksum verification, and
  atomic installation.
- [ ] Support selecting, loading, and safely deleting multiple local models.
- [ ] Replace Android development bundling with production model delivery.
- [ ] Replace iOS development bundling with production model delivery.

## Performance and adaptability

- [ ] Establish a physical-device benchmark protocol.
- [ ] Record load time, time to first token, generation speed, peak memory,
  thermals, and battery impact.
- [ ] Add safe runtime controls for context, sampling, and supported native
  tuning.
- [ ] Detect device capabilities and choose conservative defaults.
- [ ] Introduce battery-saver, balanced, and performance profiles.

## Product quality

- [ ] Add conversations, chat, models, settings, and diagnostics navigation.
- [ ] Add copy, regenerate, edit-and-resend, retry, and new-chat actions.
- [ ] Verify accessibility, dynamic text, touch targets, and contrast.
- [ ] Support phone, tablet, landscape, and practical foldable layouts.
- [ ] Move user-facing text into localization resources.

## Privacy, licensing, and release

- [ ] Document and test the offline privacy boundary.
- [ ] Decide backup and encryption behavior for stored conversations.
- [ ] Add required Gemma notices and third-party license material.
- [ ] Select and add a license for the application repository.
- [ ] Add model-free continuous integration checks.
- [ ] Add dependency and patch-compatibility monitoring.
- [ ] Create debug, benchmark, and release configurations.
- [ ] Complete physical-device, offline, memory, migration, privacy, and store
  packaging release gates.
