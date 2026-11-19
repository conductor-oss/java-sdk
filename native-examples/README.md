# Native workflow examples

Workflows written as plain Java: a `@Workflow` method calls tasks, decides with `if` and `switch`,
loops with `for` and `while`, and Conductor runs it durably on the server. Read the examples in
order; each one is a single file with a `main` that hosts the workflow, starts it and prints the
result.

| # | Example | What it shows |
|---|---|---|
| 01 | [Hello world](src/main/java/examples/ex01_hello_world/HelloWorld.java) | a workflow that calls two `@WorkerTask` methods, one after the other |
| 02 | [Conditionals](src/main/java/examples/ex02_conditionals/Conditionals.java) | decisions in plain Java (`if`, `switch`) and with `switchOn` |
| 03 | [Loops](src/main/java/examples/ex03_loops/Loops.java) | each loop twice: plain Java `for`, `while` and `do`-`while`, then `forEach`, `repeatUntil` and `doWhile` |
| 04 | [Parallel](src/main/java/examples/ex04_parallel/Parallel.java) | `async` and `join`; `fanOut`, a branch per item |
| 05 | [Sub workflows](src/main/java/examples/ex05_sub_workflows/SubWorkflows.java) | `child` to wait for a workflow's result, `startWorkflow` to start one and carry on |
| 06 | [Signals](src/main/java/examples/ex06_signals/Signals.java) | `@Signal`, `@Update`, `@Query`, `@Variable` and `awaitCondition`: a shopping cart |
| 07 | [Timers](src/main/java/examples/ex07_timers/Timers.java) | durable `sleep`, `now()`, and `@Schedule` |
| 08 | [Error handling](src/main/java/examples/ex08_error_handling/ErrorHandling.java) | retries, `NonRetryableException`, `catch (TaskFailure e)`, undoing with a saga, `WorkflowFailedException` |
| 09 | [HTTP](src/main/java/examples/ex09_http/HttpCalls.java) | `http(ref)`: GET and POST from the server, typed responses |
| 10 | [Human in the loop](src/main/java/examples/ex10_human_in_the_loop/HumanInTheLoop.java) | `human(ref)` for a person's decision, `external(ref)` for another system, with timeouts |
| 11 | [LLM](src/main/java/examples/ex11_llm/LlmCalls.java) | `llm(ref)`: text, and structured output read into a record |
| 12 | [MCP](src/main/java/examples/ex12_mcp/McpTools.java) | `mcp(ref)`: list an MCP server's tools and call them |
| 13 | [Agent](src/main/java/examples/ex13_agent/AgentCalls.java) | `agent(ref)`: a conversation with an A2A agent, and a long-running agent in `async` |
| 14 | [Agent harness](src/main/java/examples/ex14_agent_harness/AgentHarness.java) | a durable agent loop: a model plans each step, the workflow calls MCP tools, delegates to helper agents and asks a person |

Each example has an in-memory test next to it under `src/test/java/examples`. The test runs the
workflow without a server, with the answers of tasks, models, tools and agents given by the
test. Use them as a starting point for testing your own workflows.

## Running

You need a Conductor server. Examples 09 and 11 to 14 also need the stub in `stubs/ai_stub.py`,
which stands in for a web API, a chat model, an MCP server and an A2A agent:

```shell
python3 native-examples/stubs/ai_stub.py 8765 &
```

Start the server with these settings:

| Setting | Needed by |
|---|---|
| `conductor.workflow-message-queue.enabled=true` | 06, for its signal, update and query handlers |
| `conductor.ai.litellm.baseURL=http://localhost:8765` | 11 and 14, so that the server's LiteLLM provider reaches the stub's model |
| `conductor.a2a.callback.url=<the server's URL>` | only for agents called with `pushNotifications(...)` (not used in 13) |

Then run any example by its number and name, from the repository root:

```shell
export CONDUCTOR_SERVER_URL=http://localhost:8080/api
./gradlew :native-examples:ex01_hello_world
./gradlew :native-examples:ex14_agent_harness
```

Each prints the execution's id: open it in the Conductor UI, at `/execution/<id>`, to see the diagram. Set `STUB_URL`
when the stub is not on `http://localhost:8765`.

Run the examples' tests:

```shell
./gradlew :native-examples:test --tests 'examples.*'
```

## More examples

- `agents.SupportTicket` and `agents.SupportDesk`: an agentic support desk. A model routes each
  ticket, a `switchOn` hands it to that team's agent or to a person, and `fanOut` handles a whole
  inbox at once.
- `reuse.Reuse`: a workflow that calls workers another team already runs, by name and through a
  typed interface, with a task domain.

## Everything else in this module

The other packages are the SDK's own checks, not examples to learn from:
- `kitchensink`: what `./gradlew :native-examples:run` runs.
- `primitives`: one workflow per construct, with checks.
- `agents.Agents`, `conformance`, `branches`, `switches`, `loops`, `operator`, `failures`,
  `retrythrow` and `hold`: checks against a server.
- `durability`: the durability bench and its chaos runs.
- `drawing`: the drawing checks.
