#!/usr/bin/env python3
"""Verify the documented agent configuration schema without creating tracked outputs."""

from __future__ import annotations

import dataclasses
import importlib.util
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

from jsonschema import Draft202012Validator


ROOT = Path(__file__).resolve().parents[2]
SCHEMA_PATH = ROOT / "docs" / "agents" / "reference" / "agent-schema.json"
ROOT_CLASS = "AgentConfig"
PY_TYPES = {"string": "str", "integer": "int", "number": "float", "boolean": "bool", "object": "dict[str, Any]"}
JAVA_TYPES = {"string": "String", "integer": "Integer", "number": "Double", "boolean": "Boolean", "object": "Map<String, Object>"}


def class_name(name: str) -> str:
    return ROOT_CLASS if name == "#" else name.rsplit("/", 1)[-1][:1].upper() + name.rsplit("/", 1)[-1][1:]


def schema_type(node: Any, scalar, array) -> str:
    if not isinstance(node, dict):
        return scalar(None)
    if "$ref" in node:
        return class_name(node["$ref"])
    if "oneOf" in node or "anyOf" in node:
        return scalar(None)
    kind = node.get("type")
    if isinstance(kind, list):
        kind = next((item for item in kind if item != "null"), None)
    if kind == "array":
        return array(schema_type(node.get("items", {}), scalar, array))
    return scalar(kind)


def python_type(node: Any) -> str:
    return schema_type(node, lambda kind: PY_TYPES.get(kind, "Any"), lambda item: f"list[{item}]")


def java_type(node: Any) -> str:
    return schema_type(node, lambda kind: JAVA_TYPES.get(kind, "Object"), lambda item: f"List<{item}>")


def models(schema: dict[str, Any]) -> list[tuple[str, dict[str, Any]]]:
    return [(ROOT_CLASS, schema)] + [(class_name(name), node) for name, node in schema.get("$defs", {}).items()]


def write_python(path: Path, schema: dict[str, Any]) -> None:
    parts = [
        "from __future__ import annotations",
        "from dataclasses import dataclass",
        "from typing import Any, Optional",
        "",
    ]
    for name, node in models(schema):
        properties = node.get("properties", {})
        parts.append("@dataclass")
        parts.append(f"class {name}:")
        if properties:
            parts.extend(f"    {field}: Optional[{python_type(value)}] = None" for field, value in properties.items())
        else:
            parts.append("    pass")
        parts.append("")
    path.write_text("\n".join(parts), encoding="utf-8")


def write_java(path: Path, schema: dict[str, Any]) -> None:
    records = []
    for name, node in models(schema):
        properties = node.get("properties", {})
        components = ", ".join(f"{java_type(value)} {field}" for field, value in properties.items())
        records.append(f"    public record {name}({components}) {{}}")
    path.write_text(
        "import java.util.List;\nimport java.util.Map;\n\n"
        "public final class AgentConfigModel {\n"
        "    private AgentConfigModel() {}\n\n"
        + "\n".join(records)
        + "\n}\n",
        encoding="utf-8",
    )


def check_generated_fields(module: Any, schema: dict[str, Any]) -> None:
    for name, node in models(schema):
        actual = {field.name for field in dataclasses.fields(getattr(module, name))}
        expected = set(node.get("properties", {}))
        if actual != expected:
            raise AssertionError(f"{name} fields differ from schema: {actual ^ expected}")


def main() -> None:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    Draft202012Validator.check_schema(schema)
    validator = Draft202012Validator(schema)

    valid = {"name": "schema-verifier", "model": "openai/gpt-4o-mini", "maxTurns": 3}
    validator.validate(valid)
    invalid_errors = list(validator.iter_errors({"name": "schema-verifier", "unknown": True}))
    if not invalid_errors:
        raise AssertionError("schema must reject unknown top-level properties")

    with tempfile.TemporaryDirectory(prefix="agent-schema-") as temporary_directory:
        temporary = Path(temporary_directory)
        python_path = temporary / "agent_config.py"
        java_path = temporary / "AgentConfigModel.java"
        write_python(python_path, schema)
        write_java(java_path, schema)

        spec = importlib.util.spec_from_file_location("agent_config", python_path)
        if spec is None or spec.loader is None:
            raise AssertionError("could not load generated Python model")
        module = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = module
        spec.loader.exec_module(module)
        check_generated_fields(module, schema)
        subprocess.run(["javac", "--release", "21", "-d", str(temporary / "classes"), str(java_path)], check=True)

    print("agent schema: Draft 2020-12, generated field mapping, examples, and Java 21 compilation verified")


if __name__ == "__main__":
    main()
