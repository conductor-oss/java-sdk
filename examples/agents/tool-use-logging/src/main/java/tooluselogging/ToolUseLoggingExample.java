package tooluselogging;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tooluselogging.workers.LogRequestWorker;
import tooluselogging.workers.ExecuteToolWorker;
import tooluselogging.workers.LogResponseWorker;
import tooluselogging.workers.CreateAuditEntryWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Logging Demo
 *
 * Demonstrates a sequential pipeline of four workers that log tool requests
 * and responses, execute tools, and create audit entries:
 *   log_request -> execute_tool -> log_response -> create_audit_entry
 *
 * Run:
 *   java -jar target/tool-use-logging-1.0.0.jar
 */
public class ToolUseLoggingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Logging Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tl_log_request", "tl_execute_tool", "tl_log_response", "tl_create_audit_entry"));
        System.out.println("  Registered: tl_log_request, tl_execute_tool, tl_log_response, tl_create_audit_entry\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_logging'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LogRequestWorker(),
                new ExecuteToolWorker(),
                new LogResponseWorker(),
                new CreateAuditEntryWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("tool_use_logging", 1,
                Map.of("toolName", "sentiment_analysis",
                        "toolArgs", Map.of("text", "I am thrilled with the new product launch and excited about the future roadmap!"),
                        "userId", "user-7392",
                        "sessionId", "sess-abc-12345"));
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
