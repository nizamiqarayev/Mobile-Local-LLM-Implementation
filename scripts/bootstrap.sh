#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd "$SCRIPT_DIR/.." && pwd)
LLAMA_DIR="$REPO_ROOT/third_party/llama.cpp"
PATCH_FILE="$REPO_ROOT/patches/llama-mobile.patch"

info() {
    printf '%-20s %s\n' "$1" "$2"
}

fail() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

require_file() {
    [ -f "$1" ] || fail "Required project file is missing: $1"
}

command -v git >/dev/null 2>&1 || fail "Git is required but was not found."

require_file "$REPO_ROOT/.gitmodules"
require_file "$REPO_ROOT/settings.gradle.kts"
require_file "$PATCH_FILE"

ACTUAL_ROOT=$(git -C "$REPO_ROOT" rev-parse --show-toplevel 2>/dev/null) ||
    fail "The project root is not a Git working tree."
[ "$ACTUAL_ROOT" = "$REPO_ROOT" ] ||
    fail "Expected repository root $REPO_ROOT, but Git reported $ACTUAL_ROOT."

printf 'LocalAI bootstrap\n\n'

EXPECTED_COMMIT=$(git -C "$REPO_ROOT" rev-parse HEAD:third_party/llama.cpp 2>/dev/null) ||
    fail "The root repository does not pin third_party/llama.cpp."

if ! git -C "$LLAMA_DIR" rev-parse --verify HEAD >/dev/null 2>&1; then
    printf 'Initializing pinned llama.cpp submodule...\n'
    git -C "$REPO_ROOT" submodule update --init --recursive
fi

ACTUAL_COMMIT=$(git -C "$LLAMA_DIR" rev-parse HEAD 2>/dev/null) ||
    fail "llama.cpp was not initialized successfully."

[ "$ACTUAL_COMMIT" = "$EXPECTED_COMMIT" ] || fail "llama.cpp is at $ACTUAL_COMMIT, but the project pins $EXPECTED_COMMIT. Preserve any local work, then restore the pinned submodule revision."

PATCH_STATE=
if git -C "$LLAMA_DIR" apply --check "$PATCH_FILE" >/dev/null 2>&1; then
    if [ -n "$(git -C "$LLAMA_DIR" status --porcelain --untracked-files=all)" ]; then
        fail "llama.cpp has local changes before patching. Inspect them with: git -C third_party/llama.cpp status --short"
    fi

    printf 'Applying LocalAI mobile compatibility patch...\n'
    git -C "$LLAMA_DIR" apply "$PATCH_FILE"
    PATCH_STATE=applied
elif git -C "$LLAMA_DIR" apply --reverse --check "$PATCH_FILE" >/dev/null 2>&1; then
    ACTUAL_PATCH=$(mktemp "${TMPDIR:-/tmp}/localai-llama-patch.XXXXXX") ||
        fail "Could not create a temporary patch verification file."
    trap 'rm -f "$ACTUAL_PATCH"' EXIT HUP INT TERM

    git -C "$LLAMA_DIR" \
        -c diff.noprefix=false \
        -c diff.mnemonicPrefix=false \
        -c diff.context=3 \
        diff --no-ext-diff --binary --no-renames > "$ACTUAL_PATCH"
    if ! cmp -s "$PATCH_FILE" "$ACTUAL_PATCH"; then
        fail "The LocalAI patch is present, but llama.cpp also contains different local edits. Inspect them with: git -C third_party/llama.cpp diff"
    fi
    git -C "$LLAMA_DIR" diff --cached --quiet ||
        fail "llama.cpp contains staged local edits. Inspect them with: git -C third_party/llama.cpp status --short"
    [ -z "$(git -C "$LLAMA_DIR" ls-files --others --exclude-standard)" ] ||
        fail "llama.cpp contains untracked files. Inspect them with: git -C third_party/llama.cpp status --short"

    PATCH_STATE="already applied"
else
    fail "llama.cpp is neither clean nor in the expected patched state. Inspect it with: git -C third_party/llama.cpp status --short"
fi

git -C "$LLAMA_DIR" diff --check >/dev/null ||
    fail "The patched llama.cpp working tree contains whitespace errors."

JAVA_RESULT="not found"
if command -v java >/dev/null 2>&1; then
    JAVA_LINE=$(java -version 2>&1 | sed -n '1p')
    JAVA_VERSION=$(printf '%s\n' "$JAVA_LINE" | sed -n 's/.*version "\([^"]*\)".*/\1/p')
    [ -n "$JAVA_VERSION" ] || JAVA_VERSION=unknown

    JAVA_MAJOR=$(printf '%s\n' "$JAVA_VERSION" | awk -F. '{ if ($1 == "1") print $2; else print $1 }')
    case "$JAVA_MAJOR" in
        ''|*[!0-9]*) fail "Could not determine the installed Java major version from: $JAVA_LINE" ;;
    esac
    [ "$JAVA_MAJOR" -ge 17 ] || fail "Java 17 or newer is required. Found $JAVA_VERSION."
    JAVA_RESULT="$JAVA_VERSION"
fi

[ "$JAVA_RESULT" != "not found" ] || fail "Java 17 or newer is required but was not found."

ANDROID_SDK=
if [ -n "${ANDROID_HOME:-}" ]; then
    ANDROID_SDK=$ANDROID_HOME
elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
    ANDROID_SDK=$ANDROID_SDK_ROOT
elif [ -f "$REPO_ROOT/local.properties" ]; then
    ANDROID_SDK=$(sed -n 's/^sdk\.dir=//p' "$REPO_ROOT/local.properties" | sed -n '$p')
fi

ANDROID_RESULT="not configured"
ANDROID_PLATFORM="not checked"
ANDROID_NDK="not checked"
ANDROID_CMAKE="not checked"
if [ -n "$ANDROID_SDK" ]; then
    if [ -d "$ANDROID_SDK" ]; then
        ANDROID_RESULT=$ANDROID_SDK
        [ -d "$ANDROID_SDK/platforms/android-36" ] && ANDROID_PLATFORM=found || ANDROID_PLATFORM=missing
        [ -d "$ANDROID_SDK/ndk/29.0.13113456" ] && ANDROID_NDK=found || ANDROID_NDK=missing
        [ -d "$ANDROID_SDK/cmake/3.31.6" ] && ANDROID_CMAKE=found || ANDROID_CMAKE=missing
    else
        ANDROID_RESULT="configured path does not exist: $ANDROID_SDK"
    fi
fi

XCODE_RESULT="not available on this platform"
if [ "$(uname -s)" = Darwin ]; then
    if command -v xcodebuild >/dev/null 2>&1 && xcode-select -p >/dev/null 2>&1; then
        XCODE_RESULT=$(xcodebuild -version 2>/dev/null | sed -n '1p')
    else
        XCODE_RESULT="not configured"
    fi
fi

ANDROID_MODEL="not installed"
IOS_MODEL="not installed"
[ -s "$REPO_ROOT/android/app/src/main/assets/models/model.gguf" ] && ANDROID_MODEL=installed
[ -s "$REPO_ROOT/ios/LocalAI/Models/model.gguf" ] && IOS_MODEL=installed

printf '\n'
info "Repository:" ready
info "llama.cpp:" "$ACTUAL_COMMIT"
info "Mobile patch:" "$PATCH_STATE"
info "Java:" "$JAVA_RESULT"
info "Android SDK:" "$ANDROID_RESULT"
info "Android API 36:" "$ANDROID_PLATFORM"
info "Android NDK:" "$ANDROID_NDK"
info "Android CMake:" "$ANDROID_CMAKE"
info "Xcode:" "$XCODE_RESULT"
info "Android model:" "$ANDROID_MODEL"
info "iOS model:" "$IOS_MODEL"

printf '\nBootstrap complete.\n\n'
printf 'Build Android:\n  ./gradlew :androidApp:assembleDebug\n\n'
printf 'Build shared iOS frameworks:\n'
printf '  ./gradlew \\\n'
printf '    :shared:linkDebugFrameworkIosArm64 \\\n'
printf '    :shared:linkDebugFrameworkIosSimulatorArm64 \\\n'
printf '    :shared:linkDebugFrameworkIosX64\n'
