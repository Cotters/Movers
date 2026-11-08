#!/usr/bin/env bash
set -euo pipefail

if (( $# != 0 )); then
    printf 'Usage: %s (no arguments)\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
[[ "$(uname -s)" == Darwin ]] || { printf 'iOS builds require macOS and Xcode.\n' >&2; exit 2; }
command -v xcodebuild >/dev/null 2>&1 || { printf 'Select a full Xcode installation first.\n' >&2; exit 2; }

exec xcodebuild -quiet -project iosApp/Movers.xcodeproj -scheme Movers -configuration Debug \
    -sdk iphoneos -destination 'generic/platform=iOS' \
    -derivedDataPath "$repo_dir/build/iosDerivedData" \
    CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
