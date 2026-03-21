# Three-Agent Pipeline in Java Using Conductor : Researcher, Writer, Reviewer

Three-Agent Pipeline. Researcher + Writer + Reviewer with final output assembly. ## Quality Content Needs Research, Writing, and Review

A single LLM call to "write an article about quantum computing" produces unreferenced, unreviewed content. The three-agent pipeline mirrors a real editorial process: the researcher finds authoritative sources and extracts key facts, the writer uses those facts to craft a narrative for the target audience, and the reviewer evaluates accuracy, clarity, and completeness. flagging issues before publication.

Each agent adds value that the others can't. The researcher focuses on finding and validating information without worrying about prose quality. The writer focuses on narrative and engagement without having to verify facts. The reviewer brings a fresh perspective, catching issues that the writer is too close to see. Separating these roles produces higher-quality output than any single agent attempting all three tasks.

## The Solution

**You write the research, writing, and review logic. Conductor handles the editorial pipeline, draft versioning, and quality scoring.**

`ResearcherAgentWorker` investigates the topic and produces structured research with key facts, sources, statistics, and talking points for the target audience. `WriterAgentWorker` crafts the content using the researcher's material, structuring it for the specified audience with appropriate tone and depth. `ReviewerAgentWorker` evaluates the draft against quality criteria. factual accuracy, clarity, completeness, audience fit, and provides an overall score with specific feedback. `FinalOutputWorker` packages the reviewed content with the review score and any reviewer notes. Conductor chains these four steps and records each agent's output for editorial quality tracking.

### What You Write: Workers

Four workers form the editorial pipeline, the researcher gathers facts, the writer crafts the narrative, the reviewer scores quality, and the final output is assembled.

| Worker | Task | What It Does |
|---|---|---|
| **FinalOutputWorker** | `thr_final_output` | Final output worker. assembles the complete report from all agent outputs. |
| **ResearcherAgentWorker** | `thr_researcher_agent` | Researcher agent. gathers key facts, statistics, and sources on a subject. |
| **ReviewerAgentWorker** | `thr_reviewer_agent` | Reviewer agent. evaluates the draft for accuracy, clarity, and audience fit. |
| **WriterAgentWorker** | `thr_writer_agent` | Writer agent. produces a draft article incorporating research facts for the target audience. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
thr_researcher_agent
 │
 ▼
thr_writer_agent
 │
 ▼
thr_reviewer_agent
 │
 ▼
thr_final_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
