#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v sips >/dev/null 2>&1 || { printf 'App icon generation requires macOS sips.\n' >&2; exit 2; }
source_icon="$repo_dir/assets/branding/movers-icon.jpeg"
[[ -f "$source_icon" ]] || { printf 'Missing assets/branding/movers-icon.jpeg.\n' >&2; exit 2; }

resize_icon() {
    local pixels="$1" destination="$2"
    mkdir -p "$(dirname -- "$destination")"
    sips -s format png -z "$pixels" "$pixels" "$source_icon" --out "$destination" >/dev/null
}

android_res="$repo_dir/app/src/main/res"
resize_icon 512 "$android_res/drawable-nodpi/app_icon_artwork.png"
resize_icon 1024 "$repo_dir/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png"
printf 'Generated Android and iOS app icons from assets/branding/movers-icon.jpeg.\n'
