#!/usr/bin/env python3
"""Materialize Maven artifacts locally and verify a deterministic SHA-256 inventory."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = ROOT / "third_party" / "maven"
INVENTORY = ROOT / "third_party" / "artifacts.json"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def import_cache(cache):
    count = 0
    for source in sorted(cache.glob("*/*/*/*/*")):
        if not source.is_file():
            continue
        group, artifact, version, _, name = source.relative_to(cache).parts
        target = REPOSITORY.joinpath(*group.split("."), artifact, version, name)
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and digest(target) != digest(source):
            raise ValueError(f"Conflicting cached artifacts: {group}:{artifact}:{version}/{name}")
        if not target.exists():
            shutil.copyfile(source, target)
        count += 1
    if not count:
        raise ValueError("No Gradle module artifacts found")
    print(f"Imported {count} cached files into the local Maven repository.")


def inventory():
    entries = []
    for path in sorted(REPOSITORY.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(REPOSITORY)
        parts = relative.parts
        item = {
            "path": relative.as_posix(),
            "coordinate": f"{'.'.join(parts[:-3])}:{parts[-3]}:{parts[-2]}",
            "size": path.stat().st_size,
            "sha256": digest(path),
        }
        if path.suffix == ".pom":
            root = ET.parse(path).getroot()
            namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
            item["licenses"] = [
                {"name": license.findtext("m:name", "", namespace),
                 "url": license.findtext("m:url", "", namespace)}
                for license in root.findall("m:licenses/m:license", namespace)
            ]
        entries.append(item)
    return entries


def restore_maven_filenames():
    # Gradle's cache sometimes stores the display name instead of the repository URL name.
    for metadata in REPOSITORY.rglob("*.module"):
        data = json.loads(metadata.read_text(encoding="utf-8"))
        for variant in data.get("variants", []):
            for artifact in variant.get("files", []):
                name, url = artifact["name"], artifact["url"]
                if "/" in url or "\\" in url or url in {".", ".."}:
                    continue
                target = metadata.parent / url
                source = metadata.parent / name
                if target.exists() or not source.is_file():
                    continue
                expected = artifact.get("sha256")
                if expected and digest(source) != expected:
                    raise ValueError(f"Published checksum mismatch: {source.relative_to(REPOSITORY)}")
                shutil.copyfile(source, target)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--import-gradle-cache", type=Path)
    parser.add_argument("--write-inventory", action="store_true")
    args = parser.parse_args()
    if args.import_gradle_cache:
        import_cache(args.import_gradle_cache.resolve(strict=True))
        restore_maven_filenames()
    current = inventory()
    if not current:
        raise ValueError("Local Maven repository is empty")
    if args.write_inventory:
        INVENTORY.write_text(json.dumps(current, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")
    elif json.loads(INVENTORY.read_text(encoding="utf-8")) != current:
        raise ValueError("Local artifact inventory differs: missing, changed or unexpected files")
    print(f"Verified {len(current)} local artifact files ({sum(item['size'] for item in current)} bytes).")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, ET.ParseError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)
