package raptorrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import raptorrag.workers.ChunkDocsWorker;
import raptorrag.workers.LeafSummariesWorker;
import raptorrag.workers.ClusterSummariesWorker;
import raptorrag.workers.TreeSearchWorker;
import raptorrag.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 152: RAPTOR RAG — Recursive Abstractive Processing for Tree-Organized Retrieval
 *
 * Builds a hierarchical summary tree from document chunks, then
 * searches at multiple levels to retrieve context for generation.
 * Workers perform chunking, summarization, tree search, and answer generation.
 *
 * Run:
 *   java -jar target/raptor-rag-1.0.0.jar
 *   java -jar target/raptor-rag-1.0.0.jar --workers
 */
public class RaptorRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 152: RAPTOR RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rp_chunk_docs", "rp_leaf_summaries",
                "rp_cluster_summaries", "rp_tree_search", "rp_generate"));
        System.out.println("  Registered: rp_chunk_docs, rp_leaf_summaries, rp_cluster_summaries, rp_tree_search, rp_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'raptor_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ChunkDocsWorker(),
                new LeafSummariesWorker(),
                new ClusterSummariesWorker(),
                new TreeSearchWorker(),
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

        // Step 4 — Start workflow
        System.out.println("Step 4: Starting RAPTOR RAG workflow...\n");
        String workflowId = client.startWorkflow("raptor_rag_workflow", 1,
                Map.of("documentText", "Conductor is an open-source orchestration platform for building "
                                + "distributed applications. It supports workflow versioning, polyglot workers, "
                                + "retries, rate limiting, and event-driven automation.",
                        "question", "What features does Conductor offer for building distributed applications?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer") + "\n");

        System.out.println("--- RAPTOR RAG Pattern ---");
        System.out.println("  - Chunk docs: Split document into manageable segments");
        System.out.println("  - Leaf summaries: Summarize chunks at the leaf level (level 0)");
        System.out.println("  - Cluster summaries: Build hierarchical clusters (level 1) and root (level 2)");
        System.out.println("  - Tree search: Traverse tree top-down for multi-level context");
        System.out.println("  - Generate: Produce answer using context from all tree levels");

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
