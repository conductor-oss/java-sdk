package ratelimiting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import ratelimiting.workers.RlApiCallWorker;

import java.util.List;
import java.util.Map;

/**
 * Rate Limiting — Task-level rate limiting configuration
 *
 * Demonstrates how to configure rate limiting on a Conductor task:
 * - rateLimitPerFrequency: max executions per time window
 * - rateLimitFrequencyInSeconds: time window duration
 * - concurrentExecLimit: max concurrent executions
 *
 * Run:
 *   java -jar target/rate-limiting-1.0.0.jar
 *   java -jar target/rate-limiting-1.0.0.jar --workers
 */
public class RateLimitingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Rate Limiting Demo: Task-Level Rate Limiting ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition with rate limit config
        System.out.println("Step 1: Registering task definition with rate limiting...");
        TaskDef taskDef = new TaskDef();
        taskDef.setName("rl_api_call");
        taskDef.setRetryCount(2);
        taskDef.setTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        taskDef.setOwnerEmail("examples@orkes.io");
        taskDef.setRateLimitPerFrequency(5);
        taskDef.setRateLimitFrequencyInSeconds(10);
        taskDef.setConcurrentExecLimit(2);
        client.registerTaskDefs(List.of(taskDef));
        System.out.println("  Registered: rl_api_call");
        System.out.println("    rateLimitPerFrequency: 5");
        System.out.println("    rateLimitFrequencyInSeconds: 10");
        System.out.println("    concurrentExecLimit: 2\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rate_limit_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new RlApiCallWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rate_limit_demo", 1,
                Map.of("batchId", 42));
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
