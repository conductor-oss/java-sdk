# Database Agent in Java Using Conductor: Natural Language to SQL with Validation and Execution

The support engineer needs to know which departments have the highest revenue. The data is in the database. They don't know SQL. So they Slack the developer, who is in a meeting for the next two hours. The developer eventually runs the query, pastes the results in a thread, and the support engineer squints at raw column output trying to figure out what "dept_id 7" means. Multiply this by every team that needs data but doesn't have database access. This example builds a natural-language-to-SQL pipeline with Conductor: parse the question, generate the query, validate it for safety (no DROP, no unbounded scans), execute it, and format the results into a human-readable answer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability.

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
| **ParseQuestionWorker** | `db_parse_question` | Parses a natural-language question against a database schema. Extracts the user's intent, relevant entities (metric,  |
| **ValidateQueryWorker** | `db_validate_query` | Validates a generated SQL query for safety and correctness. Runs five validation checks (syntax, tables exist, no mut |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

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
java -jar target/database-agent-1.0.0.jar

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

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/database-agent-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow database_agent \
  --version 1 \
  --input '{"question": "What are the top 5 departments by total revenue, including employee count and average sale?", "databaseSchema": {"database": "company_analytics", "tables": [{"name": "employees", "columns": ["id", "name", "department_id", "hire_date", "salary"]}, {"name": "departments", "columns": ["id", "name", "location", "budget"]}, {"name": "sales", "columns": ["id", "employee_id", "amount", "sale_date", "product_id"]}, {"name": "products", "columns": ["id", "name", "category", "price"]}]}}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w database_agent -s COMPLETED -c 5

```

## How to Extend

Each worker owns one stage of the text-to-SQL pipeline. Plug in an LLM (GPT-4, SQLCoder) for query generation, a real SQL parser (JSqlParser) for validation, and JDBC for execution against live databases, and the parse-generate-validate-execute-format workflow runs unchanged.

- **GenerateQueryWorker** (`db_generate_query`): use GPT-4 with the database schema as context and few-shot examples of question-to-SQL mappings, or use specialized text-to-SQL models like SQLCoder
- **ValidateQueryWorker** (`db_validate_query`): integrate with a SQL parser (JSqlParser) for AST-level validation, EXPLAIN plan analysis for performance checks, and a blocklist of dangerous patterns
- **ExecuteQueryWorker** (`db_execute_query`): connect to real databases via JDBC with read-only credentials, query timeout limits, and result set size caps to prevent runaway queries

Wire in a real database and LLM for SQL generation; the query pipeline maintains the same parse-generate-validate-execute contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
database-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/databaseagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DatabaseAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExecuteQueryWorker.java
│       ├── FormatWorker.java
│       ├── GenerateQueryWorker.java
│       ├── ParseQuestionWorker.java
│       └── ValidateQueryWorker.java
└── src/test/java/databaseagent/workers/
    ├── ExecuteQueryWorkerTest.java        # 9 tests
    ├── FormatWorkerTest.java        # 9 tests
    ├── GenerateQueryWorkerTest.java        # 9 tests
    ├── ParseQuestionWorkerTest.java        # 9 tests
    └── ValidateQueryWorkerTest.java        # 9 tests

```
