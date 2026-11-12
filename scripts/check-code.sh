#!/usr/bin/env bash
set -euo pipefail

if (( $# > 1 )); then
    printf 'Usage: %s [task-start-commit]\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
"$repo_dir/scripts/review-code.sh" "$@"
exec "$repo_dir/scripts/lint-kotlin.sh"
