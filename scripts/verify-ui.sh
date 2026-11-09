#!/usr/bin/env bash
set -euo pipefail

if (( $# != 2 )) || [[ "$1" != ios && "$1" != android ]] || [[ -z "$2" ]]; then
    printf 'Usage: %s <ios|android> <already-running-device-id>\n' "$0" >&2
    exit 64
fi

platform="$1"
device_id="$2"
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
maestro_bin="${MAESTRO_BIN:-maestro}"
command -v "$maestro_bin" >/dev/null 2>&1 || {
    printf 'Install Maestro CLI 2.12.1 or set MAESTRO_BIN to its executable.\n' >&2
    exit 2
}

if [[ "$platform" == ios ]]; then
    command -v xcrun >/dev/null 2>&1 &&
        xcrun simctl list devices booted | grep -F -- "($device_id) (Booted)" >/dev/null || {
            printf 'The selected iOS Simulator must already be booted.\n' >&2
            exit 2
        }
else
    command -v adb >/dev/null 2>&1 &&
        [[ "$(adb -s "$device_id" get-state 2>/dev/null)" == device ]] || {
            printf 'The selected Android target must already be connected.\n' >&2
            exit 2
        }
fi

output_dir="$repo_dir/build/ui-smoke/$platform"
mkdir -p "$output_dir"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
exec "$maestro_bin" --platform "$platform" --device "$device_id" test \
    --format JUNIT --output "$output_dir/report.xml" \
    --test-output-dir "$output_dir/artifacts" "$repo_dir/.maestro"
