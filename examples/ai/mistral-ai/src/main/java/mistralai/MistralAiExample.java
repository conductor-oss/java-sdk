package mistralai;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import mistralai.workers.MistralChatWorker;
import mistralai.workers.MistralComposeRequestWorker;
import mistralai.workers.MistralExtractAnswerWorker;

import java.util.List;
import java.util.Map;

/**
 * Mistral AI Chat Completion — Conductor Workflow Example
 *
 * Demonstrates a three-step AI pipeline:
 * 1. Compose a Mistral chat request from document + question inputs
 * 2. Call the Mistral API (deterministic. for chat completion
 * 3. Extract the answer from the API response
 *
 * Run:
 *   java -jar target/mistral-ai-1.0.0.jar
 *   java -jar target/mistral-ai-1.0.0.jar --workers
 */
public class MistralAiExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Mistral AI Chat Completion Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mistral_compose_request",
                "mistral_chat",
                "mistral_extract_answer"));
        System.out.println("  Registered: mistral_compose_request, mistral_chat, mistral_extract_answer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'mistral_ai_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MistralComposeRequestWorker(),
                new MistralChatWorker(),
                new MistralExtractAnswerWorker());
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
        String workflowId = client.startWorkflow("mistral_ai_workflow", 1,
                Map.of(
                        "document", "SOFTWARE LICENSE AGREEMENT\n\nThis agreement is entered into between Licensor and Licensee...",
                        "question", "What are the key obligations and terms of this agreement?"
                ));
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
