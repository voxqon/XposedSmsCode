#!/usr/bin/env python3
"""Run local security checks, tests and the debug build without downloading dependencies."""
import argparse
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def run(*command):
    subprocess.run(command, cwd=ROOT, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checks-only", action="store_true")
    args = parser.parse_args()
    run(sys.executable, "scripts/checks/check_security.py")
    run(sys.executable, "scripts/offline_dependencies.py")
    run(sys.executable, "-m", "unittest", "discover", "-s", "smscode/rules/tools", "-p", "test_*.py")
    run(sys.executable, "smscode/rules/tools/validate_rules.py")
    run(sys.executable, "smscode/rules/tools/build_index.py", "--check")
    if args.checks_only:
        return
    local_jdk = ROOT / ".tools/jdk27/jdk-27+35"
    local_sdk = ROOT / ".tools/sdk"
    if "JAVA_HOME" not in os.environ and local_jdk.is_dir():
        os.environ["JAVA_HOME"] = str(local_jdk)
    if "ANDROID_HOME" not in os.environ and local_sdk.is_dir():
        os.environ["ANDROID_HOME"] = str(local_sdk)
    wrapper = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    run(
        wrapper, "--offline", "--console=plain", "--no-configuration-cache",
        ":build-logic:test", "--tests", "SigningTaskPolicyTest",
        "--tests", "AndroidApplicationPluginFunctionalTest",
    )
    run(
        wrapper, "--offline", "--console=plain", "--no-configuration-cache",
        f"-PpythonExecutable={sys.executable}",
        ":smscode-core:verification:test", ":smscode-core:domain:test",
        ":smscode-core:rule:test", ":smscode-core:contract:test",
        ":magisk-xposed-kit:logging:jvmTest", ":smscode-core:runtime:testDebugUnitTest",
        ":magisk-ui-kit:testAndroidHostTest",
        ":runtime:testGithubDebugUnitTest", ":app:testGithubDebugUnitTest",
        ":presentation:testGithubDebugUnitTest", "verifyModuleBoundaries",
        ":app:assembleGithubDebug",
    )
    run(sys.executable, "scripts/checks/check_apk.py", "--max-size-mib", "15")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
