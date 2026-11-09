#!/usr/bin/env bash
set -euo pipefail

if (( $# != 0 )); then
    printf 'Usage: %s (no arguments)\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v python3 >/dev/null 2>&1 || { printf 'Documentation checks require Python 3.\n' >&2; exit 2; }

python3 - "$repo_dir" <<'PY'
from pathlib import Path
from urllib.parse import unquote, urlsplit
import re
import sys

root = Path(sys.argv[1]).resolve()
documents = [root / "AGENTS.md", root / "README.md", root / "build-logic/README.md", root / "supabase/README.md"]
documents.extend(sorted((root / "docs").rglob("*.md")))
failures = []
links = 0
for document in documents:
    content = re.sub(r"```.*?```", "", document.read_text(encoding="utf-8"), flags=re.S)
    for match in re.finditer(r"\]\(([^)]+)\)", content):
        target = match.group(1).strip().strip("<>")
        if target.startswith("#") or urlsplit(target).scheme:
            continue
        path = Path(unquote(target.split("#", 1)[0]))
        resolved = (document.parent / path).resolve()
        links += 1
        outside = resolved != root and root not in resolved.parents
        if path.is_absolute() or outside or not resolved.exists():
            failures.append(f"{document.relative_to(root)}: {target}")
if failures:
    print("Broken or non-portable local documentation targets:", file=sys.stderr)
    print("\n".join(failures), file=sys.stderr)
    sys.exit(1)
print(f"Documentation: {len(documents)} files, {links} local link targets checked.")
print("External URLs and heading anchors are not checked; no app build or network access.")
PY
