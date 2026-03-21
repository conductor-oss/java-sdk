# SQL RAG in Java Using Conductor :  Natural Language to SQL Query Execution

A Java Conductor workflow that turns natural language questions into SQL. parsing the question to extract intent and entities, generating a SQL query from the question and database schema, validating the SQL for safety and correctness, executing it against the database, and formatting the results into a human-readable answer. This is text-to-SQL RAG where the "retrieval" is a SQL query instead of a vector search. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate NL parsing, SQL generation, validation, execution, and formatting as independent workers,  you write the SQL generation and validation logic, Conductor handles sequencing, retries, durability, and observability.

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/rag-sql-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, GenerateSqlWorker and ParseNlWorker call gpt-4o-mini instead of using demo output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-sql-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sql_rag_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "dbSchema": "sample-dbSchema"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sql_rag_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one text-to-SQL step. swap in an LLM for SQL generation from natural language, connect your database schema for validation, execute against a real database, and the parse-generate-validate-execute-format pipeline runs unchanged.

- **ParseNlWorker** (`sq_parse_nl`): use an LLM (GPT-4, Claude) or a text-to-SQL model to extract intent, entities, and query structure from natural-language questions
- **GenerateSqlWorker** (`sq_generate_sql`): use an LLM with schema-aware prompting (table definitions, column types, foreign keys) to generate valid SQL from the parsed intent
- **ValidateSqlWorker** (`sq_validate_sql`): validate SQL against the schema, check for dangerous operations (DROP, DELETE), and optionally run EXPLAIN to catch performance issues before execution
- **ExecuteSqlWorker** (`sq_execute_sql`): execute the validated SQL against a real database (PostgreSQL, MySQL) via JDBC with parameterized queries and result size limits
- **FormatResultsWorker** (`sq_format_results`): use an LLM to convert SQL result rows into a natural-language answer, or format as a table/chart for the frontend

Each worker's contract is fixed. swap the SQL generator model, add new validation rules, or change the target database without altering the pipeline structure.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
rag-sql/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragsql/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagSqlExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteSqlWorker.java
│       ├── FormatResultsWorker.java
│       ├── GenerateSqlWorker.java
│       ├── ParseNlWorker.java
│       └── ValidateSqlWorker.java
└── src/test/java/ragsql/workers/
    ├── ExecuteSqlWorkerTest.java        # 5 tests
    ├── FormatResultsWorkerTest.java        # 6 tests
    ├── GenerateSqlWorkerTest.java        # 6 tests
    ├── ParseNlWorkerTest.java        # 5 tests
    └── ValidateSqlWorkerTest.java        # 11 tests

```
