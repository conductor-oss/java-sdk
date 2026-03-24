# Two-Agent Pipeline in Java Using Conductor : Writer Agent to Editor Agent

Sequential writer-editor pipeline: writer agent drafts content, editor agent refines it, final output assembles the result.

## Two Agents Are Better Than One

A single LLM call to "write a product description" produces serviceable but unpolished output. Adding a second pass. where a different agent reviews and improves the first agent's output, consistently produces better results. The writer focuses on content and structure. The editor focuses on clarity, conciseness, and polish. Two specialized agents with a single handoff.

This is the most common multi-agent pattern: agent A produces output, agent B improves it. It works for content (writer + editor), code (generator + reviewer), analysis (analyst + critic), and many other domains. Separating generation from refinement means you can swap either agent independently, compare different writer/editor combinations, and see exactly what the editor changed.

## The Solution

**You write the content generation and editorial refinement logic. Conductor handles the handoff, version tracking, and quality comparison between drafts.**

`WriterAgentWorker` generates the initial content. a product description, blog post, or marketing copy, based on the input parameters. `EditorAgentWorker` reviews the writer's output and improves it, fixing grammar, improving clarity, tightening prose, and ensuring the content meets style guidelines. `FinalOutputWorker` packages the edited content with metadata (word count, readability score, edit summary). Conductor chains the three steps and records both the original and edited versions for quality comparison.

### What You Write: Workers

Three workers form the simplest multi-agent pattern, the writer drafts content, the editor refines it, and the final output assembles the result with metadata.

| Worker | Task | What It Does |
|---|---|---|
| **EditorAgentWorker** | `tap_editor_agent` | Editor agent that reviews and improves a draft. Takes draft, tone, and systemPrompt as inputs and returns editedConte... |
| **FinalOutputWorker** | `tap_final_output` | Final output worker that assembles the pipeline result. Takes originalDraft, editedContent, and editorNotes as inputs... |
| **WriterAgentWorker** | `tap_writer_agent` | Writer agent that produces a draft about a given topic. Takes topic, tone, and systemPrompt as inputs and returns a d... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tap_writer_agent
 │
 ▼
tap_editor_agent
 │
 ▼
tap_final_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
