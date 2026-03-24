# Knowledge Graph RAG: Graph Traversal + Vector Search in Parallel

Vector search alone misses relationships. This pipeline extracts entities (Conductor/Software, Netflix/Organization), runs graph traversal and vector search in parallel, merges structured triples (`{subject: "Conductor", predicate: "developed_by", object: "Netflix", confidence: 0.99}`) with unstructured documents, and generates an enriched answer.

## Workflow

```
question
    │
    ▼
┌────────────────────────┐
│ kg_extract_entities    │  NER: entities + types
└───────────┬────────────┘
            ▼
┌─── FORK_JOIN ──────────────────────────────────┐
│ ┌───────────────────────┐ ┌───────────────────┐│
│ │kg_graph_traverse      │ │kg_vector_search   ││
│ │(relationship triples) │ │(similarity search)││
│ └───────────────────────┘ └───────────────────┘│
└──────────────────────┬─────────────────────────┘
                       ▼
            ┌──────────────────┐
            │ kg_merge_context │  Combine facts + docs
            └────────┬─────────┘
                     ▼
            ┌──────────────────┐
            │ kg_generate      │  Generate from enriched context
            └──────────────────┘
```

## Workers

**ExtractEntitiesWorker** (`kg_extract_entities`) -- Returns entities: `{name: "Conductor", type: "Software"}`, `{name: "Netflix", type: "Organization"}`.

**GraphTraverseWorker** (`kg_graph_traverse`) -- Returns triples: `developed_by` (0.99 confidence), `is_a` ("workflow orchestration engine", 0.98).

**VectorSearchWorker** (`kg_vector_search`) -- Returns 4 documents with entity hints and scores.

**MergeContextWorker** (`kg_merge_context`) -- Combines graph facts, relations, and vector docs. Handles null lists.

**GenerateWorker** (`kg_generate`) -- Calls `gpt-4o-mini` with the merged context.

## Tests

34 tests cover entity extraction, graph traversal, vector search, context merging, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
