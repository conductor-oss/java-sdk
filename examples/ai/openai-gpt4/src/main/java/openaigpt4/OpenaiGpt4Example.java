package openaigpt4;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import openaigpt4.workers.Gpt4BuildRequestWorker;
import openaigpt4.workers.Gpt4CallApiWorker;
import openaigpt4.workers.Gpt4ExtractResultWorker;

import java.util.List;
import java.util.Map;

/**
 * OpenAI GPT-4 Integration — Conductor Workflow Example
 *
 * Demonstrates a three-step AI pipeline:
 * 1. Build an OpenAI chat completion request
 * 2. Call the GPT-4 API
 * 3. Extract the result from the API response
 *
 * Run:
 *   java -jar target/openai-gpt4-1.0.0.jar
 */
public class OpenaiGpt4Example {

    private static final long WAIT_TIMEOUT_MS = 30_000;

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        String configuredModel = Gpt4BuildRequestWorker.configuredDefaultModel();

        System.out.println("=== OpenAI GPT-4 Integration Workflow ===\n");

        var gpt4Worker = new Gpt4CallApiWorker();
        System.out.println("Mode: LIVE (CONDUCTOR_OPENAI_API_KEY detected)");
        System.out.println("Model: " + configuredModel + "\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("gpt4_build_request", "gpt4_call_api", "gpt4_extract_result"));
        System.out.println("  Registered: gpt4_build_request, gpt4_call_api, gpt4_extract_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'openai_gpt4_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new Gpt4BuildRequestWorker(),
                gpt4Worker,
                new Gpt4ExtractResultWorker()
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
        String workflowId = client.startWorkflow("openai_gpt4_workflow", 1,
                Map.of(
                        "prompt", "Analyze these Q3 revenue figures and provide key insights:\n\n"
                                + "| Region    | Q2 Revenue | Q3 Revenue | YoY Growth |\n"
                                + "|-----------|------------|------------|------------|\n"
                                + "| North America | $12.4M | $14.1M | +18% |\n"
                                + "| Europe    | $8.2M  | $7.9M  | -4%  |\n"
                                + "| APAC      | $5.1M  | $6.8M  | +33% |\n"
                                + "| LATAM     | $2.3M  | $2.5M  | +9%  |\n\n"
                                + "Total Q3: $31.3M (up from $28.0M in Q2). "
                                + "Enterprise deals accounted for 62% of new bookings. "
                                + "Churn rate increased from 4.2% to 5.1% in the SMB segment.",
                        "systemMessage", "You are a senior financial analyst. Be specific with numbers and actionable recommendations.",
                        "model", configuredModel
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", WAIT_TIMEOUT_MS);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("  Output: " + workflow.getOutput());
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
