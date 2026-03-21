# SQL RAG in Java Using Conductor : Natural Language to SQL Query Execution

## Querying Databases with Natural Language

Not all knowledge lives in documents. Revenue numbers, user counts, and order histories live in SQL databases. Text-to-SQL RAG lets users ask "What were our top 5 products by revenue last quarter?" and get a precise answer by generating and executing a SQL query against the actual database.

The pipeline has five safety-critical steps: parse the question, generate SQL from the schema and intent, validate the SQL (no DROP TABLE, no unbounded SELECTs), execute the query, and format the results. Each step must succeed in order. you can't execute unvalidated SQL, and you can't format results from a failed query.

## The Solution

**You write the SQL generation, validation, and execution logic. Conductor handles the safety-critical pipeline, retries, and observability.**

Each stage is an independent worker. NL parsing, SQL generation (given the database schema), SQL validation (safety checks), query execution, and result formatting. Conductor sequences them, ensuring SQL is validated before execution. Every query is tracked with the original question, generated SQL, validation result, and query output. creating an audit trail of all natural language queries against your database.

### What You Write: Workers

Five workers implement natural language to SQL. parsing the question for intent and entities, generating a SQL query, validating it for safety, executing against the database, and formatting the results into a readable answer.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteSqlWorker** | `sq_execute_sql` | Worker that executes a SQL query and returns demo result rows. Returns 5 rows with workflow_name, execution_coun... |
| **FormatResultsWorker** | `sq_format_results` | Worker that formats SQL query results into a natural-language answer. Takes the original question, SQL, rows, and row... |
| **GenerateSqlWorker** | `sq_generate_sql` | Worker that generates a SQL query from the parsed intent, entities, and schema. |
| **ParseNlWorker** | `sq_parse_nl` | Worker that parses a natural-language question against a database schema. Extracts intent and structured entities. |
| **ValidateSqlWorker** | `sq_validate_sql` | Worker that validates a SQL query against a schema, checking for dangerous operations (DROP, DELETE, TRUNCATE, ALTER,... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
sq_parse_nl
 │
 ▼
sq_generate_sql
 │
 ▼
sq_validate_sql
 │
 ▼
sq_execute_sql
 │
 ▼
sq_format_results

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
