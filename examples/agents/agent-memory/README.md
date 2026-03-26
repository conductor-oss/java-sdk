# Agent with Persistent Memory: Load, Think, Update, Respond

An agent that remembers across conversations. It loads conversation history and facts from memory, thinks using relevant facts to plan a response, updates memory with the new turn, and responds with a confidence score and token count.

## Workflow

```
userId, userMessage -> am_load_memory -> am_agent_think -> am_update_memory -> am_agent_respond
```

## Workers

**LoadMemoryWorker** (`am_load_memory`) -- Loads prior conversation entries (e.g., `{role: "user", content: "Hello, I'm interested in machine learning."}`).

**AgentThinkWorker** (`am_agent_think`) -- Retrieves relevant facts from memory and produces structured thoughts.

**UpdateMemoryWorker** (`am_update_memory`) -- Updates the memory snapshot: `{totalEntries: 6, factsStored: 5}`.

**AgentRespondWorker** (`am_agent_respond`) -- Generates the final response with confidence score and token count.

## Tests

33 tests cover memory loading, reasoning, memory updates, and response generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
