#!/usr/bin/env bash
# Optional local maintenance; no remote source is fetched.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "${ROOT_DIR}/build-logic/scripts/sync_consumer_gradle_assets.sh" "${ROOT_DIR}"
