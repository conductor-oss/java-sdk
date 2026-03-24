# Reflection Agent in Java Using Conductor : Generate, Reflect, Improve in Iterative Refinement Loop

Reflection Agent. generates content on a topic, iteratively reflects and improves through a DO_WHILE loop, then produces final polished output.

## First Drafts Are Never Good Enough

An LLM's first response to "Write an essay about climate change solutions" is serviceable but rarely excellent. It might lack specific examples, have a weak conclusion, or miss an important perspective. A reflection agent catches these weaknesses: after generating the initial draft, a separate reflection step identifies specific issues ("Paragraph 3 lacks a concrete example", "The economic analysis is superficial"), and an improvement step addresses each issue.

Each iteration sharpens the output. the first reflection might catch structural problems, the second might catch factual gaps, the third might polish prose quality. The loop terminates when the reflection step finds no significant issues or a maximum iteration count is reached. Without orchestration, managing the evolving draft across iterations, tracking which issues were found and fixed, and implementing loop termination logic requires careful state management.

## The Solution

**You write the generation, reflection, and improvement logic. Conductor handles the refinement loop, quality threshold evaluation, and version tracking.**

`InitialGenerationWorker` produces the first draft of the content based on the topic. A `DO_WHILE` loop then iterates: `ReflectWorker` analyzes the current version and identifies specific weaknesses with severity ratings and suggested improvements. `ImproveWorker` addresses each identified weakness, producing an improved version with change annotations. After the loop exits (quality threshold met or max iterations reached), `FinalOutputWorker` delivers the polished content with a summary of all reflections and improvements made. Conductor tracks each iteration's reflection and improvement for quality analysis.

### What You Write: Workers

Four workers power iterative refinement. Generating an initial draft, reflecting on weaknesses, improving based on feedback, and delivering the polished output.

| Worker | Task | What It Does |
|---|---|---|
| **FinalOutputWorker** | `rn_final_output` | Produces the final polished output after all reflection iterations. Combines the improved drafts into a high-quality ... |
| **ImproveWorker** | `rn_improve` | Incorporates reflection feedback to improve the draft. Returns the revised content and a flag indicating the feedback... |
| **InitialGenerationWorker** | `rn_initial_generation` | Generates an initial draft on a given topic. Produces a shallow first pass with a low quality score, setting the stag... |
| **ReflectWorker** | `rn_reflect` | Reflects on the current draft and provides constructive feedback along with a quality score. The feedback varies by i... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
rn_initial_generation
 │
 ▼
DO_WHILE
 └── rn_reflect
 └── rn_improve
 │
 ▼
rn_final_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
