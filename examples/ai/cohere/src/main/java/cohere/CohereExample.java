package cohere;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cohere.workers.CohereBuildPromptWorker;
import cohere.workers.CohereGenerateWorker;
import cohere.workers.CohereSelectBestWorker;

import java.util.List;
import java.util.Map;

/**
 * Cohere Text Generation — Conductor Workflow Example
 *
 * Demonstrates a 3-step workflow:
 * 1. cohere_build_prompt  — Builds a Cohere generate request
 * 2. cohere_generate      — Performs the Cohere API call
 * 3. cohere_select_best   — Selects the best generation by likelihood
 *
 * Run:
 *   java -jar target/cohere-1.0.0.jar
 *   java -jar target/cohere-1.0.0.jar --workers
 */
public class CohereExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Cohere Text Generation Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cohere_build_prompt",
                "cohere_generate",
                "cohere_select_best"));
        System.out.println("  Registered: cohere_build_prompt, cohere_generate, cohere_select_best\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'cohere_text_generation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CohereBuildPromptWorker(),
                new CohereGenerateWorker(),
                new CohereSelectBestWorker());
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("cohere_text_generation", 1,
                Map.of(
                        "productDescription", "SmartBoard Pro - an AI-powered project management tool that uses machine learning to predict project timelines, identify bottlenecks, and automatically assign tasks based on team members' skills and availability",
                        "targetAudience", "Engineering managers and tech leads at mid-size SaaS companies who are frustrated with traditional project management tools"));
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
