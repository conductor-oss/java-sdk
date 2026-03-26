# Natural Language to SQL: Parse, Generate, Validate, Execute, Format

A user asks "What were last quarter's sales?" and the system must convert that to SQL, validate it for safety, execute it, and format the results. The validation step blocks dangerous operations to prevent data loss from LLM hallucinations.

## Workflow

```
question, dbSchema
       │
       ▼
┌──────────────────┐
│ sq_parse_nl      │  NLP: extract entities from question
└────────┬─────────┘
         ▼
┌──────────────────┐
│ sq_generate_sql  │  LLM: convert to SQL
└────────┬─────────┘
         ▼
┌──────────────────┐
│ sq_validate_sql  │  Block dangerous operations
└────────┬─────────┘
         ▼
┌──────────────────┐
│ sq_execute_sql   │  Run query, get rows
└────────┬─────────┘
         ▼
┌──────────────────┐
│ sq_format_results│  Format rows for display
└──────────────────┘
```

## Workers

**ParseNlWorker** (`sq_parse_nl`) -- Extracts entities from the question. When API key is set, uses LLM for entity extraction. Returns a `parsed` map with entities.

**GenerateSqlWorker** (`sq_generate_sql`) -- Converts the parsed entities and db schema into a SQL query. Uses LLM when API key is set.

**ValidateSqlWorker** (`sq_validate_sql`) -- Checks SQL against a `DANGEROUS_OPS` list using `upperSql.contains(op)` for each operation. Blocks destructive SQL (DROP, DELETE, TRUNCATE, ALTER, etc.) to prevent data loss.

**ExecuteSqlWorker** (`sq_execute_sql`) -- Executes the validated query. Returns result rows as `List.of(Map...)`.

**FormatResultsWorker** (`sq_format_results`) -- Formats rows for human-readable display. Handles empty result lists.

## Tests

33 tests cover NLP parsing, SQL generation, dangerous SQL detection, query execution, and result formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
