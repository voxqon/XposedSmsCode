#!/usr/bin/env python3
"""Validate release metadata locally, without fetching or publishing anything."""
from pathlib import Path
import argparse
import re
import subprocess
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[2]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tag")
    args = parser.parse_args()
    catalog = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8"))
    version = catalog["versions"]["versionName"]
    code = int(catalog["versions"]["versionCode"])
    if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?", args.tag):
        parser.error("Tag must have the form vX.Y.Z")
    if args.tag != f"v{version}" or code <= 0:
        parser.error("Tag must match versionName and versionCode must be positive")
    for locale in ("en-US", "zh-CN"):
        notes = ROOT / "distribution/whatsnew" / f"whatsnew-{locale}"
        text = notes.read_text(encoding="utf-8").strip()
        if not text or len(text) > 500:
            parser.error(f"Release notes for {locale} must contain 1-500 characters")
    subprocess.run([sys.executable, str(ROOT / "scripts/checks/check_security.py")], check=True)
    print(f"Release metadata verified: {args.tag}, versionCode={code}")


if __name__ == "__main__":
    main()
