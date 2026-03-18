package googlegemini;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import googlegemini.workers.GeminiFormatOutputWorker;
import googlegemini.workers.GeminiGenerateWorker;
import googlegemini.workers.GeminiPrepareContentWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 114: Orchestrating Google Gemini Calls
 *
 * Demonstrates how to orchestrate Google Gemini API calls through Conductor.
 * The GeminiGenerateWorker calls the real Gemini REST API.
 * Requires GOOGLE_API_KEY to be set.
 *
 * Run:
 *   java -jar target/google-gemini-1.0.0.jar
 */
public class GoogleGeminiExample {

    private static final long WAIT_TIMEOUT_MS = 30_000;

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        String configuredModel = GeminiGenerateWorker.configuredDefaultModel();

        System.out.println("=== Example 114: Orchestrating Google Gemini ===\n");

        GeminiGenerateWorker generateWorker = new GeminiGenerateWorker();
        System.out.println("Mode: LIVE (GOOGLE_API_KEY detected)");
        System.out.println("Model: " + configuredModel + "\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gemini_prepare_content", "gemini_generate", "gemini_format_output"));
        System.out.println("  Registered: gemini_prepare_content, gemini_generate, gemini_format_output\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'google_gemini_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GeminiPrepareContentWorker(),
                generateWorker,
                new GeminiFormatOutputWorker());
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("google_gemini_workflow", 1,
                Map.of("prompt", "Create a product launch plan for our new analytics dashboard.",
                        "context", "B2B SaaS company, 10K existing users, targeting enterprise segment.",
                        "model", configuredModel));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", WAIT_TIMEOUT_MS);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("  Model: " + workflow.getOutput().get("model"));
            String resultText = (String) workflow.getOutput().get("result");
            if (resultText != null) {
                System.out.println("  Result: " + resultText.substring(0, Math.min(80, resultText.length())) + "...");
            }
            System.out.println("  Tokens: " + workflow.getOutput().get("tokens"));
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            printFailureDetails(workflow);
            System.exit(1);
        }
    }

    private static void printFailureDetails(Workflow workflow) {
        if (workflow.getReasonForIncompletion() != null && !workflow.getReasonForIncompletion().isBlank()) {
            System.out.println("  Workflow reason: " + workflow.getReasonForIncompletion());
        }

        if (workflow.getTasks() == null || workflow.getTasks().isEmpty()) {
            System.out.println("  Workflow did not finish within " + WAIT_TIMEOUT_MS + " ms.");
            return;
        }

        workflow.getTasks().stream()
                .filter(t -> t.getStatus() != null
                        && (t.getStatus().name().equals("FAILED")
                            || t.getStatus().name().equals("FAILED_WITH_TERMINAL_ERROR")))
                .findFirst()
                .ifPresentOrElse(
                        t -> System.out.println("  Failed task: " + t.getTaskDefName()
                                + "\n  Reason: " + t.getReasonForIncompletion()),
                        () -> System.out.println("  Workflow did not finish within "
                                + WAIT_TIMEOUT_MS + " ms. Last known task states: "
                                + workflow.getTasks().stream()
                                .map(t -> t.getTaskDefName() + "=" + t.getStatus())
                                .toList())
                );
    }
}
