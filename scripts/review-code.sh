#!/usr/bin/env bash
set -euo pipefail

if (( $# > 1 )); then
    printf 'Usage: %s [base-commit] (default: HEAD)\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v python3 >/dev/null 2>&1 || { printf 'Code review requires Python 3.\n' >&2; exit 2; }

python3 - "$repo_dir" "${1:-HEAD}" <<'PY'
from pathlib import Path
import re
import subprocess
import sys

root = Path(sys.argv[1])


def git(*args):
    return subprocess.check_output(
        ["git", "--no-pager", *args], cwd=root, text=True,
    )


base = git("rev-parse", "--verify", "--end-of-options", sys.argv[2] + "^{commit}").strip()
for snapshot in ([], ["--cached"]):
    patch_check = subprocess.run(
        ["git", "diff", "--no-ext-diff", "--check", *snapshot, base, "--"],
        cwd=root, capture_output=True,
    )
    if patch_check.returncode:
        label = "staged" if snapshot else "working tree"
        print(f"Patch hygiene failed in {label}; inspect git diff --check locally. Source is omitted.", file=sys.stderr)
        sys.exit(1)
changed = git("diff", "--name-only", "-z", "--diff-filter=ACMR", base, "--").split("\0")
staged = set(git("diff", "--cached", "--name-only", "-z", "--diff-filter=ACMR", base, "--").split("\0"))
untracked = set(git("ls-files", "--others", "--exclude-standard", "-z").split("\0"))
extensions = {".kt", ".kts", ".swift", ".py", ".sh", ".yml", ".yaml", ".toml", ".sql"}
files = sorted(
    name for name in set(changed) | staged | untracked
    if name and Path(name).suffix in extensions and ((root / name).is_file() or name in staged)
    and not (root / name).is_symlink()
)


def added_lines(name, cached):
    if name in untracked and not cached:
        yield from enumerate((root / name).read_text(encoding="utf-8").splitlines(), 1)
        return
    snapshot = ["--cached"] if cached else []
    diff = git("diff", "--no-ext-diff", "--no-textconv", "--unified=0", *snapshot, base, "--", name)
    number = None
    for line in diff.splitlines():
        hunk = re.match(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@", line)
        if hunk:
            number = int(hunk.group(1))
        elif number is not None:
            if line.startswith("+"):
                yield number, line[1:]
                number += 1
            elif line.startswith(" "):
                number += 1


def line_problem(name, line):
    if re.match(r"^(<<<<<<< |>>>>>>> )", line):
        return "unresolved merge marker"
    if name in untracked and line.rstrip() != line:
        return "trailing whitespace"
    if not name.endswith((".kt", ".kts")):
        return None
    imported = re.match(r"\s*import\s+([\w.]+)", line)
    if not imported:
        return None
    package = imported.group(1)
    if "/src/commonMain/" in name and package.startswith(("android.", "java.", "javax.", "platform.")):
        return "platform-only import in commonMain"
    if name.startswith("designsystem/src/"):
        other_project_module = package.startswith("com.jcotters.") and not package.startswith("com.jcotters.designsystem.")
        data_or_di = package.startswith(("org.koin.", "androidx.lifecycle.", "androidx.room.", "io.ktor.", "io.github.jan.supabase."))
        if other_project_module or data_or_di:
            return "domain, data or DI import in the design system"
    return None


failures = []
for name in files:
    print(name)
    for cached in (False, True):
        if cached and name not in staged:
            continue
        for number, line in added_lines(name, cached):
            reason = line_problem(name, line)
            if reason:
                label = "staged" if cached else "working tree"
                failures.append(f"{name}:{number}: {reason} ({label})")

if failures:
    print("\n".join(failures), file=sys.stderr)
    sys.exit(1)
print(f"Mechanical checks passed; {len(files)} changed code/config files against {base[:8]}, including untracked files.")
print("Required next: review full affected functions and callers with docs/workflow/code-review.md.")
print("This checks patch hygiene and selected added imports; it does not validate SOLID, syntax, behaviour or UI quality.")
PY
