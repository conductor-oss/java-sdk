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

| Worker | Task | What It Does |
|---|---|---|
| **PlanApiCallWorker** | `ap_plan_api_call` | Plans an API call based on the user's request and an API catalog. Selects GitHub as the best API, determines the endpoint (`https://api.github.com/repos/conductor-oss/conductor`), method (GET), params (owner, repo), auth type (bearer_token), and expected response schema (8 fields). |
| **AuthenticateWorker** | `ap_authenticate` | Authenticates against the selected API by producing a bearer token. Takes apiName and authType, returns a HMAC-signed JWT ), expiry (3600s), and token type (Bearer). |
| **CallApiWorker** | `ap_call_api` | Calls the selected API endpoint. Takes endpoint, method, params, and authToken. Returns a demo GitHub repo response (conductor-oss/conductor with 16500 stars, 2100 forks, Java, Apache-2.0 license), status code (200), and response time (185ms). |
| **ParseResponseWorker** | `ap_parse_response` | Parses and validates the raw API response against the expected schema. Extracts fields (name, description, stars, forks, language, license, openIssues, defaultBranch), counts fields extracted, and validates that status code is 200 with fields present. |
| **FormatOutputWorker** | `ap_format_output` | Formats the parsed API data into a human-readable answer. Constructs a natural language sentence: "The repository conductor-oss/conductor is .. It is written in Java and has 16500 stars and 2100 forks. It is licensed under Apache License 2.0." |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
