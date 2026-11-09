#!/usr/bin/env bash
set +x
set -euo pipefail
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v python3 >/dev/null 2>&1 || { printf 'Install Python 3 before running backend setup.\n' >&2; exit 2; }
exec python3 "$repo_dir/scripts/supabase_setup.py" "$@"
