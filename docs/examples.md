# Recommended examples

The [examples catalog](../examples/README.md) is the complete machine-generated catalog. These paths are the maintained starting points; do not infer equal support from every catalog entry.

| Path | Tag | Prerequisites and command | Expected outcome | Cleanup |
|---|---|---|---|---|
| [Hello World](../examples/basics/hello-world/README.md) | Start here | Java 21 and a local server; `cd examples/basics/hello-world && ./run.sh` | `Result: PASSED` | `conductor server stop`, or `docker compose down` for the Docker path |
| [Basic agent](../agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example01BasicAgent.java) | Start here | Server-side provider credentials; `./gradlew :agent-examples:run -PmainClass=org.conductoross.conductor.ai.examples.Example01BasicAgent` | Prints an `AgentResult` | Stop the local server/workers |
| [Plan and execute](../agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example108PlanExecuteRefs.java) | Production pattern | Same agent setup | Runs a durable plan with output references | Stop the local server/workers |
| [Planner context](../agent-examples/src/main/java/org/conductoross/conductor/ai/examples/Example115PlannerContext.java) | Reference only | Same agent setup and provider access | Prints the generated task trace | Stop the local server/workers |

Credentials must be configured on the Conductor server. Do not run reference-only examples against production credentials without reviewing their tools and side effects.
