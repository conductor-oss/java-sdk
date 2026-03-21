# Question Answering in Java with Conductor : Parse, Retrieve, and Generate Answers from a Knowledge Base

## Answering Questions with Knowledge-Grounded Responses

An effective question answering system does not just search for keywords. it understands what the user is asking, finds the most relevant information from a knowledge base, and synthesizes a clear, accurate answer. This requires a pipeline: parse the question to extract its intent and key terms, retrieve the most relevant passages from the knowledge base, and generate an answer that directly addresses the question using the retrieved context.

This workflow implements that three-step pipeline. The question parser analyzes the input to extract intent (e.g., "how-to" vs. "definition" vs. "comparison") and key terms. The context retriever searches the specified knowledge base using the parsed question and returns the most relevant passages. The answer generator synthesizes a natural language response from the retrieved context, directly answering the user's question.

## The Solution

**You just write the question-parsing, context-retrieval, and answer-generation workers. Conductor handles the QA pipeline and passage routing.**

Three workers form the QA pipeline. question parsing, context retrieval, and answer generation. The parser extracts intent and keywords from the question. The retriever searches the knowledge base for relevant passages. The generator produces a natural language answer from the retrieved context. Conductor sequences the three steps and passes parsed questions, retrieved context, and generated answers between them via JSONPath.

### What You Write: Workers

ParseQuestionWorker extracts intent and keywords, RetrieveContextWorker finds the most relevant passages from the knowledge base, and GenerateAnswerWorker synthesizes a natural language response with a confidence score.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateAnswerWorker** | `qas_generate_answer` | Synthesizes a natural-language answer from retrieved context passages, with a confidence score. |
| **ParseQuestionWorker** | `qas_parse_question` | Analyzes the input question to extract intent type (how-to, definition, comparison) and keywords. |
| **RetrieveContextWorker** | `qas_retrieve_context` | Searches the knowledge base for the most relevant passages matching the parsed question. |

### The Workflow

```
qas_parse_question
 │
 ▼
qas_retrieve_context
 │
 ▼
qas_generate_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
