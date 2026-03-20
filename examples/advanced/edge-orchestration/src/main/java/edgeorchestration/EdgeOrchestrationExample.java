package edgeorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import edgeorchestration.workers.EorDispatchWorker;
import edgeorchestration.workers.EorEdgeProcessWorker;
import edgeorchestration.workers.EorCollectWorker;
import edgeorchestration.workers.EorMergeWorker;

import java.util.List;
import java.util.Map;

/**
 * Edge Orchestration Demo
 *
 * Run:
 *   java -jar target/edgeorchestration-1.0.0.jar
 */
public class EdgeOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Edge Orchestration Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "eor_dispatch",
                "eor_edge_process",
                "eor_collect",
                "eor_merge"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'edge_orchestration_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new EorDispatchWorker(),
                new EorEdgeProcessWorker(),
                new EorCollectWorker(),
                new EorMergeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("edge_orchestration_demo", 1,
                Map.of("jobId", "EDGE-JOB-77", "edgeNodes", java.util.List.of("edge-factory-1", "edge-factory-2")));
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