# Language-Aware Code Search and Answer Generation

Code search is not text search -- you need AST-aware queries that understand function declarations, method definitions, and call expressions. This pipeline parses the question with language context, embeds it with code-specific filters, searches a code index for matching snippets with file paths and line numbers, and generates an answer.

## Workflow

```
question, language
       │
       ▼
┌───────────────────────┐
│ cr_parse_query        │  Extract keywords + language
└───────────┬───────────┘
            ▼
┌───────────────────────┐
│ cr_embed_code_query   │  Embed with AST node type filters
└───────────┬───────────┘
            ▼
┌───────────────────────┐
│ cr_search_code_index  │  Search code index for snippets
└───────────┬───────────┘
            ▼
┌───────────────────────┐
│ cr_generate_code_answer│  Generate answer with code context
└───────────────────────┘
```

## Workers

**ParseQueryWorker** (`cr_parse_query`) -- Extracts keywords `["function", "usage", "example", language]` from the question and the target programming language.

**EmbedCodeQueryWorker** (`cr_embed_code_query`) -- Generates an embedding with a `codeFilter` specifying `nodeTypes: ["function_declaration", "method_definition", "call_expression"]` to target AST-relevant code structures.

**SearchCodeIndexWorker** (`cr_search_code_index`) -- Returns 3 code snippets, each with `id`, `file`, `line`, `signature`, `body`, `astType`, and `score` fields.

**GenerateCodeAnswerWorker** (`cr_generate_code_answer`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls the LLM with a system prompt and the retrieved code context. Otherwise returns a deterministic answer.

## Tests

24 tests cover query parsing, code-specific embedding, AST-filtered search, and code answer generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
