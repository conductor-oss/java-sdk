# Google Gemini in Java Using Conductor: Structured Gemini API Calls with Content Preparation and Output Formatting

Your team picks Gemini for multimodal tasks, but the API latency varies wildly: sometimes 2 seconds, sometimes 30, and your users stare at a spinner while you have no idea if it's a quota issue, a cold start, or a malformed request. A rate-limit 429 loses the parts-based content you just assembled, and there's zero visibility into token consumption across calls. This example builds a structured Gemini pipeline using [Conductor](https://github.com/conductor-oss/conductor), content preparation, API invocation, and response formatting, so every call is retryable, observable, and decoupled from the request-building logic.

## Calling Gemini in Production

Google Gemini's API has a specific request format. Content must be structured as parts, generation config specifies temperature/topK/topP/maxOutputTokens, and safety settings control content filtering. The response comes back as candidates with safety ratings and usage metadata that need to be extracted and formatted for downstream consumers.

When you embed all of this in a single method: building the request body, making the HTTP call, parsing the nested candidate structure, you end up with tightly coupled code where changing the prompt format means touching API call logic, a rate-limit error loses the prepared request, and there's no record of token consumption across calls. Retrying a failed API call means re-building the entire request from scratch.

## The Solution

**You write the Gemini content preparation and output formatting logic. Conductor handles the API invocation pipeline, durability, and observability.**

Each concern is an independent worker. Content preparation (building Gemini's parts-based request with generation config), API invocation (the actual generateContent call), and output formatting (extracting text from the candidate structure with usage metadata). Conductor chains them so the prepared request body feeds into the API call, and the raw candidate response feeds into the formatter. In this example the task defs are configured to fail fast on provider errors, which makes quota and model-access problems obvious during local validation. Every call records the prompt, model, token usage, and formatted output.

### What You Write: Workers

Three workers handle the Gemini integration. Preparing parts-based content with safety settings, calling the Gemini API, and formatting the candidate response with token counts and finish reason.

| Worker | Task | What It Does |
|---|---|---|
| **GeminiFormatOutputWorker** | `gemini_format_output` | Extracts the generated text from Gemini's candidate structure and returns it as formattedResult. |
| **GeminiGenerateWorker** | `gemini_generate` | Calls the Google Gemini generateContent REST API using the configured model (`GEMINI_MODEL`, default `gemini-2.5-flash`). Falls back to a simulated response when GOOGLE_API_KEY is not set. |
| **GeminiPrepareContentWorker** | `gemini_prepare_content` | Prepares a Gemini-style request body with parts-based content structure, generation config, and safety settings. |

When `GOOGLE_API_KEY` is set and has active billing/quota for the Gemini API, `GeminiGenerateWorker` makes a real HTTP call to the Gemini REST API using `java.net.http.HttpClient` (built into Java 21). The model comes from `GEMINI_MODEL` by default and can also be overridden via workflow input. If your API key lacks quota (HTTP 429), the worker fails fast with a clear terminal message and includes the `Retry-After` value when Google returns one. When the key is absent, it falls back to a simulated response (prefixed with ``) so the workflow runs end-to-end without credentials.

### The Workflow

```
gemini_prepare_content
    │
    ▼
gemini_generate
    │
    ▼
gemini_format_output

```

## Example Output

```
=== Example 114: Orchestrating Google Gemini ===

Mode: LIVE (GOOGLE_API_KEY detected)
Model: gemini-2.5-flash

Step 1: Registering task definitions...
  Registered: gemini_prepare_content, gemini_generate, gemini_format_output

Step 2: Registering workflow 'google_gemini_workflow'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: 76e17bac-69d2-98e5-5b7f-b2d73dbec88e

  [prep] Gemini request: model=gemini-2.5-flash, topK=40
  [gen] Calling Google Gemini API (LIVE, model=gemini-2.5-flash)...
  [fmt] Formatted result: Product launch plan for Q4...

  Status: COMPLETED
  Model: gemini-2.5-flash
  Tokens: {promptTokenCount=78, candidatesTokenCount=132, totalTokenCount=210}

Result: PASSED

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
# With real Gemini API:
GOOGLE_API_KEY=your-key GEMINI_MODEL=gemini-2.5-flash docker compose up --build

# Without API key (simulated mode):
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 GOOGLE_API_KEY=your-key GEMINI_MODEL=gemini-2.5-flash docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run (with real Gemini API)
export GOOGLE_API_KEY=your-key
export GEMINI_MODEL=gemini-2.5-flash
mvn package -DskipTests
java -jar target/google-gemini-1.0.0.jar

```

### Option 3: Use the run script

```bash
# With real Gemini API:
GOOGLE_API_KEY=your-key GEMINI_MODEL=gemini-2.5-flash ./run.sh

# Without API key (simulated mode):
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

`run.sh` auto-loads the nearest `.env` file it finds while walking up parent directories, so a repo-root `.env` works without manual exports.

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `GOOGLE_API_KEY` | _(none)_ | Google Gemini API key with active billing and quota. When set, workers call the real Gemini API. When absent, workers use simulated responses. If the key lacks quota, you'll see a clear error with a link to the Cloud console. |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Default Gemini model used by `GeminiGenerateWorker`. You can also override it per workflow input with `model`. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/google-gemini-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow google_gemini_workflow \
  --version 1 \
  --input '{"prompt": "Create a product launch plan for our new analytics dashboard.", "context": "B2B SaaS company, 10K existing users, targeting enterprise segment.", "model": "gemini-2.5-flash"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w google_gemini_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one phase of the Gemini integration, the preparation-invocation-formatting pipeline runs unchanged as you customize individual workers.

- **GeminiGenerateWorker** (`gemini_generate`): already calls the real Gemini API via `java.net.http.HttpClient`. Change the model with `GEMINI_MODEL` or the workflow input `model`, or switch to a different endpoint.
- **GeminiPrepareContentWorker** (`gemini_prepare_content`): customize prompt templates, add multi-turn conversation support, or include image parts for multimodal requests
- **GeminiFormatOutputWorker** (`gemini_format_output`): add structured output parsing, safety rating filtering, or multi-candidate selection logic

The content-in, formatted-response-out contract is fixed. Switch Gemini models, adjust safety settings, or add multi-modal parts without modifying the workflow.

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
google-gemini/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/googlegemini/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GoogleGeminiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GeminiFormatOutputWorker.java
│       ├── GeminiGenerateWorker.java
│       └── GeminiPrepareContentWorker.java
└── src/test/java/googlegemini/workers/
    ├── GeminiFormatOutputWorkerTest.java        # 4 tests
    ├── GeminiGenerateWorkerTest.java        # 6 tests
    └── GeminiPrepareContentWorkerTest.java        # 3 tests

```
