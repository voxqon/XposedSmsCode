#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RULES_DIR = ROOT / "rules"
SCHEMA_PATH = ROOT / "schemas" / "rule-set.schema.json"
TIMESTAMP = re.compile(
    r"[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}"
    r"(?:\.[0-9]+)?(?:[Zz]|[+-](?:[01][0-9]|2[0-3]):[0-5][0-9])"
)


class RuleSetValidator:
    """Validate only the local rule schema's subset; never resolve remote schemas."""

    KEYWORDS = {
        "$schema", "$id", "title", "$defs", "$ref", "anyOf", "type", "required",
        "properties", "additionalProperties", "items", "minItems", "uniqueItems",
        "minLength", "minimum", "const", "pattern", "format",
    }
    TYPES = {
        "object": dict, "array": list, "string": str, "integer": int,
        "null": type(None),
    }

    def __init__(self, schema: dict):
        self.schema = schema
        self._check_contract(schema)

    def _check_contract(self, schema: dict) -> None:
        unknown = set(schema) - self.KEYWORDS
        if unknown:
            raise ValueError(f"Unsupported rule schema constraints: {sorted(unknown)}")
        if "type" in schema and schema["type"] not in self.TYPES:
            raise ValueError(f"Unsupported rule schema type: {schema['type']}")
        if "$ref" in schema:
            self._resolve(schema["$ref"])
        if "format" in schema and schema["format"] != "date-time":
            raise ValueError(f"Unsupported rule schema format: {schema['format']}")
        if "pattern" in schema:
            re.compile(schema["pattern"])
        for key in ("$defs", "properties"):
            for child in schema.get(key, {}).values():
                self._check_contract(child)
        for child in schema.get("anyOf", []):
            self._check_contract(child)
        if "items" in schema:
            self._check_contract(schema["items"])

    def _resolve(self, reference: str) -> dict:
        prefix = "#/$defs/"
        if not reference.startswith(prefix) or reference[len(prefix):] not in self.schema.get("$defs", {}):
            raise ValueError(f"Unsupported rule schema reference: {reference}")
        return self.schema["$defs"][reference[len(prefix):]]

    def errors(self, value: object, schema: dict | None = None, path: tuple = ()) -> list[tuple]:
        schema = self.schema if schema is None else schema
        if "$ref" in schema:
            return self.errors(value, self._resolve(schema["$ref"]), path)
        if "anyOf" in schema:
            if any(not self.errors(value, choice, path) for choice in schema["anyOf"]):
                return []
            return [(path, "value does not match an allowed field format")]
        kind = schema.get("type")
        if kind and type(value) is not self.TYPES[kind]:
            return [(path, f"expected {kind}")]
        errors = []
        if "const" in schema and value != schema["const"]:
            errors.append((path, f"expected constant {schema['const']!r}"))
        if kind == "object":
            for name in schema.get("required", []):
                if name not in value:
                    errors.append((path + (name,), "required field is missing"))
            properties = schema.get("properties", {})
            for name, child in value.items():
                if name in properties:
                    errors.extend(self.errors(child, properties[name], path + (name,)))
                elif schema.get("additionalProperties") is False:
                    errors.append((path + (name,), "unknown field"))
        elif kind == "array":
            if len(value) < schema.get("minItems", 0):
                errors.append((path, "array has too few items"))
            if schema.get("uniqueItems") and any(item in value[:index] for index, item in enumerate(value)):
                errors.append((path, "array items must be unique"))
            for index, child in enumerate(value):
                errors.extend(self.errors(child, schema["items"], path + (index,)))
        elif kind == "string":
            if len(value) < schema.get("minLength", 0):
                errors.append((path, "string must not be empty"))
            if "pattern" in schema and re.fullmatch(schema["pattern"], value) is None:
                errors.append((path, "string does not match the required pattern"))
            if schema.get("format") == "date-time":
                try:
                    if TIMESTAMP.fullmatch(value) is None:
                        raise ValueError("timestamp must include a time and timezone")
                    datetime.fromisoformat(value.upper().replace("Z", "+00:00"))
                except ValueError:
                    errors.append((path, "expected a valid date-time with timezone"))
        elif kind == "integer" and value < schema.get("minimum", value):
            errors.append((path, f"value must be at least {schema['minimum']}"))
        return errors


@dataclass(frozen=True)
class RuleFileSummary:
    path: Path
    rule_set_id: str
    rule_count: int
    example_count: int


def unique_object(pairs: list[tuple]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON field: {key}")
        result[key] = value
    return result


def invalid_constant(value: str) -> None:
    raise ValueError(f"invalid JSON constant: {value}")


def load_json(path: Path) -> dict:
    return json.loads(
        path.read_text(encoding="utf-8"),
        object_pairs_hook=unique_object,
        parse_constant=invalid_constant,
    )


def format_json_path(parts: list[object]) -> str:
    if not parts:
        return "$"

    formatted = "$"
    for part in parts:
        if isinstance(part, int):
            formatted += f"[{part}]"
        else:
            formatted += f".{part}"
    return formatted


def collect_schema_errors(path: Path, data: dict, validator: RuleSetValidator) -> list[str]:
    errors = sorted(
        validator.errors(data),
        key=lambda error: [str(part) for part in error[0]],
    )
    return [
        f"{path}: schema validation failed at {format_json_path(list(parts))}: {message}"
        for parts, message in errors
    ]


def validate_rule_file(path: Path, validator: RuleSetValidator) -> tuple[RuleFileSummary | None, list[str]]:
    try:
        data = load_json(path)
    except (OSError, ValueError) as exc:
        return None, [f"{path}: invalid rule document: {exc}"]
    errors = collect_schema_errors(path, data, validator)

    if errors:
        return None, errors

    rule_set_id = data["id"]
    expected_name = f"{rule_set_id}.json"
    if path.name != expected_name:
        errors.append(f"{path}: file name must match rule-set id '{expected_name}'")

    example_count = 0
    seen_rule_ids = set()
    for index, rule in enumerate(data["rules"], start=1):
        rule_id = rule["id"]
        if rule_id in seen_rule_ids:
            errors.append(f"{path}: duplicate rule id '{rule_id}'")
            continue
        seen_rule_ids.add(rule_id)

        keyword = rule["codeKeyword"]
        code_regex = rule["codeRegex"]
        company = rule.get("company")

        try:
            pattern = re.compile(code_regex)
        except re.error as exc:
            errors.append(f"{path}: rule '{rule_id}' invalid codeRegex: {exc}")
            continue

        sender_regex = rule.get("senderRegex")
        if sender_regex:
            try:
                re.compile(sender_regex)
            except re.error as exc:
                errors.append(f"{path}: rule '{rule_id}' invalid senderRegex: {exc}")
                continue

        for example_index, example in enumerate(rule["examples"], start=1):
            body = example["body"]
            expected_code = example["expectedCode"]
            if company and company not in body:
                errors.append(
                    f"{path}: rule '{rule_id}' example #{example_index} body does not contain company '{company}'"
                )
            if keyword not in body:
                errors.append(
                    f"{path}: rule '{rule_id}' example #{example_index} body does not contain keyword '{keyword}'"
                )
            matches = [m.group(0) for m in pattern.finditer(body)]
            if expected_code not in matches:
                errors.append(
                    f"{path}: rule '{rule_id}' example expectedCode '{expected_code}' "
                    f"not matched by codeRegex"
                )
                continue
            example_count += 1

    if errors:
        return None, errors

    return RuleFileSummary(path=path, rule_set_id=rule_set_id, rule_count=len(data["rules"]), example_count=example_count), []


def build_validator() -> RuleSetValidator:
    return RuleSetValidator(load_json(SCHEMA_PATH))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate smscode-rules documents.")
    return parser.parse_args()


def main() -> None:
    parse_args()

    files = sorted(RULES_DIR.rglob("*.json"), key=lambda path: path.relative_to(ROOT).as_posix())
    if not files:
        print(f"ERROR: no rule files found under {RULES_DIR}", file=sys.stderr)
        raise SystemExit(1)

    validator = build_validator()
    errors: list[str] = []
    summaries: list[RuleFileSummary] = []
    total_rules = 0
    total_examples = 0
    seen_rule_set_ids: dict[str, Path] = {}

    for path in files:
        summary, file_errors = validate_rule_file(path, validator)
        if file_errors:
            errors.extend(file_errors)
            continue

        assert summary is not None
        previous = seen_rule_set_ids.get(summary.rule_set_id)
        if previous is not None:
            errors.append(
                f"{path}: duplicate rule-set id '{summary.rule_set_id}' already used by {previous.relative_to(ROOT)}"
            )
            continue
        seen_rule_set_ids[summary.rule_set_id] = path

        summaries.append(summary)
        total_rules += summary.rule_count
        total_examples += summary.example_count

    if errors:
        for message in errors:
            print(f"ERROR: {message}", file=sys.stderr)
        raise SystemExit(1)

    for summary in summaries:
        print(
            f"PASS: {summary.path.relative_to(ROOT)} "
            f"rules={summary.rule_count} examples={summary.example_count}"
        )

    print(f"Validated {len(files)} file(s), {total_rules} rule(s), {total_examples} example(s).")


if __name__ == "__main__":
    main()
