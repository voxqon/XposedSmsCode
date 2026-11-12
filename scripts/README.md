# Scripts

All checks run locally. No external CI toolkit or remote workflow is required.

- `python scripts/check_local.py`: verify artifact checksums, local source policy,
  rule format/examples/index, signing policy, unit tests and the GitHub debug APK.
  The built APK's signature, permissions and bundled asset hashes are checked.
- `python scripts/check_local.py --checks-only`: source and artifact checks.
- `python scripts/checks/check_apk.py`: inspect the current GitHub debug APK;
  use `--apk PATH` for another local APK.
- `python scripts/release/check_release.py v3.4.2`: local release metadata check.
- `offline_dependencies.py`: verify the complete local Maven inventory. Its
  `--import-gradle-cache PATH --write-inventory` mode is for deliberate provisioning;
  normal builds never import, fetch or silently update dependencies.
- `config/god_file_limits.txt`: exact reviewed limits for large Kotlin files.

See `docs/OFFLINE_BUILD.md` for toolchain requirements. Shell release helpers
create only local tags; they do not publish or push.
