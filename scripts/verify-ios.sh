#!/usr/bin/env bash
set -euo pipefail

if (( $# > 1 )) || [[ "${1:-}" != "" && "${1:-}" != "--archive" ]]; then
    printf 'Usage: %s [--archive]\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

fail() {
    printf 'iOS verification prerequisite: %s\nSee docs/workflow/multiplatform.md for setup.\n' "$1" >&2
    exit 2
}

[[ "$(uname -s)" == Darwin ]] || fail 'iOS builds require macOS and Xcode.'
command -v xcodebuild >/dev/null 2>&1 || fail 'Select a full Xcode installation first.'
xcrun --sdk iphoneos --show-sdk-path >/dev/null 2>&1 || fail 'Select Xcode with the iOS device SDK installed.'
[[ -f iosApp/MoversApp.swift ]] || fail 'The tracked Swift app entry point iosApp/MoversApp.swift is missing.'
[[ -x ./gradlew ]] || fail 'The Gradle wrapper is missing or not executable.'

if [[ -z "${JAVA_HOME:-}" ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)" || fail 'Install JDK 17 or set JAVA_HOME to it.'
    export JAVA_HOME
fi
java_version="$("$JAVA_HOME/bin/java" -version 2>&1)" || fail 'Java could not start; check JAVA_HOME.'
version_pattern='version[[:space:]]+"([0-9]+)'
if [[ ! "$java_version" =~ $version_pattern ]] || [[ "${BASH_REMATCH[1]}" != 17 ]]; then
    fail 'Use JDK 17 for the verified project toolchain.'
fi

xcrun --find swift-format >/dev/null 2>&1 || fail 'The selected Xcode must provide swift-format.'
xcrun swift-format lint --strict iosApp/MoversApp.swift

configuration=Debug
action=build
if [[ "${1:-}" == "--archive" ]]; then
    configuration=Release
    action=archive
fi

build_args=(-quiet -project iosApp/Movers.xcodeproj -scheme Movers -configuration "$configuration"
    -sdk iphoneos -destination 'generic/platform=iOS'
    -derivedDataPath "$repo_dir/build/iosDerivedData")
if [[ "$action" == archive ]]; then
    build_args+=(-archivePath "$repo_dir/build/iosArchives/Movers.xcarchive")
fi

printf 'Verifying unsigned iOS %s (%s), without installing or launching.\n' "$configuration" "$action"
exec xcodebuild "${build_args[@]}" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO "$action"
