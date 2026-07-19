# Agent configuration schema

[`agent-schema.json`](agent-schema.json) is the canonical wire contract for an agent configuration submitted under `agentConfig` on compile and start requests. It uses JSON Schema Draft 2020-12, camel-case keys, and rejects unknown top-level properties. `agents`, `planner`, `fallback`, and `router` can recursively contain an agent configuration.

The schema is the precise contract. Use this page to understand its scope; use the JSON file for every field, type, enum, and defaultable value.

## Contract scope

The schema follows the server `AgentConfig` model and the Java SDK serializer at [`AgentConfigSerializer.java`](../../../conductor-client-ai/src/main/java/org/conductoross/conductor/ai/internal/AgentConfigSerializer.java). It covers identity and model settings, tools, sub-agents and handoffs, guardrails, memory, termination, callbacks, code execution, planning, and framework configuration.

Two compatibility details are deliberate:

- `sessionId` is permitted in the configuration because the Java SDK emits it there, although the server also reads session identity from the request wrapper.
- `planSource` is permitted for SDK compatibility even though static plans can be sent by a request-wrapper field.

## Continuous verification

[`tools/agent-schema/verify.py`](../../../tools/agent-schema/verify.py) runs in CI. It:

1. validates the schema itself with Draft 2020-12;
2. generates Python dataclasses and Java records in a temporary directory;
3. checks that every generated field exactly matches its schema object; and
4. validates representative valid and invalid configuration documents, then compiles the generated Java record source with Java 21.

The verifier never writes generated artifacts into `docs/`. It validates this repository’s structural contract; cross-repository server or SDK interoperability still needs the relevant integration test in that repository.

## Use it

Validate a document locally with the same CI command:

```bash
python -m pip install -r tools/agent-schema/requirements.txt
python tools/agent-schema/verify.py
```

For the Java builder surface, see [agent definition fields](agent-definition.md). For request envelopes and control-plane responses, see [AgentClient](client.md).
