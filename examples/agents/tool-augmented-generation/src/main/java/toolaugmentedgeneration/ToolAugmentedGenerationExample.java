package toolaugmentedgeneration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import toolaugmentedgeneration.workers.StartGenerationWorker;
import toolaugmentedgeneration.workers.DetectGapWorker;
import toolaugmentedgeneration.workers.CallToolWorker;
import toolaugmentedgeneration.workers.IncorporateResultWorker;
import toolaugmentedgeneration.workers.CompleteGenerationWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool-Augmented Generation Demo
 *
 * Demonstrates a sequential pipeline of five workers that detect knowledge gaps
 * during text generation, invoke external tools to fill them, and produce
 * enriched output:
 *   tg_start_generation -> tg_detect_gap -> tg_call_tool -> tg_incorporate_result -> tg_complete_generation
 *
 * Run:
 *   java -jar target/tool-augmented-generation-1.0.0.jar
 */
public class ToolAugmentedGenerationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool-Augmented Generation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tg_start_generation", "tg_detect_gap",
                "tg_call_tool", "tg_incorporate_result",
                "tg_complete_generation"));
        System.out.println("  Registered: tg_start_generation, tg_detect_gap, tg_call_tool, tg_incorporate_result, tg_complete_generation\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_augmented_generation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new StartGenerationWorker(),
                new DetectGapWorker(),
                new CallToolWorker(),
                new IncorporateResultWorker(),
                new CompleteGenerationWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("tool_augmented_generation", 1,
                Map.of("prompt", "Write a brief overview of Node.js including its current version"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
