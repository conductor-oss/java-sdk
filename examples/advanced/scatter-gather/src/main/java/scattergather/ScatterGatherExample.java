package scattergather;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import scattergather.workers.SgrBroadcastWorker;
import scattergather.workers.SgrGather1Worker;
import scattergather.workers.SgrGather2Worker;
import scattergather.workers.SgrGather3Worker;
import scattergather.workers.SgrAggregateWorker;

import java.util.List;
import java.util.Map;

/**
 * Scatter-Gather Demo
 *
 * Run:
 *   java -jar target/scattergather-1.0.0.jar
 */
public class ScatterGatherExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Scatter-Gather Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sgr_broadcast",
                "sgr_gather_1",
                "sgr_gather_2",
                "sgr_gather_3",
                "sgr_aggregate"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'sgr_scatter_gather'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SgrBroadcastWorker(),
                new SgrGather1Worker(),
                new SgrGather2Worker(),
                new SgrGather3Worker(),
                new SgrAggregateWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("sgr_scatter_gather", 1,
                Map.of("query", "laptop_model_X500"));
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