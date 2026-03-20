package toolusevalidation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import toolusevalidation.workers.GenerateToolCallWorker;
import toolusevalidation.workers.ValidateInputWorker;
import toolusevalidation.workers.ExecuteToolWorker;
import toolusevalidation.workers.ValidateOutputWorker;
import toolusevalidation.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Validation Demo
 *
 * Demonstrates a sequential pipeline of five workers that generate a tool call,
 * validate its input, execute the tool, validate its output, and deliver the result.
 *   tv_generate_tool_call -> tv_validate_input -> tv_execute_tool -> tv_validate_output -> tv_deliver
 *
 * Run:
 *   java -jar target/tool-use-validation-1.0.0.jar
 */
public class ToolUseValidationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Validation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tv_generate_tool_call", "tv_validate_input",
                "tv_execute_tool", "tv_validate_output", "tv_deliver"));
        System.out.println("  Registered: tv_generate_tool_call, tv_validate_input, tv_execute_tool, tv_validate_output, tv_deliver\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_validation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GenerateToolCallWorker(),
                new ValidateInputWorker(),
                new ExecuteToolWorker(),
                new ValidateOutputWorker(),
                new DeliverWorker()
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
        String workflowId = client.startWorkflow("tool_use_validation", 1,
                Map.of("userRequest", "What is the current weather in London?",
                        "toolName", "weather_api"));
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
