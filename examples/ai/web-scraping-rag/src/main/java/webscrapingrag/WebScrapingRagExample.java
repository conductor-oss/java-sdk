package webscrapingrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webscrapingrag.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Web Scraping to RAG Pipeline
 *
 * Scrape web pages, chunk their content, embed and store vectors,
 * then run a RAG query against the scraped data. Demonstrates
 * a full ingest-then-query pipeline in a single workflow.
 *
 * Run:
 *   java -jar target/web-scraping-rag-1.0.0.jar
 */
public class WebScrapingRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 142: Web Scraping to RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wsrag_scrape", "wsrag_chunk", "wsrag_embed_store", "wsrag_query", "wsrag_generate"));
        System.out.println("  Registered 5 tasks.\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'web_scraping_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ScrapeWorker(),
                new ChunkWorker(),
                new EmbedStoreWorker(),
                new QueryWorker(),
                new GenerateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("web_scraping_rag_workflow", 1,
                Map.of(
                        "urls", List.of("https://docs.example.com/overview", "https://docs.example.com/workers"),
                        "question", "How do Conductor workers function?"
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));
        System.out.println("  Pages scraped: " + workflow.getOutput().get("pagesScraped"));
        System.out.println("  Chunks stored: " + workflow.getOutput().get("chunksStored"));

        System.out.println("\n--- Web Scraping + RAG Pipeline ---");
        System.out.println("  1. Scrape: Fetch and parse web pages");
        System.out.println("  2. Chunk: Split content into segments");
        System.out.println("  3. Embed + Store: Vectorize and persist");
        System.out.println("  4. Query + Generate: RAG over scraped data");

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
