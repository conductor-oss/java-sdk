package correlationpattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import correlationpattern.workers.CrpReceiveMessagesWorker;
import correlationpattern.workers.CrpMatchByIdWorker;
import correlationpattern.workers.CrpAggregateWorker;
import correlationpattern.workers.CrpProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Correlation Pattern Demo
 *
 * Run:
 *   java -jar target/correlationpattern-1.0.0.jar
 */
public class CorrelationPatternExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Correlation Pattern Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "crp_receive_messages",
                "crp_match_by_id",
                "crp_aggregate",
                "crp_process"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'crp_correlation_pattern'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CrpReceiveMessagesWorker(),
                new CrpMatchByIdWorker(),
                new CrpAggregateWorker(),
                new CrpProcessWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("crp_correlation_pattern", 1,
                Map.of("messages", java.util.List.of(java.util.Map.of("correlationId","TXN-001","source","payment"), java.util.Map.of("correlationId","TXN-001","source","bank")), "correlationField", "correlationId"));
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