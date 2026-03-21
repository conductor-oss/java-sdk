# Database Integration in Java Using Conductor

A Java Conductor workflow that performs a database-to-database ETL migration. connecting to source and target databases, querying rows from the source, transforming the data (normalizing fields, adding timestamps), writing transformed rows to the target, and verifying the row counts match. Given source/target database configs, a query, and transform rules, the pipeline produces a verified migration with row counts. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the connect-query-transform-write-verify pipeline.

## Migrating Data Between Databases Reliably

Database migration involves a strict sequence: connect to both databases, query the source, transform the data to match the target schema, write the transformed rows, and verify that source and target counts match. Each step depends on the previous one. you cannot transform without data, and you cannot verify without knowing how many rows were written. If any step fails mid-migration, you need to know exactly where it stopped and be able to resume.

Without orchestration, you would manage connection lifecycles, chain SQL operations manually, and build custom verification logic. Conductor sequences the five steps and tracks every row count and connection ID between them.

## The Solution

**You just write the ETL workers. Database connection, source query, data transformation, target write, and row count verification. Conductor handles connection lifecycle management, query retries on timeouts, and row-count tracking for migration verification.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Five workers run the ETL migration: ConnectWorker establishes database connections, QueryWorker extracts source rows, TransformWorker normalizes and maps fields, WriteWorker inserts into the target, and VerifyWorker confirms row counts match.

| Worker | Task | What It Does |
|---|---|---|
| **ConnectWorker** | `dbi_connect` | Establishes connections to source and target databases. returns sourceConnectionId and targetConnectionId for use by subsequent query and write steps |
| **QueryWorker** | `dbi_query` | Queries data from the source database. executes the SQL query using the source connection and returns the result rows and rowCount |
| **TransformWorker** | `dbi_transform` | Transforms the queried rows. applies the transform rules (field normalization, timestamp addition, schema mapping) and returns the transformedRows and transformedCount |
| **WriteWorker** | `dbi_write` | Writes transformed rows to the target database. inserts the rows using the target connection and returns the writtenCount |
| **VerifyWorker** | `dbi_verify` | Verifies the migration. compares the source rowCount against the writtenCount and returns verified=true if they match |

Workers implement external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients. the workflow orchestration and error handling stay the same.

### The Workflow

```
dbi_connect
    │
    ▼
dbi_query
    │
    ▼
dbi_transform
    │
    ▼
dbi_write
    │
    ▼
dbi_verify

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
java -jar target/database-integration-1.0.0.jar

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
| `DB_URL` | _(none)_ | JDBC database URL (e.g., `jdbc:postgresql://localhost:5432/mydb`). Currently unused, all workers run in demo mode with `[DEMO]` output prefix. Swap in JDBC or HikariCP for production. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/database-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow database_integration_444 \
  --version 1 \
  --input '{"sourceDb": "api", "targetDb": "production", "query": "What is workflow orchestration?", "transformRules": "sample-transformRules"}'active'": "pending", "transformRules": "sample-transformRules", "uppercase_name": "sample-name"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w database_integration_444 -s COMPLETED -c 5

```

## How to Extend

Swap in JDBC or HikariCP for connection management, PreparedStatement execution for queries, and batch inserts for the write step against your actual source and target databases. The workflow definition stays exactly the same.

- **ConnectWorker** (`dbi_connect`): use JDBC or a connection pool (HikariCP) to establish real database connections
- **QueryWorker** (`dbi_query`): execute real SQL queries against the source database via JDBC PreparedStatement
- **WriteWorker** (`dbi_write`): use JDBC batch inserts to write transformed rows to the target database

Replace each simulation with real JDBC operations while preserving output fields, and the migration pipeline runs without modification.

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
database-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/databaseintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DatabaseIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ConnectWorker.java
│       ├── QueryWorker.java
│       ├── TransformWorker.java
│       ├── VerifyWorker.java
│       └── WriteWorker.java
└── src/test/java/databaseintegration/workers/
    ├── ConnectWorkerTest.java        # 2 tests
    ├── QueryWorkerTest.java        # 2 tests
    ├── TransformWorkerTest.java        # 2 tests
    ├── VerifyWorkerTest.java        # 2 tests
    └── WriteWorkerTest.java        # 2 tests

```
