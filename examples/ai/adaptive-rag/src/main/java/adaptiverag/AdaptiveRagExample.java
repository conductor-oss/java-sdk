package adaptiverag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import adaptiverag.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 150: Adaptive RAG — Route Queries to Different Strategies
 *
 * Classify query type (factual, analytical, creative) and route to the
 * appropriate strategy: simple RAG, multi-hop RAG with reasoning chain,
 * or creative generation (no retrieval).
 *
 * Pattern:
 *   classify -> SWITCH
 *     |-> "factual"    -> simple_retrieve -> simple_generate
 *     |-> "analytical" -> multihop_retrieve -> reasoning -> analytical_generate
 *     |-> default      -> creative_generate
 *
 * Run:
 *   java -jar target/adaptive-rag-1.0.0.jar
 *   java -jar target/adaptive-rag-1.0.0.jar --workers
 */
public class AdaptiveRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 150: Adaptive RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ar_classify", "ar_simple_ret", "ar_simple_gen",
                "ar_mhop_ret", "ar_reason", "ar_anal_gen", "ar_creative_gen"));
        System.out.println("  Registered 7 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'adaptive_rag'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyWorker(),
                new SimpleRetrieveWorker(),
                new SimpleGenerateWorker(),
                new MultiHopRetrieveWorker(),
                new ReasoningWorker(),
                new AnalyticalGenerateWorker(),
                new CreativeGenerateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  7 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start workflow with analytical query
        System.out.println("--- Test: Analytical Query ---");
        String workflowId = client.startWorkflow("adaptive_rag", 1,
                Map.of("question", "How does Conductor compare to Temporal for workflow orchestration?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Question: " + workflow.getOutput().get("question"));
        System.out.println("  Classified as: " + workflow.getOutput().get("queryType")
                + " (confidence: " + workflow.getOutput().get("confidence") + ")");

        // Find the answer task
        var answerTask = workflow.getTasks().stream()
                .filter(t -> List.of("sgen_ref", "agen_ref", "cgen_ref")
                        .contains(t.getReferenceTaskName()))
                .findFirst();
        if (answerTask.isPresent()) {
            System.out.println("  Strategy: " + answerTask.get().getOutputData().get("strategy"));
            System.out.println("  Answer: " + answerTask.get().getOutputData().get("answer"));
        }

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
