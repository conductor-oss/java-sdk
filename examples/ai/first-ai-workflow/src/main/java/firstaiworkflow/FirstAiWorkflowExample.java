package firstaiworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import firstaiworkflow.workers.AiPreparePromptWorker;
import firstaiworkflow.workers.AiCallLlmWorker;
import firstaiworkflow.workers.AiParseResponseWorker;

import java.util.List;
import java.util.Map;

/**
 * First AI Workflow — Chain of AI Workers
 *
 * Demonstrates a three-step AI pipeline:
 * 1. Prepare a prompt from user input
 * 2. Call an LLM
 * 3. Parse and validate the response
 *
 * Run:
 *   java -jar target/first-ai-workflow-1.0.0.jar
 */
public class FirstAiWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== First AI Workflow: Chain of AI Workers ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ai_prepare_prompt", "ai_call_llm", "ai_parse_response"));
        System.out.println("  Registered: ai_prepare_prompt, ai_call_llm, ai_parse_response\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'first_ai_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AiPreparePromptWorker(),
                new AiCallLlmWorker(),
                new AiParseResponseWorker()
        );
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
        String workflowId = client.startWorkflow("first_ai_workflow", 1,
                Map.of("question", "What is Orkes Conductor?", "model", "gpt-4"));
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
