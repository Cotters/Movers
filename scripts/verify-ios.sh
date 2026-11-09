#!/usr/bin/env bash
set -euo pipefail

if (( $# > 1 )) || [[ "${1:-}" != "" && "${1:-}" != "--archive" ]]; then
    printf 'Usage: %s [--archive]\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
[[ "$(uname -s)" == Darwin ]] || { printf 'iOS builds require macOS and Xcode.\n' >&2; exit 2; }
command -v xcodebuild >/dev/null 2>&1 || { printf 'Select a full Xcode installation first.\n' >&2; exit 2; }

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

exec xcodebuild "${build_args[@]}" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO "$action"
