package workflowmetadata;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowmetadata.workers.MetadataTaskWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Example 42: Workflow Metadata -- Tags, Labels, and Search
 *
 * Demonstrates how to add metadata to workflows for organization and
 * searchability. Starts workflows with different categories, then
 * demonstrates searching/querying workflow definitions.
 *
 * Run:
 *   java -jar target/workflow-metadata-1.0.0.jar
 *   java -jar target/workflow-metadata-1.0.0.jar --workers
 */
public class WorkflowMetadataExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 42: Workflow Metadata ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("md_task"));
        System.out.println("  Registered: md_task\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'metadata_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new MetadataTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start workflows with different categories
        System.out.println("Step 4: Starting workflows with different categories...\n");
        String[] categories = {"billing", "support", "engineering"};
        List<String> workflowIds = new ArrayList<>();
        for (String category : categories) {
            String workflowId = client.startWorkflow("metadata_demo", 1,
                    Map.of("category", category, "priority", "high"));
            workflowIds.add(workflowId);
            System.out.println("  Started: " + category + " -> " + workflowId);
        }
        System.out.println();

        // Step 5 -- Wait for all workflows to complete
        System.out.println("Step 5: Waiting for all workflows to complete...");
        boolean allCompleted = true;
        for (String workflowId : workflowIds) {
            Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
            String status = workflow.getStatus().name();
            System.out.println("  " + workflowId + ": " + status);
            if (!"COMPLETED".equals(status)) {
                allCompleted = false;
            }
        }
        System.out.println("\nAll workflows completed.\n");

        client.stopWorkers();

        if (allCompleted) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
