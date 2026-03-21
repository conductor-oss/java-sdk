# ReAct Agent in Java Using Conductor: Reason-Act-Observe Loop for Question Answering

Someone asks "What's the GDP per capita of the country that hosted the 2024 Olympics?" A standard LLM either guesses or admits it doesn't know. A naive agent fires off a search, gets a wall of text about the Paris ceremony, and tries to answer from that alone. never connecting the dots to look up France's GDP. It acted before it thought, and now it's reasoning backwards from incomplete data it already committed to. The ReAct pattern fixes this: reason first (what do I need to know?), then act (search for it), then observe (did that answer my question or do I need another step?). This example builds that loop with Conductor's `DO_WHILE`. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Questions That Require Multi-Step Reasoning and Action

"What is the GDP per capita of the country that hosted the 2024 Olympics?" can't be answered in one step. The agent needs to reason ("I need to find which country hosted the 2024 Olympics"), act (search for "2024 Olympics host country"), observe the result ("France"), reason again ("Now I need France's GDP per capita"), act again (search for "France GDP per capita"), and observe ("$44,408"). Only then can it produce the final answer.

The ReAct pattern interleaves reasoning (what should I do next?) with acting (executing the chosen action) and observing (interpreting the result). The agent decides when it has enough information to stop, making this an inherently iterative process. Without orchestration, the reasoning-acting-observing loop with proper state management, action retry logic, and loop termination conditions requires complex stateful code.

## The Solution

**You write the reasoning, action execution, and observation logic. Conductor handles the ReAct loop, context accumulation, and termination control.**

`InitTaskWorker` sets up the question and initializes the reasoning context. A `DO_WHILE` loop then iterates: `ReasonWorker` examines the question and accumulated observations to decide the next action (search, calculate, or answer). `ActWorker` executes the chosen action. Web search, calculation, database lookup, and returns raw results. `ObserveWorker` interprets the action results, updates the context with new information, and determines whether the agent has enough information to answer. After the loop exits, `FinalAnswerWorker` produces the answer using all accumulated observations. Conductor manages the growing context across iterations and records every reasoning-action-observation triple.

### What You Write: Workers

Five workers drive the ReAct loop. Initializing the question, then iterating through reasoning, action execution, and observation until the answer is found.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `rx_act` | Executes the action decided by the ReasonWorker. If the action is "search", returns a deterministic search result. Ot |
| **FinalAnswerWorker** | `rx_final_answer` | Produces the final answer after all ReAct iterations are complete. Returns a deterministic answer with a confidence s |
| **InitTaskWorker** | `rx_init_task` | Initializes the ReAct agent loop by capturing the question and creating an empty context list for subsequent iterations. |
| **ObserveWorker** | `rx_observe` | Observes the result of the action and determines whether the information is useful for answering the question. Always |
| **ReasonWorker** | `rx_reason` | Reasons about the question and decides what action to take next. Produces a thought, an action type, and a query for |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
rx_init_task
 │
 ▼
DO_WHILE
 └── rx_reason
 └── rx_act
 └── rx_observe
 │
 ▼
rx_final_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
