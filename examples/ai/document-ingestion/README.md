# PDF-to-Vector-Store Ingestion Pipeline

Someone drops a batch of PDFs that need to be searchable by Monday. Before RAG can answer questions, documents must be extracted, chunked, embedded, and stored. If the embedding API rate-limits you at chunk 47 of 200, you need to retry from that exact point -- not re-extract a 200-page PDF. This pipeline handles it in four stages.

## Workflow

```
documentUrl, collection, chunkSize, chunkOverlap
       │
       ▼
┌───────────────────┐
│ ingest_extract_pdf│  Parse PDF/text/URL via PDFBox
└────────┬──────────┘
         │  text, pageCount, charCount
         ▼
┌───────────────────┐
│ ingest_chunk_text │  Word-based sliding window
└────────┬──────────┘
         │  chunks[], chunkCount
         ▼
┌────────────────────┐
│ ingest_embed_chunks│  OpenAI text-embedding-3-small
└────────┬───────────┘
         │  vectors[], model
         ▼
┌─────────────────────┐
│ ingest_store_vectors│  Write to JSON file on disk
└─────────────────────┘
         │
         ▼
   pagesExtracted, chunksCreated, vectorsStored
```

## Workers

**IngestExtractPdfWorker** (`ingest_extract_pdf`) -- Handles three input formats: local PDF files (parsed with Apache PDFBox via `Loader.loadPDF()` and `PDFTextStripper`), local text files (read with `Files.readString()`), and HTTP/HTTPS URLs (fetched with `HttpClient`, 15-second connect timeout, following redirects). For non-PDF text, estimates page count as `Math.max(1, text.length() / 2000)`. Validates that `documentUrl` is not null/blank, returning `FAILED` if missing.

**IngestChunkTextWorker** (`ingest_chunk_text`) -- Splits text on `\\s+` into words and creates overlapping chunks using a sliding window. Defaults: `chunkSize=200` words, `chunkOverlap=50` words. Each chunk gets an id like `"chunk-0"`, the text content, `wordCount`, and `startOffset`. The window advances by `chunkSize - chunkOverlap` (150 words by default). Both parameters are configurable from workflow input via `Integer.parseInt()`.

**IngestEmbedChunksWorker** (`ingest_embed_chunks`) -- Requires `CONDUCTOR_OPENAI_API_KEY`. Iterates chunks and calls OpenAI's Embeddings API at `https://api.openai.com/v1/embeddings` with model `text-embedding-3-small` for each chunk. Builds a vector entry with the chunk's `id`, the embedding array (from `data[0].embedding`), and metadata (first 100 chars of text + wordCount). Distinguishes retryable errors (429/503) from terminal errors (other 4xx).

**IngestStoreVectorsWorker** (`ingest_store_vectors`) -- Writes vectors to `<collection>_vectors.json` in the system temp directory (`java.io.tmpdir`). The file name is sanitized via `replaceAll("[^a-zA-Z0-9_-]", "_")`. Uses Jackson `ObjectMapper` with `INDENT_OUTPUT` enabled. Output includes `collection`, `vectorCount`, and the full `vectors` array. The output directory is overridable via a constructor parameter for testing.

## Tests

22 tests across 4 test files cover PDF extraction, chunking with different sizes/overlaps, embedding API calls, and file-based vector storage.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
