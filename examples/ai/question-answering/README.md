# Parse, Retrieve, and Answer Questions from a Knowledge Base

A user asks "How do Conductor workflow timeouts work?" The system must parse the question to detect its type and extract keywords, retrieve relevant context from the knowledge base, and generate a grounded answer with source citations and relevance scores.

## Workflow

```
question, knowledgeBase
       │
       ▼
┌───────────────────────┐
│ qas_parse_question    │  Detect type, extract keywords
└───────────┬───────────┘
            │  parsed {original, type, keywords}
            ▼
┌───────────────────────┐
│ qas_retrieve_context  │  Find relevant passages
└───────────┬───────────┘
            │  context, sources, relevanceScores
            ▼
┌───────────────────────┐
│ qas_generate_answer   │  Synthesize answer
└───────────────────────┘
            │
            ▼
      answer, sources, confidence
```

## Workers

**ParseQuestionWorker** (`qas_parse_question`) -- Outputs a `parsed` map with `original` (the question), `type` (detected question type), and `keywords: ["Conductor", "workflow", "timeout"]`.

**RetrieveContextWorker** (`qas_retrieve_context`) -- Returns 3 context passages about Conductor timeouts: `"Conductor workflows support configurable timeouts."`, `"The timeoutSeconds parameter defines maximum execution time."`, `"If exceeded, workflow transitions to TIMED_OUT."`. Provides `sources: ["docs/timeouts.md", "docs/workflow-config.md"]` and `relevanceScores: [0.95, 0.88, 0.82]`.

**GenerateAnswerWorker** (`qas_generate_answer`) -- Synthesizes an answer from the retrieved context and question, producing the final response.

## Tests

6 tests cover question parsing, context retrieval, and answer generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
