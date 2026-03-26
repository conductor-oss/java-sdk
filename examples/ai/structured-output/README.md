# Generate JSON, Validate Schema, Transform

The LLM returns prose when you need JSON. Or JSON with a missing field. Or `"employees": "five hundred"` instead of a number. This pipeline generates structured JSON output, validates it against a schema with required fields and type checks, and transforms the validated data.

## Workflow

```
entity, fields
     │
     ▼
┌──────────────────────┐
│ so_generate_json     │  LLM generates structured JSON
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ so_validate_schema   │  Check required fields + types
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ so_transform         │  Transform validated data
└──────────────────────┘
```

## Workers

**GenerateJsonWorker** (`so_generate_json`) -- Produces JSON output with a schema definition: `required: ["name", "industry", "founded", "employees"]` with types map specifying expected types for each field.

**ValidateSchemaWorker** (`so_validate_schema`) -- Checks `data.containsKey(field)` for each required field. Validates type constraints. Returns validation results with pass/fail and error details.

**TransformWorker** (`so_transform`) -- Transforms the validated JSON data into the desired output format.

## Tests

11 tests cover JSON generation, schema validation with missing/wrong-type fields, and transformation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
