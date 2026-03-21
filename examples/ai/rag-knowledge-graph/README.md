# Knowledge Graph RAG in Java Using Conductor : Graph Traversal + Vector Search for Enriched Context

## When Vector Search Alone Misses Relationships

Vector search finds semantically similar text, but it doesn't understand relationships. A question like "Who are the competitors of Company X's parent company?" requires traversing entity relationships (Company X -> parent company -> competitors) that aren't captured in embedding similarity. Knowledge graph RAG extracts entities from the question, traverses the graph to find related entities and facts, and combines those structured results with vector search results for richer context.

The graph traversal and vector search are independent. they can run in parallel, and their results are complementary (structured facts + relevant passages). After both complete, a merge step combines them into a unified context for generation.

## The Solution

**You write the entity extraction, graph traversal, and vector search logic. Conductor handles the parallel retrieval, retries, and observability.**

Entity extraction runs first. Then Conductor's `FORK_JOIN` runs graph traversal and vector search in parallel. A merge worker combines structured graph results with unstructured vector results, and a generation worker produces the answer from the enriched context.

### What You Write: Workers

Five workers combine graph and vector retrieval. extracting entities from the question, traversing the knowledge graph and searching the vector store in parallel via FORK_JOIN, merging both context types, and generating an answer from the unified graph-plus-vector context.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractEntitiesWorker** | `kg_extract_entities` | Worker that extracts named entities from the user's question. Returns 4 fixed entities with name, type, and id fields... |
| **GenerateWorker** | `kg_generate` | Worker that generates a final answer using the question, merged context from both graph and vector retrieval, and ext... |
| **GraphTraverseWorker** | `kg_graph_traverse` | Worker that traverses a knowledge graph starting from extracted entities. Returns 7 facts (subject/predicate/object/c... |
| **MergeContextWorker** | `kg_merge_context` | Worker that merges context from graph traversal and vector search. Combines graphFacts, graphRelations, and vectorDoc... |
| **VectorSearchWorker** | `kg_vector_search` | Worker that performs vector similarity search using the question and entity hints. Returns 4 documents with id, text,... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
kg_extract_entities
 │
 ▼
FORK_JOIN
 ├── kg_graph_traverse
 └── kg_vector_search
 │
 ▼
JOIN (wait for all branches)
kg_merge_context
 │
 ▼
kg_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
