# RAG with LangChain in Java Using Conductor : Load, Split, Embed, Retrieve, Generate

## The Full LangChain Pipeline as a Workflow

LangChain defines the canonical RAG stages: load documents from a source, split them into chunks, embed the chunks, retrieve relevant ones for a query, and generate an answer. In a LangChain application, these stages are chained in code. As a Conductor workflow, each stage becomes an independently retryable, observable, and replaceable worker.

If the embedding API rate-limits you during chunk embedding, Conductor retries that stage without re-loading and re-splitting the documents. If you want to swap the text splitter (from character-based to sentence-based), you replace one worker without touching the rest of the pipeline.

## The Solution

**You write each LangChain stage as a worker. Conductor handles the five-step pipeline, retries, and observability.**

Each LangChain stage is an independent worker. document loading, text splitting, chunk embedding, retrieval, and generation. Conductor sequences them, retries any stage that fails, and tracks every execution from raw document through final answer.

### What You Write: Workers

Five workers mirror the LangChain document processing pattern. loading documents from a source, splitting text into chunks, embedding the chunks, retrieving relevant passages, and generating an answer from the retrieved context.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedChunksWorker** | `lc_embed_chunks` | Worker that generates embeddings for text chunks. Produces deterministic 4-dimensional vectors based on chunk content... |
| **GenerateWorker** | `lc_generate` | Worker that generates an answer using the RetrievalQA chain. Combines the question with retrieved documents to produc... |
| **LoadDocumentsWorker** | `lc_load_documents` | Worker that loads documents from a source URL using WebBaseLoader. Returns a list of documents with pageContent and m... |
| **RetrieveWorker** | `lc_retrieve` | Worker that retrieves the most relevant documents for a question using FAISS. Assigns decreasing similarity scores (0... |
| **SplitTextWorker** | `lc_split_text` | Worker that splits documents into chunks using sentence-based splitting. Simulates RecursiveCharacterTextSplitter fro... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
lc_load_documents
 │
 ▼
lc_split_text
 │
 ▼
lc_embed_chunks
 │
 ▼
lc_retrieve
 │
 ▼
lc_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
