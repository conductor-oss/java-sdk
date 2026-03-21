# Tool-Augmented Generation in Java Using Conductor : Generate, Detect Gaps, Call Tools, Incorporate, Complete

Tool-Augmented Generation. detect knowledge gaps during text generation, invoke external tools to fill them, and produce enriched output. ## LLMs Need Tools When Their Knowledge Runs Out

An LLM generating a response about current stock prices will hallucinate numbers because its training data is months old. Tool-augmented generation detects these knowledge gaps mid-generation and pauses to call external tools. a stock API for prices, a calculator for computations, a database for customer data, then resumes generation with real data incorporated.

This five-step pipeline separates concern: start the generation, detect where the model would hallucinate (gap detection), call the right tool to fill the gap, splice the tool result back into the generation context, and complete the response with factual data. Without this separation, the LLM confidently generates wrong numbers, and there's no record of which facts were grounded in tool calls versus model knowledge.

## The Solution

**You write the generation, gap detection, tool invocation, and incorporation logic. Conductor handles the enrichment pipeline, tool retries, and provenance tracking.**

`StartGenerationWorker` begins the LLM generation and produces an initial partial response. `DetectGapWorker` analyzes the partial response for knowledge gaps. places where the model needs real-time data (prices, dates, statistics) it doesn't have. `CallToolWorker` invokes the appropriate tool based on the gap type and returns factual data. `IncorporateResultWorker` merges the tool's output back into the generation context. `CompleteGenerationWorker` finishes the response with the tool data incorporated. Conductor chains these five steps and records which tools were called and what data they provided.

### What You Write: Workers

Five workers enrich generation with real data. Starting the draft, detecting knowledge gaps, calling external tools, incorporating the results, and completing the response.

| Worker | Task | What It Does |
|---|---|---|
| **CallToolWorker** | `tg_call_tool` | Invokes the external tool identified by the gap detector and returns the factual result along with its source. |
| **CompleteGenerationWorker** | `tg_complete_generation` | Finalises text generation by appending remaining content to the enriched text and reporting total token count. |
| **DetectGapWorker** | `tg_detect_gap` | Analyses partial text to detect the knowledge gap and determines which external tool should be called and what query ... |
| **IncorporateResultWorker** | `tg_incorporate_result` | Merges the tool result into the partial text, producing enriched text that fills the previously detected knowledge gap. |
| **StartGenerationWorker** | `tg_start_generation` | Begins text generation from a prompt and produces partial text with a knowledge gap that requires an external tool to... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tg_start_generation
 │
 ▼
tg_detect_gap
 │
 ▼
tg_call_tool
 │
 ▼
tg_incorporate_result
 │
 ▼
tg_complete_generation

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
