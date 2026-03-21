# Agent Memory in Java Using Conductor : Load, Think, Update, and Respond with Persistent Context

Agent with Memory. loads conversation history, thinks with context, updates memory, and responds. ## Stateless Agents Forget Everything Between Messages

Without memory, every agent interaction starts from zero. A user says "I'm interested in transformers" and gets a great explanation. Next message: "How do they compare to RNNs?", and the agent has no idea what "they" refers to because the previous context is gone. The user has to repeat themselves every time.

Agent memory solves this by maintaining a persistent store of facts, topics, and context across conversations. Before responding, the agent loads relevant memories ("user is learning about neural architectures, discussed transformers last time"), reasons with that context, updates the store with any new facts from the current exchange, and generates a response that builds on prior conversations. Each of these steps can fail independently. the memory store might be temporarily unavailable, the reasoning step might time out, and you need to handle failures without corrupting the memory state.

## The Solution

**You write the memory retrieval, reasoning, and persistence logic. Conductor handles sequencing, state management, and failure recovery.**

`LoadMemoryWorker` retrieves the user's memory store. prior topics, facts, and conversation context, from a session-keyed data store. `AgentThinkWorker` reasons about the current message using both the user input and loaded memories, identifying relevant prior context and planning a response strategy. `UpdateMemoryWorker` extracts new facts from the current exchange and persists them to the memory store for future conversations. `AgentRespondWorker` generates the final response informed by all prior reasoning and memories. Conductor ensures memories are loaded before reasoning, updated before responding, and that a failed memory update doesn't block the response.

### What You Write: Workers

Four workers manage stateful conversations. Loading memory, reasoning with context, updating the memory store, and generating a context-aware response.

| Worker | Task | What It Does |
|---|---|---|
| **AgentRespondWorker** | `am_agent_respond` | Generates the final response based on thoughts, relevant facts, and updated memory. Returns the response string, conf... |
| **AgentThinkWorker** | `am_agent_think` | Analyzes user message in the context of conversation history and user profile. Returns thoughts, relevantFacts, and f... |
| **LoadMemoryWorker** | `am_load_memory` | Loads conversation history and user profile from memory for the given user. Returns conversationHistory (list of entr... |
| **UpdateMemoryWorker** | `am_update_memory` | Updates the memory store with the latest interaction data. Returns a memorySnapshot and memorySize. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
am_load_memory
 │
 ▼
am_agent_think
 │
 ▼
am_update_memory
 │
 ▼
am_agent_respond

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
