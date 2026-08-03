# API map

Use this page to choose the detailed reference; it intentionally does not duplicate every signature.

| Need | Reference |
|---|---|
| Run, stream, deploy, serve, resume, or schedule an agent | [AgentRuntime](runtime.md) |
| Call `/api/agent/*` directly or inspect request/response shapes | [AgentClient control plane](client.md) |
| Build an `Agent` or inspect serialized fields | [Agent definition fields](agent-definition.md) |
| Validate the agent configuration contract | [Agent configuration schema](agent-schema.md) |

## Primary types

`AgentRuntime` is the high-level, thread-safe entry point. Use `run` when the caller waits for an `AgentResult`, `start` when it needs an `AgentHandle`, `stream` for events, and `deploy` or `serve` for long-lived agent definitions. The [runtime reference](runtime.md) contains constructors, overloads, environment variables, and lifecycle behavior.

`AgentClient`, from `new OrkesClients(conductorClient).getAgentClient()`, is the lower-level control-plane client. Use it for direct compile, deploy, start, status, response, cancellation, and SSE operations; see the [client reference](client.md).

## Related guides

- [Choose a runtime mode](../concepts/deploy-serve-run.md)
- [Define agents](../concepts/agents.md)
- [Add tools](../concepts/tools.md)
- [Schedule deployed agents](../concepts/scheduling.md)
