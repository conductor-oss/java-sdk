package documentingestion;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import documentingestion.workers.IngestChunkTextWorker;
import documentingestion.workers.IngestEmbedChunksWorker;
import documentingestion.workers.IngestExtractPdfWorker;
import documentingestion.workers.IngestStoreVectorsWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 141: Document Ingestion Pipeline — PDF, Chunk, Embed, Store
 *
 * A 4-step ingestion pipeline that processes a document from raw PDF
 * through chunking, embedding, and vector store insertion.
 *
 * Run:
 *   java -jar target/document-ingestion-1.0.0.jar
 */
public class DocumentIngestionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 141: Document Ingestion Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ingest_extract_pdf", "ingest_chunk_text",
                "ingest_embed_chunks", "ingest_store_vectors"));
        System.out.println("  Registered: ingest_extract_pdf, ingest_chunk_text, ingest_embed_chunks, ingest_store_vectors\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'document_ingestion_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IngestExtractPdfWorker(),
                new IngestChunkTextWorker(),
                new IngestEmbedChunksWorker(),
                new IngestStoreVectorsWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("document_ingestion_workflow", 1,
                Map.of("documentUrl", "https://example.com/vector-databases-guide.pdf",
                        "collection", "knowledge_base",
                        "chunkSize", "30",
                        "chunkOverlap", "5"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n  Document: " + workflow.getOutput().get("documentUrl"));
        System.out.println("  Pages extracted: " + workflow.getOutput().get("pagesExtracted"));
        System.out.println("  Chunks created: " + workflow.getOutput().get("chunksCreated"));
        System.out.println("  Vectors stored: " + workflow.getOutput().get("vectorsStored"));

        System.out.println("\n--- Document Ingestion Pipeline ---");
        System.out.println("  1. Extract: Parse PDF/DOCX to raw text");
        System.out.println("  2. Chunk: Split into overlapping segments");
        System.out.println("  3. Embed: Convert each chunk to a vector");
        System.out.println("  4. Store: Upsert vectors into the database");

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
