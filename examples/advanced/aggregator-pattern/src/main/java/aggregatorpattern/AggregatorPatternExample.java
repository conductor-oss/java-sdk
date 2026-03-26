package aggregatorpattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aggregatorpattern.workers.AgpCollectWorker;
import aggregatorpattern.workers.AgpCheckCompleteWorker;
import aggregatorpattern.workers.AgpAggregateWorker;
import aggregatorpattern.workers.AgpForwardWorker;

import java.util.List;
import java.util.Map;

/**
 * Aggregator Pattern Demo
 *
 * Run:
 *   java -jar target/aggregatorpattern-1.0.0.jar
 */
public class AggregatorPatternExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Aggregator Pattern Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "agp_collect",
                "agp_check_complete",
                "agp_aggregate",
                "agp_forward"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'agp_aggregator_pattern'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AgpCollectWorker(),
                new AgpCheckCompleteWorker(),
                new AgpAggregateWorker(),
                new AgpForwardWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("agp_aggregator_pattern", 1,
                Map.of("messages", java.util.List.of(java.util.Map.of("id","P1","amount",150.00), java.util.Map.of("id","P2","amount",230.00)), "expectedCount", 2, "aggregationKey", "daily-sales"));
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