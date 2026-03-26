package networkpartitions;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import networkpartitions.workers.NetworkPartitionWorker;

import java.util.List;
import java.util.Map;

/**
 * Network Partitions Demo -- handling network partitions with resilient workers
 *
 * Demonstrates how Conductor workers handle network partitions through
 * reconnection. The worker tracks attempt counts using an AtomicInteger
 * for thread-safe operation and returns results with attempt metadata.
 *
 * Run:
 *   java -jar target/network-partitions-1.0.0.jar
 *   java -jar target/network-partitions-1.0.0.jar --workers
 */
public class NetworkPartitionsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Network Partitions Demo: Handling Network Partitions ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definition
        System.out.println("Step 1: Registering task definition for np_resilient_task...");

        TaskDef resilientTask = new TaskDef();
        resilientTask.setName("np_resilient_task");
        resilientTask.setRetryCount(3);
        resilientTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        resilientTask.setRetryDelaySeconds(1);
        resilientTask.setTimeoutSeconds(60);
        resilientTask.setResponseTimeoutSeconds(30);
        resilientTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(resilientTask));

        System.out.println("\n  Registered: np_resilient_task");
        System.out.println("    Retries: 3 (FIXED, 1s delay)");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'network_partitions_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        List<Worker> workers = List.of(worker);
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow with data='hello'...\n");
        String workflowId = client.startWorkflow("network_partitions_demo", 1,
                Map.of("data", "hello"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        System.out.println("  Worker attempt count: " + worker.getAttemptCount());

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
