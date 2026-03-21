# Database Agent in Java Using Conductor: Natural Language to SQL with Validation and Execution

The support engineer needs to know which departments have the highest revenue. The data is in the database. They don't know SQL. So they Slack the developer, who is in a meeting for the next two hours. The developer eventually runs the query, pastes the results in a thread, and the support engineer squints at raw column output trying to figure out what "dept_id 7" means. Multiply this by every team that needs data but doesn't have database access. This example builds a natural-language-to-SQL pipeline with Conductor: parse the question, generate the query, validate it for safety (no DROP, no unbounded scans), execute it, and format the results into a human-readable answer. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Users Shouldn't Need SQL to Query Their Data

A product manager asks "Which customers churned last quarter?" and expects an answer, not a SQL tutorial. A database agent translates this into `SELECT customer_id, churn_date FROM customers WHERE churn_date BETWEEN '2025-07-01' AND '2025-09-30'`; but there are critical safety steps between the question and the result.

The generated SQL must be validated: no destructive operations (DROP, DELETE, TRUNCATE), no full table scans on billion-row tables, proper JOIN conditions to avoid cartesian products, and parameterized queries to prevent injection. Only after validation should the query execute. And the raw query results (rows and columns) need to be formatted into a human-readable answer ("47 customers churned last quarter, with the highest concentration in August").

## The Solution

**You write the NL parsing, SQL generation, validation, and execution logic. Conductor handles the query pipeline, safety gating, and full audit of every generated query.**

`ParseQuestionWorker` extracts the intent (aggregation, lookup, comparison), entities (customers, dates, metrics), and constraints from the natural language question. `GenerateQueryWorker` produces a SQL query using the database schema and parsed intent. `ValidateQueryWorker` checks the query for safety (no destructive DDL, no unbounded scans, proper WHERE clauses), correctness (valid table/column references, proper JOINs), and performance (estimated cost). `ExecuteQueryWorker` runs the validated query and returns the result set. `FormatWorker` converts the raw results into a natural language answer with key insights. Conductor chains these five steps and records the generated SQL alongside the results for audit.

### What You Write: Workers

Five workers form the text-to-SQL pipeline. Parsing the question, generating SQL, validating for safety, executing the query, and formatting results into natural language.

| Worker | Task | What It Does |
|---|---|---|
| **ExecuteQueryWorker** | `db_execute_query` | Executes a validated SQL query against the database. Returns 5 department rows with department name, emplo |
| **FormatWorker** | `db_format` | Formats the raw query results into a human-readable answer and summary. Produces a natural-language answer string alo |
| **GenerateQueryWorker** | `db_generate_query` | Generates a SQL query from the parsed intent and entities. Produces a multi-line SELECT statement with JOIN, GROUP BY |
| **ParseQuestionWorker** | `db_parse_question` | Parses a natural-language question against a database schema. Extracts the user's intent, relevant entities (metric, |
| **ValidateQueryWorker** | `db_validate_query` | Validates a generated SQL query for safety and correctness. Runs five validation checks (syntax, tables exist, no mut |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
db_parse_question
 │
 ▼
db_generate_query
 │
 ▼
db_validate_query
 │
 ▼
db_execute_query
 │
 ▼
db_format

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
