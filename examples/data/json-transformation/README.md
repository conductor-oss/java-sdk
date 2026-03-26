# Json Transformation

An integration layer receives JSON payloads from a partner API in a deeply nested schema that doesn't match the internal data model. Fields need renaming, nested objects need flattening, arrays need filtering, and the output must conform to the internal schema before downstream services can consume it.

## Pipeline

```
[jt_parse_input]
     |
     v
[jt_map_fields]
     |
     v
[jt_restructure_nested]
     |
     v
[jt_validate_schema]
     |
     v
[jt_emit_output]
```

**Workflow inputs:** `sourceJson`, `mappingRules`

## Workers

**EmitOutputWorker** (task: `jt_emit_output`)

Emits the final transformed record.

- Reads `finalRecord`, `isValid`. Writes `record`, `emitted`

**MapFieldsWorker** (task: `jt_map_fields`)

Maps source fields to target fields with renaming and value transforms.

- Lowercases strings, uppercases strings, trims whitespace
- Reads `data`. Writes `mapped`, `mappedFieldCount`

**ParseInputWorker** (task: `jt_parse_input`)

Parses the incoming source JSON and counts its top-level fields.

- Reads `sourceJson`. Writes `parsed`, `fieldCount`

**RestructureNestedWorker** (task: `jt_restructure_nested`)

Restructures a flat mapped record into nested groups.

- Reads `mapped`. Writes `restructured`

**ValidateSchemaWorker** (task: `jt_validate_schema`)

Validates that the restructured record conforms to the expected schema. Checks that identity.id and contact.email exist and are non-empty.

- Reads `record`. Writes `validated`, `isValid`

---

**43 tests** | Workflow: `json_transformation` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
