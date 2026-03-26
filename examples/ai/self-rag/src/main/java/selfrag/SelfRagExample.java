package selfrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import selfrag.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 149: Self-RAG — Self-Reflective Retrieval with Grading
 *
 * Retrieve docs, grade relevance, generate answer, grade for hallucination
 * and usefulness. If grades fail, route to a refine-and-retry path.
 *
 * Pattern:
 *   retrieve -> grade_docs -> generate -> grade_hallucination -> grade_usefulness
 *     -> SWITCH: pass -> format_output | fail -> refine_retry
 *
 * Run:
 *   java -jar target/self-rag-1.0.0.jar
 *   java -jar target/self-rag-1.0.0.jar --workers
 */
public class SelfRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 149: Self-RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sr_retrieve", "sr_grade_docs", "sr_generate",
                "sr_grade_hallucination", "sr_grade_usefulness",
                "sr_format_output", "sr_refine_retry"));
        System.out.println("  Registered 7 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'self_rag'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RetrieveWorker(),
                new GradeDocsWorker(),
                new GenerateWorker(),
                new GradeHallucinationWorker(),
                new GradeUsefulnessWorker(),
                new FormatOutputWorker(),
                new RefineRetryWorker());
        client.startWorkers(workers);
        System.out.println("  7 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("self_rag", 1,
                Map.of("question", "What task types does Conductor support?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
