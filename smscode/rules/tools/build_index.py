#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RULES_DIR = ROOT / "rules"
META_DIR = ROOT / "_meta"
OUTPUT = META_DIR / "rules-index.json"


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def serialize_index(index: dict) -> str:
    return json.dumps(index, ensure_ascii=False, indent=2) + "\n"


def current_timestamp() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def build_index(generated_at: str) -> dict:
    files = []
    for path in sorted(RULES_DIR.rglob("*.json"), key=lambda item: item.relative_to(ROOT).as_posix()):
        raw = path.read_bytes()
        data = json.loads(raw.decode("utf-8"))
        files.append(
            {
                "path": path.relative_to(ROOT).as_posix(),
                "id": data["id"],
                "name": data["name"],
                "locale": data["locale"],
                "updatedAt": data["updatedAt"],
                "ruleCount": len(data["rules"]),
                "sha256": hashlib.sha256(raw).hexdigest(),
                "size": len(raw),
            }
        )

    return {
        "sourceRepo": "magisk3171/smscode-rules",
        "branch": "main",
        "generatedAt": generated_at,
        "files": files,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build or verify the generated rules index.")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail if _meta/rules-index.json is missing or outdated.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    META_DIR.mkdir(parents=True, exist_ok=True)

    existing = load_json(OUTPUT) if OUTPUT.exists() else None
    if existing is not None:
        candidate = build_index(existing.get("generatedAt", current_timestamp()))
        if existing == candidate:
            message = f"{OUTPUT.relative_to(ROOT)} is up to date with {len(candidate['files'])} file(s)."
            print(message)
            return

    if args.check:
        print(
            f"ERROR: {OUTPUT.relative_to(ROOT)} is missing or outdated. Run `python3 tools/build_index.py`.",
            file=sys.stderr,
        )
        raise SystemExit(1)

    index = build_index(current_timestamp())
    OUTPUT.write_text(serialize_index(index), encoding="utf-8")
    print(f"Wrote {OUTPUT.relative_to(ROOT)} with {len(index['files'])} file(s).")


if __name__ == "__main__":
    main()
