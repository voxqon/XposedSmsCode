import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import build_index


class RuleIndexTest(unittest.TestCase):
    def test_checksums_cover_exact_packaged_bytes_including_crlf(self):
        document = {
            "id": "cn.test", "name": "Test", "locale": "zh-CN",
            "updatedAt": "2026-10-30T00:00:00Z", "rules": [],
        }
        for newline in ("\n", "\r\n"):
            with self.subTest(newline=repr(newline)), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                rules = root / "rules"
                rules.mkdir()
                raw = json.dumps(document, indent=2).replace("\n", newline).encode("utf-8")
                (rules / "cn.test.json").write_bytes(raw)
                with patch.object(build_index, "ROOT", root), patch.object(build_index, "RULES_DIR", rules):
                    entry = build_index.build_index("2026-10-30T00:00:00Z")["files"][0]
                self.assertEqual(len(raw), entry["size"])
                self.assertEqual(hashlib.sha256(raw).hexdigest(), entry["sha256"])
                self.assertEqual("rules/cn.test.json", entry["path"])


if __name__ == "__main__":
    unittest.main()
