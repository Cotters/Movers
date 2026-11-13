#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v python3 >/dev/null 2>&1 || { printf 'Formatting requires Python 3.\n' >&2; exit 2; }

python3 - "$repo_dir" "$@" <<'PY'
import os
from pathlib import Path
import subprocess
import sys

root = Path(sys.argv[1]).resolve()
arguments = sys.argv[2:]
mode = arguments.pop(0) if arguments and arguments[0] in ("--check", "--write") else "--check"
if not arguments or any(argument.startswith("--") for argument in arguments):
    print("Usage: scripts/format-kotlin.sh [--check|--write] <file.kt|file.kts> ...", file=sys.stderr)
    sys.exit(64)

files = []
for argument in arguments:
    file = Path(argument)
    if not file.is_absolute():
        file = root / file
    resolved = file.resolve()
    invalid_path = (
        file.is_symlink() or root not in resolved.parents
        or not resolved.is_file() or resolved.suffix not in (".kt", ".kts")
    )
    if invalid_path:
        print("Formatting accepts only explicit Kotlin files inside this checkout; no directories or external files.", file=sys.stderr)
        sys.exit(64)
    files.append(str(resolved))

configured = os.environ.get("MOVERS_STUDIO_FORMATTER")
candidates = [Path(configured).expanduser()] if configured else [
    Path.home() / "Applications/Android Studio.app/Contents/bin/format.sh",
    Path("/Applications/Android Studio.app/Contents/bin/format.sh"),
]
formatter = next((path for path in candidates if path.is_file() and os.access(path, os.X_OK)), None)
if formatter is None:
    print("Set MOVERS_STUDIO_FORMATTER to Android Studio's format launcher, or use the running IDE's Reformat Code.", file=sys.stderr)
    sys.exit(2)

command = [str(formatter.resolve()), "-allowDefaults"]
if mode == "--check":
    command.append("-dry")
environment = dict(os.environ)
if "STUDIO_PROPERTIES" not in environment:
    runtime = root / "build/quality/studio-formatter"
    runtime.mkdir(parents=True, exist_ok=True)
    properties = runtime / "studio.properties"
    properties.write_text("\n".join(
        f"idea.{directory}.path={runtime / directory}" for directory in ("config", "system", "log")
    ) + "\n", encoding="utf-8")
    environment["STUDIO_PROPERTIES"] = str(properties)
try:
    result = subprocess.run([*command, *files], cwd=root, env=environment, timeout=120)
except (OSError, subprocess.TimeoutExpired):
    print("Formatter could not finish. Use the running IDE; do not close another person's IDE session.", file=sys.stderr)
    sys.exit(2)
sys.exit(result.returncode)
PY
