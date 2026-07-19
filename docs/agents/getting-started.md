# Run your first durable AI agent

This path starts a local Conductor server with an LLM provider credential, then runs the maintained basic-agent example from this repository. The LLM credential belongs to the **server process** because Conductor performs the model call.

## Prerequisites

- Java 21+
- Node.js and npm for the recommended local-server path
- An OpenAI API key, or an existing Conductor server that already has an LLM provider configured

## 1. Start a server with a provider credential

Install the [Conductor CLI](https://github.com/conductor-oss/conductor-cli) with npm, then start the local server. Configure the provider key in the server process environment before starting it:

```bash
export OPENAI_API_KEY='set-this-in-your-shell'
npm install -g @conductor-oss/conductor-cli
conductor server start
```

Wait until the server is healthy:

```bash
curl --fail http://localhost:8080/health
```

If you need a containerized server, use the optional [Docker setup](../server-setup.md#optional-docker). If you use an existing OSS or Orkes server, use its URL and authentication instead. The selected `provider/model` must be configured on that server.

## 2. Add the dependency

### Gradle

```groovy
dependencies {
    implementation 'org.conductoross:conductor-client-ai:<VERSION>'
}
```

### Maven

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client-ai</artifactId>
    <version>&lt;VERSION&gt;</version>
</dependency>
```

Replace `<VERSION>` with a published version from [Maven Central](https://search.maven.org/search?q=g:org.conductoross).

## 3. Run the maintained example

From this SDK repository, in a second terminal:

```bash
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini \
  ./gradlew :agent-examples:run \
  -PmainClass=org.conductoross.conductor.ai.examples.Example01BasicAgent
```

`CONDUCTOR_AGENT_LLM_MODEL` is read by this repository's examples; it is not an `AgentRuntime` configuration variable. The expected result is an `AgentResult` with a completed status and a response to the capital-of-France question.

## 4. Create the same agent in your application

```java
import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;

Agent agent = Agent.builder()
        .name("hello_agent")
        .model("openai/gpt-4o-mini")
        .instructions("You are a concise assistant. Answer in one sentence.")
        .build();

try (AgentRuntime runtime = new AgentRuntime()) {
    AgentResult result = runtime.run(agent, "What is 2 + 2?");
    System.out.println(result.getOutput());
}
```

`AgentRuntime` reads `CONDUCTOR_SERVER_URL`, `CONDUCTOR_AUTH_KEY`, and `CONDUCTOR_AUTH_SECRET`. It normalizes either `http://host:8080` or `http://host:8080/api` to the API endpoint.

## First troubleshooting steps

| Symptom | Check |
|---|---|
| Connection failure | `curl --fail http://localhost:8080/health` succeeds and `CONDUCTOR_SERVER_URL` points to `/api`. |
| Authentication failure | Set the client key and secret for the target server; do not place them in source. |
| Model/provider error | The server container or remote server—not only the Java client—has the provider credential and supports the selected model. |
| Tool task stays scheduled | Keep the process containing `AgentRuntime` alive so its local tool workers can poll. |

## Next steps

- [Add Java tools](concepts/tools.md)
- [Build multi-agent and plan-execute systems](concepts/multi-agent.md)
- [Deploy or serve agents](concepts/deploy-serve-run.md)
- [Use Google ADK, LangChain4j, LangGraph4j, or OpenAI-style bridges](README.md#framework-bridges)
