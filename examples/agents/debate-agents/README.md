# Debate Agents in Java Using Conductor : Pro and Con Arguments in Iterative Rounds with Moderation

Debate Agents. PRO and CON agents argue over a topic for multiple rounds, then a moderator summarizes. ## Exploring Both Sides of an Issue Systematically

Asking an LLM for pros and cons in a single call produces a superficial list. A structured debate produces deeper analysis: the pro agent makes a strong argument, the con agent directly counters it, and the pro agent responds to the counter. each round sharpening the analysis. After three rounds, the moderator has six substantive arguments to synthesize into a balanced summary.

The debate loop requires careful state management: each agent needs access to all prior arguments (not just the last one), the round counter must track progress, and the moderator must have the complete debate transcript. Without orchestration, managing the accumulating debate history across loop iterations, handling a timeout mid-debate without losing prior rounds, and generating the final synthesis from all arguments is complex state management code.

## The Solution

**You write the argumentation and moderation logic. Conductor handles the debate loop, transcript accumulation, and round management.**

`SetTopicWorker` establishes the debate topic and the positions each agent will argue. A `DO_WHILE` loop iterates through debate rounds: `AgentProWorker` presents arguments supporting the position (with rebuttals to prior con arguments), and `AgentConWorker` presents counterarguments (addressing the pro agent's latest points). After the configured number of rounds, `ModeratorSummarizeWorker` analyzes the complete debate transcript, identifies the strongest arguments from each side, and produces a balanced summary with a verdict. Conductor manages the accumulating transcript across rounds and records each argument for analysis.

### What You Write: Workers

Four workers stage the debate. Setting the topic, then iterating PRO and CON arguments through multiple rounds before the moderator synthesizes a verdict.

| Worker | Task | What It Does |
|---|---|---|
| **AgentConWorker** | `da_agent_con` | CON agent. argues against the debate topic each round. Uses deterministic argument selection based on the round input. |
| **AgentProWorker** | `da_agent_pro` | PRO agent. argues in favor of the debate topic each round. Uses deterministic argument selection based on the round .. |
| **ModeratorSummarizeWorker** | `da_moderator_summarize` | Moderator agent. summarizes the debate and delivers a verdict after all rounds. |
| **SetTopicWorker** | `da_set_topic` | Sets up the debate topic and defines the two sides (PRO and CON). |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
da_set_topic
 │
 ▼
DO_WHILE
 └── da_agent_pro
 └── da_agent_con
 │
 ▼
da_moderator_summarize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
