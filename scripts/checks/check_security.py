#!/usr/bin/env python3
"""Guard standalone source ownership and the privacy policy of active modules."""
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def check() -> list[str]:
    errors = []
    gitlinks = subprocess.run(
        ["git", "ls-files", "--stage"], cwd=ROOT, check=True, text=True, capture_output=True
    ).stdout
    if any(line.startswith("160000 ") for line in gitlinks.splitlines()):
        errors.append("External Git submodules remain in the index")
    if (ROOT / ".gitmodules").exists():
        errors.append(".gitmodules must not reintroduce remote source dependencies")
    for name in ("build-logic", "magisk-ui-kit", "magisk-xposed-kit", "smscode/core", "smscode/rules"):
        if (ROOT / name / ".git").exists():
            errors.append(f"Embedded Git repository remains: {name}")
        if not (ROOT / name / "LICENSE").is_file():
            errors.append(f"Vendored source license missing: {name}")
    settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    if ":magisk-ui-kit:update" in settings:
        errors.append("Upstream APK updater must not be part of the build")
    if ":magisk-ui-kit:billing" in settings:
        errors.append("Online billing must not be part of the build")
    for path in ("settings.gradle.kts", "build.gradle.kts", "build-logic/settings.gradle.kts"):
        text = (ROOT / path).read_text(encoding="utf-8")
        if re.search(r"\b(?:google|mavenCentral|gradlePluginPortal)\s*\(|https?://[^\s]*[\"']", text):
            errors.append(f"Remote dependency repository returned: {path}")
    for base in (ROOT, ROOT / "build-logic/shared/gradle"):
        wrapper_dir = base / ("gradle/wrapper" if base == ROOT else "wrapper")
        wrapper = (wrapper_dir / "gradle-wrapper.properties").read_text(encoding="utf-8")
        if re.search(r"^distributionUrl=.*https?", wrapper, re.MULTILINE):
            errors.append(f"Gradle distribution must be supplied locally: {wrapper_dir.relative_to(ROOT)}")
        for script in ("gradlew", "gradlew.bat"):
            if "--offline" not in (base / script).read_text(encoding="utf-8"):
                errors.append(f"Wrapper must default to offline: {(base / script).relative_to(ROOT)}")
        properties_path = base / ("gradle.properties" if base == ROOT else "gradle.properties.common")
        properties = properties_path.read_text(encoding="utf-8")
        for key in ("android.builder.sdkDownload", "org.gradle.java.installations.auto-download"):
            if not re.search(rf"^{re.escape(key)}=false$", properties, re.MULTILINE):
                errors.append(f"Automatic tool download must be disabled: {key} in {properties_path.relative_to(ROOT)}")
    manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
    if manifest.find("application").get(ANDROID + "allowBackup") != "false":
        errors.append("Android automatic backup must remain disabled")
    for flavor in ("github", "play"):
        config = ET.parse(ROOT / f"app/src/{flavor}/res/xml/network_security_config.xml").getroot()
        if config.find("base-config").get("cleartextTrafficPermitted") != "false":
            errors.append(f"Cleartext network traffic enabled: {flavor}")
    for permission in manifest.findall("uses-permission"):
        if permission.get(ANDROID + "name") in {
            "android.permission.ACCESS_NETWORK_STATE",
            "com.android.vending.BILLING", "android.permission.REQUEST_INSTALL_PACKAGES",
        } and permission.get("{http://schemas.android.com/tools}node") != "remove":
            errors.append(f"Online permission enabled: {permission.get(ANDROID + 'name')}")
    active_roots = ("app", "hook", "runtime", "presentation", "smscode/core", "magisk-xposed-kit", "magisk-ui-kit")
    denied = re.compile(
        r"com\.magisk317\.mobile\.entitlement|otel\.gitlab-o11y\.com|DexClassLoader|InMemoryDexClassLoader"
    )
    network_transport = re.compile(
        r"\bimport\s+(?:okhttp3|retrofit2|java\.net\.(?:URL\b|HttpURLConnection|Socket\b))"
        r"|\bopenConnection\s*\("
    )
    allowed_network_sources = {
        "runtime/src/main/kotlin/io/github/magisk317/smscode/feature/backup/WebDavClient.kt",
        "runtime/src/test/kotlin/io/github/magisk317/smscode/feature/backup/WebDavClientTest.kt",
    }
    for name in active_roots:
        for path in (ROOT / name).rglob("*"):
            if "build" in path.relative_to(ROOT).parts or path.suffix not in {".kt", ".kts", ".xml"}:
                continue
            if path.is_file():
                source = path.read_text(encoding="utf-8")
                relative = path.relative_to(ROOT).as_posix()
                if denied.search(source):
                    errors.append(f"Removed remote control or dynamic-code integration returned: {relative}")
                if network_transport.search(source) and relative not in allowed_network_sources:
                    errors.append(f"Network transport outside manual WebDAV backup: {relative}")
    transport_root = ROOT / "magisk-xposed-kit/logging/src"
    for path in transport_root.rglob("*.kt"):
        if any(part.endswith("Test") for part in path.relative_to(transport_root).parts):
            continue
        source = path.read_text(encoding="utf-8")
        if re.search(r"\bimport\s+(?:java\.net\.|okhttp3\.)|\bopenConnection\s*\(", source):
            errors.append(f"Logging must have no network transport: {path.relative_to(ROOT)}")
    for path in [*(ROOT / ".github").rglob("*.yml"), *(ROOT / ".gitlab").rglob("*.yml")]:
        source = path.read_text(encoding="utf-8")
        if "magisk-ci-toolkit" in source or "submodules: recursive" in source:
            errors.append(f"Private CI dependency returned: {path.relative_to(ROOT)}")
    return errors


if __name__ == "__main__":
    failures = check()
    for failure in failures:
        print(f"ERROR: {failure}", file=sys.stderr)
    if failures:
        raise SystemExit(1)
    print("Standalone source and privacy policy checks passed.")
