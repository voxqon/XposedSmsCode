#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)/shared/gradle"
COMMON_PROPS_FILE="${SOURCE_DIR}/gradle.properties.common"
BEGIN_MARKER="# BEGIN SHARED GRADLE PROPERTIES (managed by magisk-build-logic)"
END_MARKER="# END SHARED GRADLE PROPERTIES (managed by magisk-build-logic)"

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 <consumer-repo> [<consumer-repo>...]" >&2
  exit 2
fi

for target_dir in "$@"; do
  if [ ! -d "$target_dir" ]; then
    echo "ERROR: target repo does not exist: $target_dir" >&2
    exit 1
  fi

  echo "Syncing shared Gradle assets into: $target_dir"

  install -d "${target_dir}/gradle/wrapper"
  install -m 0755 "${SOURCE_DIR}/gradlew" "${target_dir}/gradlew"
  install -m 0644 "${SOURCE_DIR}/gradlew.bat" "${target_dir}/gradlew.bat"
  install -m 0644 "${SOURCE_DIR}/wrapper/gradle-wrapper.jar" "${target_dir}/gradle/wrapper/gradle-wrapper.jar"
  install -m 0644 "${SOURCE_DIR}/wrapper/gradle-wrapper.properties" "${target_dir}/gradle/wrapper/gradle-wrapper.properties"
  # Merge shared version catalog with consumer-specific entries.
  # Consumer entries override shared entries with the same alias.
  python3 "${SCRIPT_DIR}/merge_catalogs.py" \
    "${SOURCE_DIR}/libs.versions.toml" \
    "${target_dir}/gradle/libs.versions.toml"

  TARGET_GRADLE_PROPS="${target_dir}/gradle.properties" \
  COMMON_PROPS_FILE="${COMMON_PROPS_FILE}" \
  BEGIN_MARKER="${BEGIN_MARKER}" \
  END_MARKER="${END_MARKER}" \
  python3 - <<'PY'
from pathlib import Path
import os
import re

target = Path(os.environ["TARGET_GRADLE_PROPS"])
common = Path(os.environ["COMMON_PROPS_FILE"])
begin_marker = os.environ["BEGIN_MARKER"]
end_marker = os.environ["END_MARKER"]
remove_only_keys = {
    "org.gradle.daemon.performance.disable-logging",
    "buildSplits",
}

common_lines = common.read_text(encoding="utf-8").splitlines()
managed_keys = []
for line in common_lines:
    stripped = line.strip()
    if not stripped or stripped.startswith("#"):
        continue
    managed_keys.append(stripped.split("=", 1)[0].strip())

if target.exists():
    lines = target.read_text(encoding="utf-8").splitlines()
else:
    lines = []

filtered = []
inside_managed_block = False
for line in lines:
    stripped = line.strip()
    if stripped == begin_marker:
        inside_managed_block = True
        continue
    if stripped == end_marker:
        inside_managed_block = False
        continue
    if inside_managed_block:
        continue

    is_managed_key = False
    for key in managed_keys:
        if re.match(rf"^{re.escape(key)}\s*=", stripped):
            is_managed_key = True
            break
    if not is_managed_key:
        for key in remove_only_keys:
            if re.match(rf"^{re.escape(key)}\s*=", stripped):
                is_managed_key = True
                break
    if is_managed_key:
        continue
    filtered.append(line)

while filtered and filtered[-1] == "":
    filtered.pop()

if filtered:
    filtered.append("")
filtered.append(begin_marker)
filtered.extend(common_lines)
filtered.append(end_marker)
filtered.append("")

target.write_text("\n".join(filtered), encoding="utf-8")
PY
done
