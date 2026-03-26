# Database Agent: Natural Language to SQL with Validation

A user asks a question about employees or sales. The agent parses it to extract entities and identify relevant tables (`["employees", "sales", "departments"]`), generates SQL, validates it (4 checks including syntax and safety), executes it using a `ConcurrentHashMap`-backed store with regex parsing, and formats the result.

## Workflow

```
question, databaseSchema -> db_parse_question -> db_generate_query -> db_validate_query -> db_execute_query -> db_format
```

## Workers

**ParseQuestionWorker** (`db_parse_question`) -- Extracts entities and `relevantTables: ["employees", "sales", "departments"]`.

**GenerateQueryWorker** (`db_generate_query`) -- Produces SQL using `tablesUsed: ["departments", "employees", "sales"]`.

**ValidateQueryWorker** (`db_validate_query`) -- Runs 4 validation checks: `syntax_valid`, and more.

**ExecuteQueryWorker** (`db_execute_query`) -- Uses `ConcurrentHashMap` and `Pattern`/`Matcher` for execution.

**FormatWorker** (`db_format`) -- Produces human-readable `answer` from the summary.

## Tests

53 tests extensively cover parsing, generation, validation, execution, and formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
