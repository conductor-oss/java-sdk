package ratelimitermicroservice;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ratelimitermicroservice.workers.CheckQuotaWorker;
import ratelimitermicroservice.workers.ProcessRequestWorker;
import ratelimitermicroservice.workers.UpdateCounterWorker;
import ratelimitermicroservice.workers.RejectRequestWorker;

import java.util.List;
import java.util.Map;

/**
 * Rate Limiter Microservice Demo
 *
 * Demonstrates distributed rate limiting:
 *   rl_check_quota -> SWITCH(allowed:
 *       true  -> rl_process_request -> rl_update_counter,
 *       false -> rl_reject_request)
 *
 * Run:
 *   java -jar target/rate-limiter-microservice-1.0.0.jar
 */
public class RateLimiterMicroserviceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Rate Limiter Microservice Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rl_check_quota", "rl_process_request",
                "rl_update_counter", "rl_reject_request"));
        System.out.println("  Registered tasks.\n");

        System.out.println("Step 2: Registering workflow 'rate_limiter_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckQuotaWorker(),
                new ProcessRequestWorker(),
                new UpdateCounterWorker(),
                new RejectRequestWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rate_limiter_workflow", 1,
                Map.of("clientId", "client-99",
                        "endpoint", "/api/orders",
                        "request", Map.of("action", "list")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
