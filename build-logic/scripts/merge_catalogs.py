#!/usr/bin/env python3
"""Merge the shared catalog's [versions] into a consumer's libs.versions.toml.

Line-preserving, order-preserving merge:
- The consumer file owns its layout. Existing entries keep their line and
  position; only the right-hand side of a [versions] assignment may change.
- In [versions] the newer value wins, so a shared-catalog bump reaches every
  consumer while a consumer Renovate bump is never rolled back by a later
  sync. Keys listed in CONSUMER_OVERRIDE_VERSION_KEYS always keep the
  consumer value.
- Shared-only keys (new shared entries) are appended at the end of the
  consumer's [versions] section.
- [libraries] and [plugins] are consumer-owned and never touched. The shared
  catalog mirrors coordinates only so Renovate can resolve updates for the
  shared [versions] keys; propagating those sections would inject aliases the
  consumer build files never reference.

Usage: merge_catalogs.py <shared.toml> <consumer.toml>
"""
import re
import sys
from pathlib import Path

# Keys where the CONSUMER value wins even though shared defines one.
# build-logic needs these present in its own catalog (build.gradle.kts /
# MagiskCatalog.kt read javaBytecode), but some consumers cannot move to
# the shared value (xinyi crashes on bytecode 27), so the consumer pin wins.
CONSUMER_OVERRIDE_VERSION_KEYS = {"javaBytecode"}


def entry_value(line):
    """Return the assignment's right-hand side, without quotes."""
    return line.split("=", 1)[1].strip().strip('"').strip("'").strip()


def parse_version(value):
    """Split a version string into its numeric core and any release marker."""
    text = value.strip()
    core = re.split(r"[-+]", text, maxsplit=1)[0]
    marker = text[len(core):]
    return [int(n) for n in re.findall(r"\d+", core)], marker


def marker_key(marker):
    """Order a release marker, comparing embedded numbers numerically.

    String comparison alone would rank -alpha11 below -alpha2, which is wrong
    for the prerelease suffixes this catalog carries (datastore 1.3.0-alpha11).
    """
    return tuple(
        (1, int(part), "") if part.isdigit() else (0, 0, part)
        for part in re.findall(r"\d+|\D+", marker)
    )


def newer_version(left, right):
    """Return the newer of two version strings, falling back to `right`.

    Compares numeric segments first (missing segments count as zero), then
    lets a release beat a prerelease suffix -- 2.5.0 is newer than 2.5.0-Beta1.
    """
    left_core, left_marker = parse_version(left)
    right_core, right_marker = parse_version(right)
    if not left_core or not right_core:
        return right
    width = max(len(left_core), len(right_core))
    left_core += [0] * (width - len(left_core))
    right_core += [0] * (width - len(right_core))
    for a, b in zip(left_core, right_core):
        if a != b:
            return left if a > b else right
    if left_marker == right_marker:
        return left
    if not left_marker:
        return left
    if not right_marker:
        return right
    return left if marker_key(left_marker) > marker_key(right_marker) else right


def parse_section_keys(text, section):
    """Return {key: value} for a section."""
    entries = {}
    inside = False
    for line in text.splitlines():
        if re.match(rf"^\[{section}\]", line):
            inside = True
            continue
        if re.match(r"^\[", line):
            inside = False
            continue
        m = re.match(r"^([a-zA-Z0-9_.-]+)\s*=", line)
        if m and inside:
            entries[m.group(1)] = entry_value(line)
    return entries


shared_text = Path(sys.argv[1]).read_text()
consumer_path = Path(sys.argv[2])
consumer_text = consumer_path.read_text() if consumer_path.exists() else ""

shared_versions = parse_section_keys(shared_text, "versions")

if not consumer_text:
    print("consumer catalog missing; nothing to merge")
    sys.exit(0)

lines = consumer_text.splitlines()
out = []
inside_versions = False
versions_closed = False
seen_keys = set()
changed = 0

for line in lines:
    if re.match(r"^\[versions\]", line):
        inside_versions = True
        out.append(line)
        continue
    if inside_versions and re.match(r"^\[", line):
        # first section after [versions]: append shared-only keys here
        inside_versions = False
        versions_closed = True
        for key, value in shared_versions.items():
            if key not in seen_keys:
                out.append(f'{key} = "{value}"')
                changed += 1
    m = re.match(r'^([a-zA-Z0-9_.-]+)(\s*=\s*)(".*")\s*$', line)
    if inside_versions and m:
        key = m.group(1)
        seen_keys.add(key)
        if key in shared_versions and key not in CONSUMER_OVERRIDE_VERSION_KEYS:
            current = entry_value(line)
            incoming = shared_versions[key]
            if current != incoming and newer_version(current, incoming) == incoming:
                out.append(f"{key}{m.group(2)}\"{incoming}\"")
                changed += 1
                continue
    out.append(line)

if inside_versions and not versions_closed:
    # file ended inside [versions]
    for key, value in shared_versions.items():
        if key not in seen_keys:
            out.append(f'{key} = "{value}"')
            changed += 1

new_text = "\n".join(out) + "\n"
consumer_path.write_text(new_text)

shared_total = len(shared_versions)
local_total = len(parse_section_keys(consumer_text, "versions"))
print(f"Merged: {shared_total} shared + {local_total} local, {changed} change(s)")
