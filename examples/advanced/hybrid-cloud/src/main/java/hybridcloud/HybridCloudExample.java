package hybridcloud;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import hybridcloud.workers.HybClassifyDataWorker;
import hybridcloud.workers.HybProcessOnpremWorker;
import hybridcloud.workers.HybProcessCloudWorker;

import java.util.List;
import java.util.Map;

/**
 * Hybrid Cloud Demo
 *
 * Run:
 *   java -jar target/hybridcloud-1.0.0.jar
 */
public class HybridCloudExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Hybrid Cloud Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "hyb_classify_data",
                "hyb_process_onprem",
                "hyb_process_cloud"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'hybrid_cloud_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new HybClassifyDataWorker(),
                new HybProcessOnpremWorker(),
                new HybProcessCloudWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("hybrid_cloud_demo", 1,
                Map.of("dataId", "DATA-PII-001", "dataType", "pii", "payload", "user_records"));
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