package anthropicclaude;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import anthropicclaude.workers.ClaudeBuildMessagesWorker;
import anthropicclaude.workers.ClaudeCallApiWorker;
import anthropicclaude.workers.ClaudeProcessResponseWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 113: Orchestrating Anthropic Claude Calls
 *
 * Demonstrates how to orchestrate Anthropic Claude API calls through Conductor.
 * The worker calls Claude's Messages API with its distinct format: separate
 * system parameter, content blocks with type/text structure, and stop_reason.
 *
 * Run:
 *   java -jar target/anthropic-claude-1.0.0.jar
 */
public class AnthropicClaudeExample {

    private static final long WAIT_TIMEOUT_MS = 30_000;

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        String configuredModel = ClaudeCallApiWorker.configuredDefaultModel();

        System.out.println("=== Example 113: Orchestrating Anthropic Claude ===\n");

        var claudeWorker = new ClaudeCallApiWorker();
        System.out.println("Mode: LIVE (CONDUCTOR_ANTHROPIC_API_KEY detected)");
        System.out.println("Model: " + configuredModel + "\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("claude_build_messages", "claude_call_api", "claude_process_response"));
        System.out.println("  Registered: claude_build_messages, claude_call_api, claude_process_response\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'anthropic_claude_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClaudeBuildMessagesWorker(),
                claudeWorker,
                new ClaudeProcessResponseWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("anthropic_claude_workflow", 1,
                Map.of(
                        "userMessage", "Perform a security audit of our authentication module and list vulnerabilities by severity.",
                        "systemPrompt", "You are a senior application security engineer. Provide structured audit findings with severity levels and remediation steps.",
                        "model", configuredModel
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", WAIT_TIMEOUT_MS);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("  Model: " + workflow.getOutput().get("model"));
            System.out.println("  Stop reason: " + workflow.getOutput().get("stopReason"));
            String analysis = (String) workflow.getOutput().get("analysis");
            if (analysis != null && analysis.length() > 80) {
                System.out.println("  Analysis: " + analysis.substring(0, 80) + "...");
            } else {
                System.out.println("  Analysis: " + analysis);
            }
            System.out.println("  Usage: " + workflow.getOutput().get("usage"));
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
