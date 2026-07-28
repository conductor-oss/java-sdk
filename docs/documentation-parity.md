# Documentation parity: Java and Python SDKs

**Audience:** maintainers keeping the Java and Python SDK documentation aligned. **Prerequisites:** none.

The Java and Python SDKs share one documentation information architecture, so a reader moving between languages finds the same guides in the same places. This page records where the two trees agree, where they deliberately diverge, and which gaps are real work rather than intentional differences.

The counterpart page in the Python SDK is `docs/documentation-parity.md` in [conductor-oss/python-sdk](https://github.com/conductor-oss/python-sdk).

**Snapshot date:** 2026-07-28. Re-verify before relying on the divergence tables; the shared sets change as either SDK adds guides.

## How to use this page

- Adding a guide to a shared set below? Add it to both SDKs, or record it here as an intentional divergence with a reason.
- Porting documentation between SDKs? Check the direction first. This architecture originated in the Java SDK, so a Python change that cites "alignment with the Java SDK" is usually Python catching up, not a change to mirror back. Diff the file lists before writing anything.
- Language-idiomatic names are expected to differ (`langchain4j` here, `langchain` there). Only the *concept* needs to exist in both.

## Shared sets

These are identical in both SDKs, filename for filename. Treat them as a contract.

| Set | Files |
|---|---|
| Core guides (`docs/*.md`) | `api-map`, `compatibility`, `connection-authentication`, `core-quickstart`, `debugging`, `deployment-scaling`, `documentation-standard`, `examples`, `observability`, `README`, `reliability`, `schedules-events`, `schema-client`, `security`, `server-setup`, `upgrading`, `workers`, `workflow-lifecycle`, `workflow-testing`, `workflows` |
| Agent concepts (`docs/agents/concepts/*.md`) | `agents`, `callbacks`, `deploy-serve-run`, `guardrails`, `multi-agent`, `scheduling`, `stateful`, `streaming-hitl`, `structured-output`, `termination`, `tools` |
| Agent reference (`docs/agents/reference/`) | `agent-definition.md`, `agent-schema.md`, `agent-schema.json`, `api.md`, `client.md`, `runtime.md` |
| Agent hub | `docs/agents/README.md`, `docs/agents/getting-started.md` |

The published [agent schema](agents/reference/agent-schema.md) and its `agent-schema.json` artifact are a cross-SDK wire contract, not just documentation. Both SDKs ship the same schema and both verify it in CI.

## Java-only, by design

| Page | Why Python has no counterpart |
|---|---|
| [Files](file-client.md) | `FileClient` is a Java-only client surface. |
| [Spring Boot selector](spring-boot.md) and [agent Spring Boot](agents/spring-boot.md) | Spring is a JVM framework; there is no Python analogue. |

## Python-only

Paths below are relative to the Python SDK's `docs/` directory.

| Page | Status |
|---|---|
| `workflow-message-queue.md` | Real gap. No Java counterpart yet. |
| `agents/frameworks/claude-agent-sdk.md` | Real gap. Python bridges the Claude Agent SDK; Java has no equivalent bridge. |
| `AUTHORIZATION.md`, `INTEGRATION.md`, `METADATA.md`, `PROMPT.md`, `SCHEDULE.md`, `SECRET_MANAGEMENT.md`, `TASK_MANAGEMENT.md`, `WORKER.md`, `WORKFLOW.md`, `WORKFLOW_TESTING.md`, `LEASE_EXTENSION.md` | Not gaps. Pre-existing Python pages that the shared lowercase guides superseded. |
| `agents/advanced.md`, `agents/api-reference.md`, `agents/framework-agents.md`, `agents/writing-agents.md` | Not gaps. Superseded by the shared `concepts/` and `reference/` sets. The agent API-reference page is a path this repo's CI guard explicitly retires. |

## Framework bridges

Both SDKs document the same bridges under `docs/agents/frameworks/`, using each language's own package naming.

| Bridge | Java | Python |
|---|---|---|
| Google ADK | [`google-adk.md`](agents/frameworks/google-adk.md) | `google-adk.md` |
| OpenAI Agents style | [`openai.md`](agents/frameworks/openai.md) | `openai.md` |
| LangChain | [`langchain4j.md`](agents/frameworks/langchain4j.md) | `langchain.md` |
| LangGraph | [`langgraph4j.md`](agents/frameworks/langgraph4j.md) | `langgraph.md` |
| Claude Agent SDK | — | `claude-agent-sdk.md` |

## Expected result

After reading this page you can tell, for any documentation change, whether it needs a matching change in the sibling SDK. If a page you are adding belongs to a shared set and has no counterpart, either add the counterpart or add a row above explaining why it is Java-only.

## Common failure modes

- **Porting in the wrong direction.** A sibling PR titled "align with the Java SDK" is Python adopting this structure. Porting it back here is mostly a no-op — verify with a file-list diff first.
- **Mirroring a claim instead of the diff.** Sibling PR descriptions can overstate what the code does, particularly around backward-compatibility aliases. Read the diff.
- **Renaming externally-owned settings.** Server boot properties belong to the Conductor server, not this SDK. Renaming one in these docs points readers at a flag that may not exist.

## Next steps

- [Documentation standard](documentation-standard.md) — what every primary guide must contain.
- [Compatibility matrix](compatibility.md) — supported server and JDK versions.
- [API map](api-map.md) — generated Javadocs as the signature source of truth.
