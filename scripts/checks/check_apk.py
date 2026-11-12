#!/usr/bin/env python3
"""Verify the built APK's signature, permission policy and bundled assets."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FORBIDDEN_PERMISSIONS = {
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "com.android.vending.BILLING",
}


def run(*command: str) -> str:
    return subprocess.run(
        command, check=True, capture_output=True, text=True, encoding="utf-8", errors="replace",
    ).stdout


def dex_class_descriptors(package: zipfile.ZipFile) -> set[bytes]:
    descriptors = set()
    for name in package.namelist():
        if not re.fullmatch(r"classes(?:[2-9]|[1-9]\d+)?\.dex", name):
            continue
        data = package.read(name)
        if not data.startswith(b"dex\n"):
            raise ValueError(f"Invalid DEX file: {name}")
        _, strings_offset, _, types_offset = struct.unpack_from("<4I", data, 0x38)
        class_count, classes_offset = struct.unpack_from("<2I", data, 0x60)
        for index in range(class_count):
            type_index = struct.unpack_from("<I", data, classes_offset + index * 32)[0]
            string_index = struct.unpack_from("<I", data, types_offset + type_index * 4)[0]
            offset = struct.unpack_from("<I", data, strings_offset + string_index * 4)[0]
            # Skip the ULEB128 string length before reading the class descriptor.
            while data[offset] & 0x80:
                offset += 1
            offset += 1
            descriptors.add(data[offset:data.index(b"\0", offset)])
    return descriptors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--max-size-mib", type=float)
    args = parser.parse_args()
    if args.max_size_mib is not None and args.max_size_mib <= 0:
        parser.error("--max-size-mib must be positive")
    if args.apk:
        apk = args.apk.resolve(strict=True)
    else:
        output = ROOT / "app/build/outputs/apk/github/debug"
        metadata = json.loads((output / "output-metadata.json").read_text(encoding="utf-8"))
        elements = metadata["elements"]
        if len(elements) != 1:
            raise ValueError("Specify --apk when the build produces multiple APKs")
        apk = (output / elements[0]["outputFile"]).resolve(strict=True)
    size_mib = apk.stat().st_size / (1024 * 1024)
    if args.max_size_mib is not None and size_mib > args.max_size_mib:
        raise ValueError(
            f"APK is {size_mib:.2f} MiB, exceeding {args.max_size_mib:g} MiB; "
            "check that R8/resource shrinking is enabled and inspect runtime dependencies"
        )

    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or ROOT / ".tools/sdk")
    build_tools = sdk / "build-tools"
    tools = sorted(
        (path for path in build_tools.iterdir() if path.is_dir()),
        key=lambda path: tuple(int(part) for part in re.findall(r"\d+", path.name)),
        reverse=True,
    )
    suffix = ".exe" if os.name == "nt" else ""
    signer_suffix = ".bat" if os.name == "nt" else ""
    tool_dir = next((
        path for path in tools
        if (path / f"aapt2{suffix}").is_file() and (path / f"apksigner{signer_suffix}").is_file()
    ), None)
    if tool_dir is None:
        raise ValueError("Android SDK build-tools with aapt2 and apksigner are required")
    local_jdk = ROOT / ".tools/jdk27/jdk-27+35"
    if "JAVA_HOME" not in os.environ and local_jdk.is_dir():
        os.environ["JAVA_HOME"] = str(local_jdk)
    permissions = set(re.findall(
        r"uses-permission: name='([^']+)'",
        run(str(tool_dir / f"aapt2{suffix}"), "dump", "permissions", str(apk)),
    ))
    if not permissions:
        raise ValueError("Could not read APK permissions")
    if permissions & FORBIDDEN_PERMISSIONS:
        raise ValueError(f"Forbidden APK permissions: {sorted(permissions & FORBIDDEN_PERMISSIONS)}")
    if "android.permission.INTERNET" not in permissions:
        raise ValueError("Manual WebDAV backup requires the INTERNET permission")
    run(str(tool_dir / f"apksigner{signer_suffix}"), "verify", "--verbose", "--print-certs", str(apk))

    prefix = "assets/smscode-rules/"
    source_index = json.loads((ROOT / "smscode/rules/_meta/rules-index.json").read_text(encoding="utf-8"))
    with zipfile.ZipFile(apk) as package:
        names = package.namelist()
        if len(names) != len(set(names)):
            raise ValueError("APK contains duplicate ZIP entries")
        entries_path = "META-INF/xposed/java_init.list"
        source_entries = (ROOT / "app/src/main/resources" / entries_path).read_bytes()
        if package.read(entries_path) != source_entries:
            raise ValueError("APK Xposed entry metadata differs from the source")
        descriptors = dex_class_descriptors(package)
        for entry in source_entries.decode("utf-8").splitlines():
            entry = entry.strip()
            if entry and not entry.startswith("#"):
                descriptor = b"L" + entry.replace(".", "/").encode("utf-8") + b";"
                if descriptor not in descriptors:
                    raise ValueError(f"R8 removed or renamed the Xposed entry: {entry}")
        index = json.loads(package.read(prefix + "meta/rules-index.json"))
        if index != source_index:
            raise ValueError("APK rules index differs from the local index")
        expected_paths = {prefix + entry["path"] for entry in index["files"]}
        actual_paths = {name for name in names if name.startswith(prefix + "rules/") and not name.endswith("/")}
        if expected_paths != actual_paths:
            raise ValueError("APK contains missing or unindexed rule files")
        for entry in index["files"]:
            data = package.read(prefix + entry["path"])
            if len(data) != entry["size"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
                raise ValueError(f"Bundled rule integrity check failed: {entry['path']}")
        if package.read("assets/documents/PRIVACY.md") != (ROOT / "docs/PRIVACY.md").read_bytes():
            raise ValueError("APK privacy document differs from the local document")
    with apk.open("rb") as stream:
        checksum = hashlib.file_digest(stream, "sha256").hexdigest()
    print(
        f"APK signature, permissions, Xposed entry, {len(index['files'])} "
        "rule assets and privacy document verified."
    )
    print(f"Size: {size_mib:.2f} MiB")
    print(f"APK: {apk}")
    print(f"SHA256: {checksum}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, struct.error, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        if isinstance(error, subprocess.CalledProcessError):
            print(error.stdout or "", error.stderr or "", file=sys.stderr)
        sys.exit(1)
