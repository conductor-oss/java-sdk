package splitterpattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import splitterpattern.workers.SplReceiveCompositeWorker;
import splitterpattern.workers.SplSplitWorker;
import splitterpattern.workers.SplProcessPart1Worker;
import splitterpattern.workers.SplProcessPart2Worker;
import splitterpattern.workers.SplProcessPart3Worker;
import splitterpattern.workers.SplCombineWorker;

import java.util.List;
import java.util.Map;

/**
 * Splitter Pattern Demo
 *
 * Run:
 *   java -jar target/splitterpattern-1.0.0.jar
 */
public class SplitterPatternExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Splitter Pattern Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "spl_receive_composite",
                "spl_split",
                "spl_process_part_1",
                "spl_process_part_2",
                "spl_process_part_3",
                "spl_combine"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'spl_splitter_pattern'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SplReceiveCompositeWorker(),
                new SplSplitWorker(),
                new SplProcessPart1Worker(),
                new SplProcessPart2Worker(),
                new SplProcessPart3Worker(),
                new SplCombineWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("spl_splitter_pattern", 1,
                Map.of("compositeMessage", java.util.Map.of("orderId", "ORD-2024-600", "customerId", "CUST-50")));
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