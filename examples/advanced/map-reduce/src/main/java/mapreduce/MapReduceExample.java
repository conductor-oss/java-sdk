package mapreduce;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import mapreduce.workers.MprSplitInputWorker;
import mapreduce.workers.MprMap1Worker;
import mapreduce.workers.MprMap2Worker;
import mapreduce.workers.MprMap3Worker;
import mapreduce.workers.MprReduceWorker;
import mapreduce.workers.MprOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * MapReduce Demo
 *
 * Run:
 *   java -jar target/mapreduce-1.0.0.jar
 */
public class MapReduceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== MapReduce Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mpr_split_input",
                "mpr_map_1",
                "mpr_map_2",
                "mpr_map_3",
                "mpr_reduce",
                "mpr_output"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'mpr_map_reduce'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MprSplitInputWorker(),
                new MprMap1Worker(),
                new MprMap2Worker(),
                new MprMap3Worker(),
                new MprReduceWorker(),
                new MprOutputWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("mpr_map_reduce", 1,
                Map.of("documents", java.util.List.of("doc1","doc2","doc3"), "searchTerm", "error"));
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