# LangGraph4j

Run a [LangGraph4j](https://github.com/bsorrentino/langgraph4j) `AgentExecutor` on the durable
Conductor agent runtime. Hand the runtime a native `AgentExecutor.Builder` and it recovers the
configured `ChatModel` (and system message, if any), then runs the agent server-side.

## Dependency

```groovy
implementation 'org.conductoross:conductor-client-ai:<VERSION>'
implementation 'dev.langchain4j:langchain4j:1.0.0'
implementation 'dev.langchain4j:langchain4j-open-ai:1.0.0'
implementation 'org.bsc.langgraph4j:langgraph4j-core:1.6.0-beta5'
implementation 'org.bsc.langgraph4j:langgraph4j-agent-executor:1.6.0-beta5'
```

These framework versions are exercised by this repository's agent examples. Replace `<VERSION>` with a published SDK version from [Maven Central](https://search.maven.org/search?q=g:org.conductoross).

## Usage (drop-in)

The runtime accepts the native `AgentExecutor.Builder` directly — no Conductor-specific types required.

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;

// apiKey is required by the LangChain4j builder but unused — Conductor runs the
// LLM call on the server using server-registered credentials.
ChatModel model = OpenAiChatModel.builder()
    .apiKey("conductor-server-handles-credentials")
    .modelName("gpt-4o-mini")
    .build();

AgentExecutor.Builder agent = AgentExecutor.builder().chatModel(model);

try (AgentRuntime runtime = new AgentRuntime()) {
    AgentResult result = runtime.run(agent, "Tell me a fun fact about state machines.");
    result.printResult();
}
```

`run`, `start`, and `stream` all accept the `AgentExecutor.Builder` drop-in. To attach `@Tool`
POJOs, pass them as trailing arguments — `runtime.run(agent, prompt, new MyTools())`.

The builder must have a `chatModel` set (the runtime fails fast otherwise) and must build into a
valid LangGraph4j `StateGraph` — the runtime validates this before submitting the agent.

## See also

- [LangChain4j](langchain4j.md) — for `@Tool`-POJO agents and `ChatModel` + `LangChainBridge`.
