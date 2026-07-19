# AgentClient control-plane reference

`AgentClient` (`io.orkes.conductor.client.AgentClient`, in the `conductor-client` module) is the Java SDK's interface to the agent control-plane (`/api/agent/*`). Standard Conductor endpoints (`/api/workflow/*`, `/api/tasks`, etc.) remain on the SDK's typed clients (`WorkflowClient`, `TaskClient`, `MetadataClient`). Obtain an agent client with `new OrkesClients(conductorClient).getAgentClient()` or construct `OrkesAgentClient` directly.

Every request goes through the shared `ConductorClient`'s native HTTP + auth + serialization layer. No hand-rolled HTTP. `ConductorClientException` is mapped to the typed `AgentAPIException` / `AgentNotFoundException` (`io.orkes.conductor.client.exceptions`).

## Methods

| Method | HTTP | Java input type | Java return type | Description |
|---|---|---|---|---|
| [`compileAgent`](#compileagent) | `POST /api/agent/compile` | `AgentRequest` | `CompileResponse` | Compile to Conductor workflow def — no side effects |
| [`deployAgent`](#deployagent) | `POST /api/agent/deploy` | `AgentRequest` | `StartResponse` | Register workflow def without starting |
| [`startAgent`](#startagent) | `POST /api/agent/start` | `AgentRequest` | `StartResponse` | Compile + register + start execution |
| [`getAgentStatus`](#getagentstatus) | `GET /api/agent/{id}/status` | path: `executionId` | `AgentStatusResponse` | Poll execution status; includes HITL pending-tool |
| `getExecution` | `GET /api/agent/execution/{id}` | path: `executionId` | `Map<String,Object>` | Fetch the full execution tree |
| `listExecutions` | `GET /api/agent/executions` | query parameter map | `Map<String,Object>` | Search agent executions |
| [`respond`](#respond) | `POST /api/agent/{id}/respond` | `RespondBody` | `void` | Resume a paused HITL task |
| [`cancelAgent`](#cancelagent) | `DELETE /api/agent/{id}/cancel` | path: `executionId`; optional `reason` query | `void` | Immediately cancel/terminate an execution |
| [`stopAgent`](#stopagent) | `POST /api/agent/{id}/stop` | path: `executionId` | `void` | Gracefully stop after the current iteration |
| `signalAgent` | `POST /api/agent/{id}/signal` | path: `executionId`; message body | `void` | Inject persistent context |
| `streamSse` | `GET /api/agent/stream/{id}` | path: `executionId`; optional last event ID | `SseClient` | Open the resumable SSE event stream |

---

## AgentRequest

Input to `compileAgent`, `deployAgent`, and `startAgent` (`io.orkes.conductor.client.model.agent.AgentRequest`). A pure transport DTO: the agent definition arrives pre-serialized as a JSON-ready map — domain serialization is owned by `conductor-client-ai` (`AgentRuntime.agentRequest(agent)` calls `AgentConfigSerializer.serialize(agent)` and resolves the `Framework` discriminator before building the request).

```java
// Already-deployed agent — version is optional
AgentRequest.deployedAgent("researcher", 3)
    .prompt("Summarize the release risks")
    .build()

// Native agent — AgentRuntime passes the serialized agent map
AgentRequest.nativeAgent(serializedAgent).build()

// Framework-backed agent — framework wire name + serialized raw config
AgentRequest.frameworkAgent("openai", serializedAgent).build()
AgentRequest.frameworkAgent("langchain", serializedAgent).build()

// Framework skill reference instead of rawConfig
AgentRequest.frameworkAgent("skill", null)
    .model("anthropic/claude-sonnet-4-6")
    .skillRef(Map.of("name", "code-review", "version", 2))
    .build()

// With execution fields (for /start only)
AgentRequest.nativeAgent(serializedAgent)
    .prompt("What is the capital of France?")
    .sessionId("session-abc")
    .runId("a1b2c3...")           // per-execution domain UUID for stateful agents
    .staticPlan(plan.toJson())    // pre-serialized map, written as "static_plan"
    .idempotencyKey("question-123")
    .build()
```

**Wire output (mutually exclusive shapes, fixed at build time):**

| Factory | JSON emitted |
|---|---|
| `deployedAgent(name, version)` | `"name": name, "version": version` (version omitted when null) |
| `nativeAgent(config)` | `"agentConfig": config` |
| `frameworkAgent(framework, config)` | `"framework": framework, "rawConfig": config` |
| `frameworkAgent(framework, null).skillRef(ref)` | `"framework": framework, "skillRef": ref` |

**Field mapping to server `StartRequest`:**

| `AgentRequest` field | Java type | JSON key | Server `StartRequest` field | Used by |
|---|---|---|---|---|
| `name` | `String` | `"name"` | `name` | deployed agents |
| `version` | `Integer` | `"version"` | `version` | deployed agents (optional) |
| `agentConfig` | `Object` (map) | `"agentConfig"` (native path) | `agentConfig` | native agents |
| `framework` | `String` | `"framework"` (framework path) | `framework` | framework agents |
| `rawConfig` | `Object` (map) | `"rawConfig"` (framework path) | `rawConfig` | framework agents |
| `model` | `String` | `"model"` | `model` | optional model override |
| `skillRef` | `Map<String,Object>` | `"skillRef"` | `skillRef` | framework skill agents |
| `prompt` | `String` | `"prompt"` | `prompt` | start only |
| `sessionId` | `String` | `"sessionId"` | `sessionId` | start (stateful) |
| `runId` | `String` | `"runId"` | `runId` | start (stateful isolation) |
| `staticPlan` | `Object` (map) | `"static_plan"` (`AgentRuntime` calls `plan.toJson()`) | `staticPlan` (`@JsonProperty("static_plan")`) | start (PLAN_EXECUTE) |
| `media` | `List<String>` | `"media"` | `media` | start (multi-modal) |
| `context` | `Map<String,Object>` | `"context"` | `context` | start |
| `idempotencyKey` | `String` | `"idempotencyKey"` | `idempotencyKey` | start |
| `credentials` | `List<String>` | `"credentials"` | `credentials` | compile / start |
| `timeoutSeconds` | `Integer` | `"timeoutSeconds"` | `timeoutSeconds` | compile / start |

Null fields are never written — the class is annotated `@JsonInclude(NON_NULL)`. The three request forms are mutually exclusive: use deployed `name`/`version`, inline native `agentConfig`, or framework `framework` plus `rawConfig`/`skillRef`.

**Framework wire values:**

| Enum constant | Wire value |
|---|---|
| `Framework.OPENAI` | `"openai"` |
| `Framework.GOOGLE_ADK` | `"google_adk"` |
| `Framework.LANGCHAIN` | `"langchain"` |
| `Framework.LANGGRAPH` | `"langgraph"` |
| `Framework.SKILL` | `"skill"` |
| `Framework.VERCEL_AI` | `"vercel_ai"` |
| `Framework.CLAUDE_AGENT_SDK` | `"claude_agent_sdk"` |

`AgentRuntime` resolves `agent.getFramework()` → `Framework` via `Framework.of(String)` (returns `Optional.empty()` for unrecognised strings, routing them through the native path).

`staticPlan` is serialized as `"static_plan"`.

---

## RespondBody

Input to `respond`. Provides factory methods for the three common patterns; arbitrary extra fields are flattened to the top level via `@JsonAnyGetter`.

```java
RespondBody.approve()                  // { "approved": true }
RespondBody.approve("Looks good")      // { "approved": true, "reason": "Looks good" }
RespondBody.reject("Needs review")     // { "approved": false, "reason": "Needs review" }
RespondBody.of(Map.of("selected", "writer"))  // { "selected": "writer" }  ← MANUAL strategy
```

**Used by `AgentHandle`:**

| `AgentHandle` method | `RespondBody` factory | Wire JSON |
|---|---|---|
| `handle.approve()` | `RespondBody.approve()` | `{ "approved": true }` |
| `handle.approve(comment)` | `RespondBody.approve(comment)` | `{ "approved": true, "reason": "..." }` |
| `handle.reject(reason)` | `RespondBody.reject(reason)` | `{ "approved": false, "reason": "..." }` |
| `handle.respond(map)` | `RespondBody.of(map)` | the map at the top level |

---

## compileAgent

Compile an agent into a Conductor workflow definition. No workflow is registered or executed.

Used by `AgentRuntime.plan(agent)`.

**HTTP:** `POST /api/agent/compile`

### Request body — `AgentRequest`

```java
// AgentRuntime builds this via agentRequest(agent), serializing the agent first:
AgentRequest.nativeAgent(AGENT_SERIALIZER.serialize(agent)).build()
// or, for framework agents — the resolved framework's wire name:
AgentRequest.frameworkAgent(fw.wireValue(), AGENT_SERIALIZER.serialize(agent)).build()
```

Native agent wire shape (the map produced by `AgentConfigSerializer.serialize(agent)`):
```json
{ "agentConfig": { "name": "my_agent", "model": "anthropic/claude-sonnet-4-6", "strategy": "handoff", ... } }
```

Framework agent wire shape:
```json
{ "framework": "openai", "rawConfig": { "name": "my_agent", "model": "anthropic/claude-sonnet-4-6", "tools": [...] } }
```

### Response — `CompileResponse`

```json
{ "workflowDef": { "name": "my_agent", "version": 1, "tasks": [...] }, "requiredWorkers": ["my_tool_a"] }
```

| Field | Getter | Type | Description |
|---|---|---|---|
| `workflowDef` | `getWorkflowDef()` | `Map<String,Object>` | Full Conductor workflow definition. |
| `requiredWorkers` | `getRequiredWorkers()` | `List<String>` | Task type names the SDK must register local workers for. |

**How the SDK uses it:** `AgentRuntime.plan(agent)` returns the `CompileResponse` directly.

---

## deployAgent

Compile and register the workflow definition on the server without starting an execution. Idempotent.

Used by `AgentRuntime.deploy(Agent...)`.

**HTTP:** `POST /api/agent/deploy`

### Request body — `AgentRequest`

Same as `compileAgent` — agent definition only, no `prompt`.

### Response — `StartResponse`

```json
{ "agentName": "my_agent", "requiredWorkers": ["my_tool_a"] }
```

| Field | Getter | Type | Description |
|---|---|---|---|
| `agentName` | `getAgentName()` | `String` | The registered workflow name on the server. |
| `requiredWorkers` | `getRequiredWorkers()` | `List<String>` | Task type names the SDK must have workers running for. |
| `executionId` | `getExecutionId()` | `String` | Always `null` for deploy — no execution was started. |

**How the SDK uses it:** `AgentRuntime.deploy()` reads `resp.getAgentName()` and wraps it in `DeploymentInfo`.

---

## startAgent

Compile, register, and start a workflow execution in one call.

Used by `AgentRuntime.startAsync(agent, prompt, plan)`.

**HTTP:** `POST /api/agent/start`

### Request body — `AgentRequest`

Start an already-deployed agent without resending its definition:

```java
StartResponse response = client.startAgent(
    AgentRequest.deployedAgent("researcher", 3)
        .prompt("What changed in the latest release?")
        .build());
```

```json
{
  "name": "researcher",
  "version": 3,
  "prompt": "What changed in the latest release?"
}
```

Inline definitions remain supported:

```json
{
  "agentConfig": { ... },
  "prompt": "What is the capital of France?",
  "sessionId": "session-abc",
  "runId": "a1b2c3d4e5f6...",
  "static_plan": { "steps": [...] },
  "idempotencyKey": "question-123"
}
```

The idempotency key is optional. Callers that retry the same logical start
should reuse the same stable value; the client does not generate one.

### Response — `StartResponse`

```json
{ "executionId": "a3f92b1c-8e4d-4b7a-9c2e-1d5f3a8e6b02", "agentName": "my_agent", "requiredWorkers": ["my_tool_a"] }
```

| Field | Getter | Type | Description |
|---|---|---|---|
| `executionId` | `getExecutionId()` | `String` | Conductor workflow ID. `@JsonAlias` handles legacy keys (`workflowId`, `id`, `correlationId`). |
| `agentName` | `getAgentName()` | `String` | The registered workflow name. |
| `requiredWorkers` | `getRequiredWorkers()` | `List<String>` | Task type names the SDK must have workers polling before the agent can progress. |

**How the SDK uses it:** `AgentRuntime.startAsync()` reads `response.getExecutionId()` and passes it to `new AgentHandle(executionId, agentClient, workflowClient)`.

---

## getAgentStatus

Poll the current status of a running or completed execution.

Used by `AgentHandle.waitForResult()` and `AgentHandle.waitUntilWaiting()`.

**HTTP:** `GET /api/agent/{executionId}/status`

### Response — `AgentStatusResponse`

```json
{ "executionId": "...", "status": "COMPLETED", "startTime": 1710000000000, "endTime": 1710000005000, "isComplete": true, "isRunning": false, "output": { ... } }
```

HITL paused:
```json
{ "status": "RUNNING", "isWaiting": true, "pendingTool": { "taskRefName": "...", "tool_name": "...", "parameters": { ... } } }
```

**`AgentStatusResponse` fields:**

| JSON field | Getter | Type | Source | Description |
|---|---|---|---|---|
| `executionId` | `getExecutionId()` | `String` | path param | — |
| `status` | `getStatus()` | `String` | `workflow.getStatus().name()` | `RUNNING`, `COMPLETED`, `FAILED`, `TERMINATED`, `TIMED_OUT`, `PAUSED` |
| `startTime` | `getStartTime()` | `Long` | workflow start timestamp | Epoch milliseconds; nullable until supplied by the server |
| `endTime` | `getEndTime()` | `Long` | workflow end timestamp | Epoch milliseconds; nullable while execution is active |
| `isComplete` | `isComplete()` | `boolean` | `workflow.getStatus().isTerminal()` | `true` for all terminal statuses |
| `isRunning` | `isRunning()` | `boolean` | `status == RUNNING` | — |
| `output` | `getOutput()` | `Map<String,Object>` | `workflow.getOutput()` | Only present when `isComplete() == true` |
| `reasonForIncompletion` | `getReasonForIncompletion()` | `String` | `workflow.getReasonForIncompletion()` | Only present on non-COMPLETED terminal status |
| `isWaiting` | `isWaiting()` | `boolean` | HUMAN task IN_PROGRESS | `true` when a HITL task is paused |
| `pendingTool` | `getPendingTool()` | `PendingTool` | HUMAN task inputData | Only when `isWaiting() == true` |

**`PendingTool` fields:**

| JSON field | Getter | Type | Description |
|---|---|---|---|
| `taskRefName` | `getTaskRefName()` | `String` | Conductor task reference name. |
| `tool_name` | `getToolName()` | `String` | Logical tool name shown to the human. |
| `parameters` | `getParameters()` | `Map<String,Object>` | Args the agent passed to the tool. |
| `response_schema` | `getResponseSchema()` | `Object` | JSON Schema the response must conform to (optional). |
| `response_ui_schema` | `getResponseUiSchema()` | `Object` | UI rendering hints (optional). |

---

## respond

Resume a paused HITL execution.

Used by `AgentHandle.approve()`, `.reject()`, `.respond(Map)` and `AgentStream.approve()`, `.reject()`.

**HTTP:** `POST /api/agent/{executionId}/respond`

### Request body — `RespondBody`

```json
{ "approved": true }
{ "approved": false, "reason": "Needs review" }
{ "selected": "writer" }
```

### Response

`void` — returns nothing. Throws `AgentAPIException` if no pending HUMAN task exists.

---

## cancelAgent

Immediately cancel an agent execution. This is distinct from graceful stopping: cancellation terminates now and may record an operator reason.

```java
client.cancelAgent(executionId, "Superseded by a newer request");
```

**HTTP:** `DELETE /api/agent/{executionId}/cancel?reason=Superseded%20by%20a%20newer%20request`

The `reason` query parameter is omitted when the Java argument is `null`, empty, or blank. HTTP 404 maps to `AgentNotFoundException`; other HTTP failures map to `AgentAPIException` through the normal client transport.

---

## stopAgent

Request a graceful deterministic stop after the current agent iteration finishes. Unlike cancellation, this preserves the current iteration and does not take a reason.

```java
client.stopAgent(executionId);
```

**HTTP:** `POST /api/agent/{executionId}/stop`

Use `stopAgent` when the current iteration should finish cleanly; use `cancelAgent` when work must terminate immediately.

---

## WorkflowClient usage

Raw workflow data (`GET /api/workflow/{id}`) is fetched via the standard Conductor `WorkflowClient` — not `AgentClient`. `AgentClient` owns only `/api/agent/*`.

`WorkflowClient.getWorkflow(id, true)` is called inside `AgentHandle.buildResult()` **once**, after `getAgentStatus` returns terminal, to walk the typed `Workflow`/`Task` objects and compute:

- **Token usage** — `LLM_CHAT_COMPLETE` task `outputData`: `promptTokens`, `completionTokens`, `tokenUsed` → `AgentResult.getTokenUsage()`
- **Tool calls** — worker tasks whose `referenceTaskName` starts with `call_` → `AgentResult.getToolCalls()`

Fires automatically inside `run()` / `waitForResult()`. Callers never invoke it directly.

---

## AgentConfig (request field)

The agent definition serialized under the `agentConfig` key by `AgentConfigSerializer`.

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Agent/workflow name. |
| `model` | `String` | `"provider/model"` e.g. `"anthropic/claude-sonnet-4-6"`. |
| `instructions` | `String \| Object` | System prompt or `PromptTemplateRef`. |
| `tools` | `List<ToolConfig>` | Tool definitions. |
| `agents` | `List<AgentConfig>` | Sub-agents (for multi-agent strategies). |
| `strategy` | `String` | `"handoff"` (default), `"sequential"`, `"parallel"`, `"router"`, `"swarm"`, `"round_robin"`, `"random"`, `"plan_execute"`, `"manual"`. |
| `router` | `AgentConfig \| WorkerRef` | For `"router"` strategy. |
| `guardrails` | `List<GuardrailConfig>` | Input/output guardrails. |
| `maxTurns` | `int` | Default `100`. |
| `maxTokens` | `Integer` | LLM `max_tokens`. |
| `temperature` | `Double` | LLM temperature. |
| `timeoutSeconds` | `int` | Execution timeout. |
| `credentials` | `List<String>` | Credential names injected at runtime. |
| `outputType` | `OutputTypeConfig` | Structured output definition. |
| `termination` | `TerminationConfig` | Early termination condition. |
| `handoffs` | `List<HandoffConfig>` | Swarm handoff triggers. |
| `callbacks` | `List<CallbackConfig>` | Before/after model callbacks. |
| `codeExecution` | `CodeExecutionConfig` | Local code execution settings. |
| `cliConfig` | `CliConfig` | CLI command execution settings. |
| `planner` | `AgentConfig` | `PLAN_EXECUTE`: agent that produces the plan. |
| `fallback` | `AgentConfig` | `PLAN_EXECUTE`: agent used when the plan fails. |
| `plannerContext` | `List<Map>` | `PLAN_EXECUTE`: text/URL context appended to the planner's prompt. |
| `synthesize` | `Boolean` | Append a synthesis step after parallel sub-agents. |
| `includeContents` | `String` | `"none"` = fresh context; absent = inherit parent context. |
| `baseUrl` | `String` | Per-agent LLM provider base URL override. |
| `metadata` | `Map<String,Object>` | Arbitrary metadata stored with the workflow definition. |
| `framework` | `String` | Framework ID — set by SDK bridges, not by callers directly. |

### ToolConfig

| Field | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | | Tool name shown to the LLM. |
| `description` | `String` | | Tool description. |
| `inputSchema` | `Map<String,Object>` | | JSON Schema for tool parameters. |
| `outputSchema` | `Map<String,Object>` | | JSON Schema for tool return value. |
| `toolType` | `String` | `"worker"` | `"worker"`, `"http"`, `"mcp"`, `"human"`, `"generate_image"`, `"generate_audio"`, `"generate_pdf"`, `"rag_search"`, `"pull_workflow_messages"`. |
| `approvalRequired` | `boolean` | `false` | Pause for human approval before executing. |
| `timeoutSeconds` | `Integer` | | Per-tool execution timeout. |
| `maxCalls` | `Integer` | | Maximum invocations per run. |
| `config` | `Map<String,Object>` | | Type-specific config: `url`/`method`/`headers` for HTTP; `server_url` for MCP. |
| `guardrails` | `List<GuardrailConfig>` | | Tool-level guardrails. |
| `stateful` | `boolean` | `false` | Register worker under a per-execution domain (prevents cross-instance task stealing). |
