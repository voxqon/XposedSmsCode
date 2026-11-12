import copy
import json
from pathlib import Path
import tempfile
import unittest

from validate_rules import RuleSetValidator, build_validator, load_json, validate_rule_file


class RuleValidationTest(unittest.TestCase):
    def setUp(self):
        self.validator = build_validator()
        self.document = {
            "specVersion": 1,
            "id": "cn.test",
            "name": "Test",
            "locale": "zh-CN",
            "updatedAt": "2026-10-30T00:00:00+08:00",
            "rules": [{
                "id": "login-code",
                "codeKeyword": "Code",
                "codeRegex": "[0-9]{6}",
                "examples": [{"body": "Code 123456", "expectedCode": "123456"}],
            }],
        }

    def validate_file(self, document=None, name="cn.test.json", raw=None):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / name
            path.write_text(
                raw if raw is not None else json.dumps(document or self.document),
                encoding="utf-8",
            )
            return validate_rule_file(path, self.validator)

    def test_valid_examples_and_nullable_optional_fields(self):
        rule = self.document["rules"][0]
        rule.update(company=None, senderRegex=None, packageNameHint=None, priority=0, rejectedSuffixes=[])
        summary, errors = self.validate_file()
        self.assertEqual([], errors)
        self.assertEqual((1, 1), (summary.rule_count, summary.example_count))

    def test_required_and_unknown_fields_at_every_object_level(self):
        for level in ("root", "rule", "example"):
            for operation in ("missing", "unknown"):
                with self.subTest(level=level, operation=operation):
                    document = copy.deepcopy(self.document)
                    target = {
                        "root": document,
                        "rule": document["rules"][0],
                        "example": document["rules"][0]["examples"][0],
                    }[level]
                    if operation == "missing":
                        target.pop(next(iter(target)))
                    else:
                        target["unexpected"] = True
                    self.assertTrue(self.validator.errors(document))

    def test_empty_arrays_and_invalid_array_types(self):
        for field in ("rules", "examples"):
            for value in ([], None, {}):
                with self.subTest(field=field, value=value):
                    document = copy.deepcopy(self.document)
                    target = document if field == "rules" else document["rules"][0]
                    target[field] = value
                    self.assertTrue(self.validator.errors(document))

    def test_integer_fields_reject_booleans_fractional_values_and_wrong_version(self):
        for version in (True, 1.5, "1", 2):
            self.document["specVersion"] = version
            self.assertTrue(self.validator.errors(self.document))
        self.document["specVersion"] = 1
        for priority in (False, 0.5, -1):
            self.document["rules"][0]["priority"] = priority
            self.assertTrue(self.validator.errors(self.document))

    def test_all_identifier_patterns(self):
        for name, value in (("id", "../test"), ("id", "cn.test\n"), ("locale", "Chinese")):
            document = copy.deepcopy(self.document)
            document[name] = value
            self.assertTrue(self.validator.errors(document))
        for name, value in (("id", "BAD ID"), ("packageNameHint", "single"), ("packageNameHint", "../app")):
            document = copy.deepcopy(self.document)
            document["rules"][0][name] = value
            self.assertTrue(self.validator.errors(document))

    def test_string_fields_must_be_nonempty(self):
        for field in ("company", "codeKeyword", "codeRegex", "senderRegex", "notes"):
            document = copy.deepcopy(self.document)
            document["rules"][0][field] = ""
            self.assertTrue(self.validator.errors(document), field)

    def test_suffixes_must_be_unique_nonempty_strings(self):
        for suffixes in (["x", "x"], [""], [3], "suffix"):
            self.document["rules"][0]["rejectedSuffixes"] = suffixes
            self.assertTrue(self.validator.errors(self.document))

    def test_timestamps_require_valid_date_time_and_timezone(self):
        for value in ("2026-10-30", "2026-10-30T12:00:00", "2026-02-30T12:00:00Z",
                      "2026-10-30T25:00:00Z", "2026-10-30T12:00:00+08:99"):
            self.document["updatedAt"] = value
            self.assertTrue(self.validator.errors(self.document), value)
        for value in ("2026-10-30t12:00:00z", "2026-10-30T12:00:00.125-05:30"):
            self.document["updatedAt"] = value
            self.assertEqual([], self.validator.errors(self.document))

    def test_rule_id_duplicates_are_rejected(self):
        self.document["rules"].append(copy.deepcopy(self.document["rules"][0]))
        self.assertIn("duplicate rule id", self.validate_file()[1][0])

    def test_invalid_regex_and_sender_regex_are_reported(self):
        for field in ("codeRegex", "senderRegex"):
            document = copy.deepcopy(self.document)
            document["rules"][0][field] = "("
            self.assertIn(f"invalid {field}", self.validate_file(document)[1][0])

    def test_examples_require_keyword_company_and_expected_code(self):
        for update, message in (
            ({"codeKeyword": "Missing"}, "does not contain keyword"),
            ({"company": "Missing"}, "does not contain company"),
            ({"codeRegex": "[0-9]{7}"}, "not matched by codeRegex"),
        ):
            document = copy.deepcopy(self.document)
            document["rules"][0].update(update)
            self.assertIn(message, self.validate_file(document)[1][0])

    def test_filename_must_match_rule_set(self):
        self.assertIn("file name must match", self.validate_file(name="other.json")[1][0])

    def test_invalid_json_duplicate_keys_and_non_json_numbers_are_reported(self):
        for raw in ("{", '{"id":"one","id":"two"}', '{"priority":NaN}', '{"priority":Infinity}'):
            summary, errors = self.validate_file(raw=raw)
            self.assertIsNone(summary)
            self.assertIn("invalid rule document", errors[0])

    def test_schema_extensions_fail_closed_and_remote_refs_are_rejected(self):
        schema = load_json(Path(__file__).resolve().parents[1] / "schemas/rule-set.schema.json")
        for constraint in ({"maxLength": 10}, {"$ref": "https://example.test/schema.json"},
                           {"format": "email"}, {"type": "number"}):
            modified = copy.deepcopy(schema)
            modified["properties"]["name"].update(constraint)
            with self.assertRaises(ValueError):
                RuleSetValidator(modified)


if __name__ == "__main__":
    unittest.main()
