# API-Calling Agent in Java Using Conductor: Plan, Authenticate, Call, Parse, Format

A user says "cancel my last order" and your AI understands the intent perfectly; but it has no idea which API to call, what parameters it needs, how to authenticate, or what to do when the API returns a 429. The gap between natural language intent and a successful `POST /orders/{id}/cancel` with a valid bearer token is five distinct steps, each with its own failure mode. This example uses [Conductor](https://github.com/conductor-oss/conductor) to bridge that gap as a durable pipeline: plan the API call, acquire credentials, execute the request, parse the response, and format a human-readable answer, with per-step retries, timeout handling, and a full audit trail of every API interaction.

## Users Speak Natural Language, APIs Speak JSON

A user says "Tell me about the Conductor open-source repository on GitHub" but the GitHub API needs a structured call: `GET /repos/conductor-oss/conductor` with a bearer token in the Authorization header. Bridging this gap requires five steps: understanding the user's intent and mapping it to an API endpoint with parameters, acquiring authentication credentials (API keys, OAuth tokens), making the HTTP call, parsing the nested JSON response, and formatting the result into a human-readable answer.

Each step has different failure modes, the LLM might plan the wrong API, the auth token might be expired, the API might rate-limit you, the response format might have changed. Without orchestration, these steps get tangled in a single method where an auth failure means re-running the planning step, a parse error has no record of what the API actually returned, and there's no audit trail of which APIs the agent called.

## The Solution

**You write the API planning, authentication, and formatting logic. Conductor handles the request pipeline, retries on rate limits, and full audit trails.**

`PlanApiCallWorker` analyzes the user request against an API catalog and determines the endpoint, HTTP method, and parameters. `AuthenticateWorker` acquires the necessary credentials (API key lookup, OAuth token refresh). `CallApiWorker` executes the planned HTTP request with the acquired credentials. `ParseResponseWorker` extracts the relevant data from the API response and validates it against the expected schema. `FormatOutputWorker` converts the parsed data into a natural language answer. Conductor chains these steps, retries failed API calls with backoff, and records the full request-response chain for debugging.

### What You Write: Workers

Five workers bridge natural language to API calls. Planning the endpoint, authenticating, executing the request, parsing the response, and formatting the answer.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **PlanApiCallWorker** | `ap_plan_api_call` | Plans an API call based on the user's request and an API catalog. Selects GitHub as the best API, determines the endpoint (`https://api.github.com/repos/conductor-oss/conductor`), method (GET), params (owner, repo), auth type (bearer_token), and expected response schema (8 fields). | Simulated. Swap in GPT-4 function calling for dynamic API selection |
| **AuthenticateWorker** | `ap_authenticate` | Authenticates against the selected API by producing a bearer token. Takes apiName and authType, returns a simulated token (`ghp_sim_...`), expiry (3600s), and token type (Bearer). | Simulated. Swap in AWS Secrets Manager or Auth0 for real credentials |
| **CallApiWorker** | `ap_call_api` | Calls the selected API endpoint. Takes endpoint, method, params, and authToken. Returns a simulated GitHub repo response (conductor-oss/conductor with 16500 stars, 2100 forks, Java, Apache-2.0 license), status code (200), and response time (185ms). | Simulated. Swap in OkHttp or Apache HttpClient for real HTTP calls |
| **ParseResponseWorker** | `ap_parse_response` | Parses and validates the raw API response against the expected schema. Extracts fields (name, description, stars, forks, language, license, openIssues, defaultBranch), counts fields extracted, and validates that status code is 200 with fields present. | Real. Deterministic field extraction and validation logic |
| **FormatOutputWorker** | `ap_format_output` | Formats the parsed API data into a human-readable answer. Constructs a natural language sentence: "The repository conductor-oss/conductor is .. It is written in Java and has 16500 stars and 2100 forks. It is licensed under Apache License 2.0." | Real. Deterministic template-based formatting |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If an API call fails (rate limit, timeout), Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes after authentication but before the API call, Conductor resumes from exactly where it left off with the token still available |
| **Observability** | Every step is tracked with inputs, outputs, timing, and status. Full audit trail of which API was called, what response was received, and how it was formatted |
| **Timeout management** | Per-task timeouts prevent hung API calls from blocking the pipeline |

### The Workflow

```
ap_plan_api_call
    |
    v
ap_authenticate
    |
    v
ap_call_api
    |
    v
ap_parse_response
    |
    v
ap_format_output
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/api-calling-agent-1.0.0.jar
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

## Example Output

```
=== API Calling Agent Demo ===

Step 1: Registering task definitions...
  Registered: ap_plan_api_call, ap_authenticate, ap_call_api, ap_parse_response, ap_format_output

Step 2: Registering workflow 'api_calling_agent'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  [ap_plan_api_call] Planning API call for request: Tell me about the Conductor open-source repository on GitHub
  [ap_authenticate] Authenticating for API: github (auth type: bearer_token)
  [ap_call_api] Calling GET https://api.github.com/repos/conductor-oss/conductor
  [ap_parse_response] Parsing response (status code: 200)
  [ap_format_output] Formatting output for request: Tell me about the Conductor open-source repository on GitHub

  Workflow ID: 1a2b3c4d-5e6f-7890-abcd-ef0123456789

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {answer=The repository conductor-oss/conductor is Conductor is an event driven orchestration platform. It is written in Java and has 16500 stars and 2100 forks. It is licensed under Apache License 2.0., dataSource=github_api, fieldsUsed=7, validationPassed=true}

Result: PASSED
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/api-calling-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Query a GitHub repository
conductor workflow start \
  --workflow api_calling_agent \
  --version 1 \
  --input '{"userRequest": "Tell me about the Conductor open-source repository on GitHub", "apiCatalog": [{"name": "github", "baseUrl": "https://api.github.com", "description": "GitHub REST API for repository and user data"}, {"name": "weather", "baseUrl": "https://api.openweathermap.org", "description": "OpenWeatherMap API for weather forecasts"}]}'

# Query weather data
conductor workflow start \
  --workflow api_calling_agent \
  --version 1 \
  --input '{"userRequest": "What is the weather in Tokyo right now?", "apiCatalog": [{"name": "weather", "baseUrl": "https://api.openweathermap.org", "description": "OpenWeatherMap API for weather forecasts"}]}'

# Query news headlines
conductor workflow start \
  --workflow api_calling_agent \
  --version 1 \
  --input '{"userRequest": "Show me the top tech news headlines", "apiCatalog": [{"name": "news", "baseUrl": "https://newsapi.org", "description": "News API for top headlines and article search"}]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w api_calling_agent -s COMPLETED -c 5
```

## How to Extend

Each worker owns one stage of the API-calling pipeline. Connect GPT-4 for intent-to-endpoint planning, real credential stores (Vault, AWS Secrets Manager) for auth, and OkHttp for execution, and the plan-authenticate-call-parse-format workflow runs unchanged.

- **PlanApiCallWorker** (`ap_plan_api_call`): use GPT-4 function calling to map natural language to API specifications from an OpenAPI/Swagger catalog, with schema-aware parameter extraction and endpoint selection
- **AuthenticateWorker** (`ap_authenticate`): integrate with real credential stores: AWS Secrets Manager for API keys, Auth0 for OAuth token management, HashiCorp Vault for dynamic credentials, or implement OAuth 2.0 refresh token flows
- **CallApiWorker** (`ap_call_api`): use OkHttp or Apache HttpClient to make real HTTP calls with proper timeout handling, retry headers (Retry-After), circuit breakers, and response streaming for large payloads
- **ParseResponseWorker** (`ap_parse_response`): add schema evolution handling for API version changes, JSON Path extraction for deeply nested responses, and error response parsing with actionable error messages
- **FormatOutputWorker** (`ap_format_output`): use an LLM to generate natural language answers that incorporate the parsed data conversationally, or support multiple output formats (text, markdown, JSON, voice-optimized)
- **Add error handling**: insert a `SWITCH` task after `CallApiWorker` to route non-200 responses to a retry-with-different-params path or an error-reporting path

Replace with real HTTP calls and LLM planning; the API pipeline preserves the same plan-authenticate-call-parse-format interface.

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
api-calling-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/apicalling/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ApiCallingAgentExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PlanApiCallWorker.java   # Maps user request to API endpoint and params
│       ├── AuthenticateWorker.java  # Acquires bearer token for the selected API
│       ├── CallApiWorker.java       # Executes HTTP request, returns response + status
│       ├── ParseResponseWorker.java # Extracts fields, validates against schema
│       └── FormatOutputWorker.java  # Converts parsed data to natural language answer
└── src/test/java/apicalling/workers/
    ├── PlanApiCallWorkerTest.java   # 9 tests
    ├── AuthenticateWorkerTest.java  # 8 tests
    ├── CallApiWorkerTest.java       # 9 tests
    ├── ParseResponseWorkerTest.java # 9 tests
    └── FormatOutputWorkerTest.java  # 9 tests
```
