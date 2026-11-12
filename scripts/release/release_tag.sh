#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
tag="${1:?Usage: release_tag.sh vX.Y.Z}"
python3 "$root/scripts/release/check_release.py" "$tag"
if [[ -n "$(git -C "$root" status --porcelain)" ]]; then
  echo "Commit or preserve local changes before creating a release tag." >&2
  exit 1
fi
bash "$root/gradlew" -p "$root" verifyModuleBoundaries :smscode-core:verification:test
git -C "$root" tag --sign --message "Release $tag" -- "$tag"
