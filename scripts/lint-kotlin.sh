#!/usr/bin/env bash
set -euo pipefail

if (( $# != 0 )); then
    printf 'Usage: %s (no arguments)\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec "$repo_dir/gradlew" --no-daemon --console=plain -p "$repo_dir/tools/lint" lintKotlin
