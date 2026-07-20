# Core API map

**Audience:** developers choosing the typed Java client.  
**Reference policy:** generated Javadocs are canonical for signatures; source links are useful for snapshot development.

| Need | Type | Module | Route |
|---|---|---|---|
| Configure HTTP/auth transport | `ConductorClient` / `ApiClient` | core | [Javadocs](https://javadoc.io/doc/org.conductoross/conductor-client) |
| Start, query, pause, resume workflows | `WorkflowClient` | core | [source](../conductor-client/src/main/java/com/netflix/conductor/client/http/WorkflowClient.java) |
| Poll and update tasks | `TaskClient` | core | [source](../conductor-client/src/main/java/com/netflix/conductor/client/http/TaskClient.java) |
| Register task and workflow metadata | `MetadataClient` | core | [source](../conductor-client/src/main/java/com/netflix/conductor/client/http/MetadataClient.java) |
| Create schedules | `SchedulerClient` | core/Orkes clients | [source](../conductor-client/src/main/java/io/orkes/conductor/client/SchedulerClient.java) |
| Manage schemas | `SchemaClient` | core/Orkes clients | [source](../conductor-client/src/main/java/io/orkes/conductor/client/SchemaClient.java) |
| Transfer workflow-scoped files | `FileClient` | core | [guide](file-client.md) |
| Emit SDK metrics | metrics integration | metrics | [README](../conductor-client-metrics/README.md) |
| Compile, deploy, start, signal, and stream agents | `AgentClient` | core + AI | [reference](agents/reference/client.md) |
| Build/serve/run agents | `AgentRuntime` | AI | [reference](agents/reference/runtime.md) |

`OrkesClients` assembles Orkes-specific typed clients from the shared transport. Use [connection and authentication](connection-authentication.md) before constructing any client.

Next: [workflows](workflows.md), [agents](agents/README.md), and [compatibility](compatibility.md).
