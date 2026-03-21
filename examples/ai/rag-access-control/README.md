# RAG Access Control in Java Using Conductor :  Authentication, Permission Filtering, and Data Redaction

A Java Conductor workflow that wraps a RAG pipeline with enterprise access controls .  authenticating the user, checking document-level permissions, filtering retrieval results to only include documents the user is authorized to see, redacting sensitive fields (PII, financial data, classified content) from the context, and generating an answer from the sanitized, permission-filtered documents. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-stage access-controlled pipeline as independent workers ,  you write the auth, permission, redaction, and generation logic, Conductor handles sequencing, retries, durability, and observability.

## RAG Without Access Control Is a Data Leak

A standard RAG pipeline retrieves documents and generates answers without checking who's asking. In an enterprise, this means an intern can ask questions about board-level financial documents, a contractor can access employee HR records, and sensitive customer PII shows up in generated responses. The vector store doesn't know about your org chart or data classification policies.

Access-controlled RAG adds four gates before generation: authenticate the user (validate their token/session), check their permissions (which document categories can they access?), filter retrieval results (remove any documents they shouldn't see), and redact sensitive fields from the remaining documents. Only then does the sanitized context reach the LLM.

Each gate must execute in order .  you can't filter by permissions before you know who the user is, and you can't redact before filtering. If any gate fails (invalid token, permission service unavailable), the pipeline must stop cleanly without leaking data.

## The Solution

**You write the authentication, permission filtering, and data redaction logic. Conductor handles the access-controlled pipeline, retries, and observability.**

Each access control gate is an independent worker .  authentication, permission checking, filtered retrieval, sensitive data redaction, and generation. Conductor sequences them so each gate's output feeds into the next. If the permission service times out, Conductor retries it without re-authenticating. Every query is tracked with the user identity, permissions applied, documents filtered, fields redacted, and final answer ,  creating a complete audit trail for compliance.

### What You Write: Workers

Five workers enforce access control throughout the RAG pipeline .  authenticating the user, checking document-level permissions, retrieving only authorized content, redacting sensitive fields, and generating an answer from the sanitized context.

| Worker | Task | What It Does |
|---|---|---|
| **AuthenticateUserWorker** | `ac_authenticate_user` | Worker that authenticates a user by validating their auth token. Returns authenticated status, roles, and clearance l... |
| **CheckPermissionsWorker** | `ac_check_permissions` | Worker that checks user permissions based on roles and clearance level. Determines which document collections the use... |
| **FilteredRetrieveWorker** | `ac_filtered_retrieve` | Worker that retrieves documents filtered by the user's allowed collections. Contains a corpus of 5 documents across d... |
| **GenerateWorker** | `ac_generate` | Worker that generates an answer from the access-controlled and redacted context. Notes when data has been redacted du... |
| **RedactSensitiveWorker** | `ac_redact_sensitive` | Worker that redacts sensitive information from documents based on clearance level. Redacts SSN patterns and salary fi... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
ac_authenticate_user
    │
    ▼
ac_check_permissions
    │
    ▼
ac_filtered_retrieve
    │
    ▼
ac_redact_sensitive
    │
    ▼
ac_generate

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
java -jar target/rag-access-control-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for embeddings and generation. When absent, workers use simulated responses. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-access-control-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_access_control \
  --version 1 \
  --input '{"input": "test"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_access_control -s COMPLETED -c 5

```

## How to Extend

Each worker enforces one security gate .  swap in your identity provider for authentication, your IAM system for permission checks, real PII detection for redaction, and the five-stage access-controlled pipeline runs unchanged.

- **AuthenticateUserWorker** (`ac_authenticate_user`): validate JWT tokens against your identity provider (Auth0, Okta, AWS Cognito) and extract roles and clearance level from claims
- **CheckPermissionsWorker** (`ac_check_permissions`): query a policy engine (Open Policy Agent, AWS Cedar, Casbin) to determine which document collections the user may access
- **FilteredRetrieveWorker** (`ac_filtered_retrieve`): query Pinecone, Weaviate, or Elasticsearch with metadata filters that enforce the allowed-collections list, preventing unauthorized document retrieval at the vector DB level
- **GenerateWorker** (`ac_generate`): send the access-controlled, redacted context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate an answer that respects the user's clearance level
- **RedactSensitiveWorker** (`ac_redact_sensitive`): apply PII redaction (SSN patterns, salary figures) using regex or a dedicated PII detection library (Presidio, AWS Comprehend) based on user clearance

Each worker preserves the security contract at its boundary .  swap the auth provider, change the permission model, or upgrade the redaction strategy without altering the pipeline routing.

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
rag-access-control/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragaccesscontrol/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagAccessControlExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AuthenticateUserWorker.java
│       ├── CheckPermissionsWorker.java
│       ├── FilteredRetrieveWorker.java
│       ├── GenerateWorker.java
│       └── RedactSensitiveWorker.java
└── src/test/java/ragaccesscontrol/workers/
    ├── AuthenticateUserWorkerTest.java        # 3 tests
    ├── CheckPermissionsWorkerTest.java        # 3 tests
    ├── FilteredRetrieveWorkerTest.java        # 3 tests
    ├── GenerateWorkerTest.java        # 3 tests
    └── RedactSensitiveWorkerTest.java        # 3 tests

```
