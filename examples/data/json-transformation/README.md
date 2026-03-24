# JSON Transformation in Java Using Conductor : Field Mapping, Value Transforms, Nested Restructuring, and Schema Validation

## The Problem

Your upstream system sends customer records with snake_case field names (`cust_id`, `first_name`, `last_name`, `acct_type`, `reg_date`) in a flat structure, but your downstream API expects camelCase fields (`customerId`, `fullName`, `accountType`) organized into nested groups (`identity.id`, `contact.email`, `account.type`). The transformation is not just renaming. `first_name` and `last_name` need to be concatenated into `fullName` with whitespace trimmed, `email` needs to be lowercased for consistency, and `acct_type` needs to be uppercased to match the enum values the downstream system expects. After restructuring, the output must conform to a target schema: `identity.id` and `contact.email` are required fields, and any record missing them is invalid.

Without orchestration, you'd write a single mapper method that parses, renames, transforms, restructures, and validates in one pass. When a field rename breaks the schema validation, you'd have no visibility into whether the issue was in the mapping rules, the restructuring logic, or the source data. If you want to add a new transform (say, phone number formatting), you'd modify deeply coupled code. There's no record of what the intermediate mapped record looked like before restructuring, making it impossible to debug transform chain issues.

## The Solution

**You just write the JSON parsing, field mapping, nested restructuring, schema validation, and output workers. Conductor handles the multi-stage transformation sequence, retries on step failures, and field count tracking at every stage so you can trace how the record shape evolves from input to output.**

Each stage of the JSON transformation is a simple, independent worker. The parser reads the source JSON and counts its top-level fields. The field mapper applies the mapping rules. Renaming `cust_id` to `customerId`, concatenating `first_name` and `last_name` into `fullName`, lowercasing the email, and uppercasing the account type. The restructurer takes the flat mapped record and organizes it into nested groups (`identity`, `contact`, `account`). The schema validator checks that required fields like `identity.id` and `contact.email` exist in the restructured output. The emitter produces the final record for downstream consumption. Conductor executes them in sequence, passes the evolving record between stages, retries if a step fails, and tracks the field count at every stage so you can see exactly how the record shape changed from input to output.

### What You Write: Workers

Five workers handle JSON transformation: parsing the source JSON, mapping and renaming fields with value transforms, restructuring flat fields into nested groups, validating against the target schema, and emitting the final record.

| Worker | Task | What It Does |
|---|---|---|
| **EmitOutputWorker** | `jt_emit_output` | Emits the final transformed record. |
| **MapFieldsWorker** | `jt_map_fields` | Maps source fields to target fields with renaming and value transforms. |
| **ParseInputWorker** | `jt_parse_input` | Parses the incoming source JSON and counts its top-level fields. |
| **RestructureNestedWorker** | `jt_restructure_nested` | Restructures a flat mapped record into nested groups. |
| **ValidateSchemaWorker** | `jt_validate_schema` | Validate Schema. Computes and returns validated, is valid |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
jt_parse_input
 │
 ▼
jt_map_fields
 │
 ▼
jt_restructure_nested
 │
 ▼
jt_validate_schema
 │
 ▼
jt_emit_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
