# Legacy Gradle Examples

These are the original Gradle-based examples, preserved for reference. For new projects, use the [791 Maven examples](../) instead — they are self-contained, easier to run, and all OSS-compatible.

## Running

```bash
# Start Conductor locally
docker run -d -p 8080:8080 -p 1234:5000 conductoross/conductor:3.30.0.rc12

export CONDUCTOR_SERVER_URL=http://localhost:8080/api

./gradlew run --main-class <ClassName>
```

## OSS-Compatible Examples

These work with a plain OSS Conductor server — only `CONDUCTOR_SERVER_URL` required, no auth keys.

| Class | Description |
|-------|-------------|
| `com.netflix.conductor.gettingstarted.CreateWorkflow` | Register a workflow definition |
| `com.netflix.conductor.gettingstarted.StartWorkflow` | Start a workflow |
| `com.netflix.conductor.gettingstarted.HelloWorker` | Poll and execute tasks |
| `com.netflix.conductor.sdk.examples.helloworld.Main` | End-to-end hello world |
| `com.netflix.conductor.sdk.examples.TaskRunner` | Task polling with workers |
| `com.netflix.conductor.sdk.examples.TaskRegistration` | Register task definitions |
| `com.netflix.conductor.sdk.examples.events.EventHandlerExample` | Event handler registration |
| `com.netflix.conductor.sdk.examples.events.EventListenerExample` | Event listener setup |
| `com.netflix.conductor.sdk.examples.taskdomains.Main` | Task domain routing |
| `com.netflix.conductor.sdk.examples.shipment.Main` | Shipment workflow |
| `io.orkes.conductor.sdk.examples.WorkflowManagement` | Workflow CRUD operations |
| `io.orkes.conductor.sdk.examples.WorkflowManagement2` | Workflow execution patterns |
| `io.orkes.conductor.sdk.examples.workflowops.Main` | Workflow operations |

## Orkes Cloud–Only Examples

These require an Orkes Cloud server with `CONDUCTOR_AUTH_KEY` and `CONDUCTOR_AUTH_SECRET` set.

| Class | Why Orkes-only |
|-------|----------------|
| `io.orkes.conductor.sdk.examples.AuthorizationManagement` | Uses Orkes authorization API |
| `io.orkes.conductor.sdk.examples.SchedulerManagement` | Uses Orkes scheduler API |
| `io.orkes.conductor.sdk.examples.MetadataManagement` | Uses `OrkesClients` metadata extensions |
| `io.orkes.conductor.sdk.examples.agentic.FunctionCallingExample` | Requires LLM provider configured in server |
| `io.orkes.conductor.sdk.examples.agentic.LlmChatExample` | Requires LLM provider configured in server |
| `io.orkes.conductor.sdk.examples.agentic.RagWorkflowExample` | Requires vector DB + LLM integration |
| `io.orkes.conductor.sdk.examples.agentic.VectorDbExample` | Requires vector DB integration |
| `io.orkes.conductor.sdk.examples.agentic.HumanInLoopChatExample` | Requires LLM + human task integration |
| `io.orkes.conductor.sdk.examples.agentic.AgenticExamplesRunner` | Runner for the agentic examples above |
