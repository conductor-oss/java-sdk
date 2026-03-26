package taskdomains;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskdomains.workers.TdProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Task Domains — Route tasks to specific worker groups.
 *
 * Demonstrates the domain concept: workers poll with domain="gpu"
 * to only receive GPU-tagged tasks. This enables routing specific
 * tasks to specialized worker pools.
 *
 * Run:
 *   java -jar target/task-domains-1.0.0.jar
 *   java -jar target/task-domains-1.0.0.jar --workers
 */
public class TaskDomainsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Task Domains: Route Tasks to Specific Worker Groups ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("td_process"));
        System.out.println("  Registered: td_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'task_domain_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers with domain mapping
        System.out.println("Step 3: Starting workers with domain='gpu'...");
        List<Worker> workers = List.of(
                new TdProcessWorker("gpu-group")
        );
        client.startWorkers(workers, Map.of("td_process", "gpu"));
        System.out.println("  1 worker polling (domain: gpu).\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("task_domain_demo", 1,
                Map.of("data", Map.of("type", "matrix", "value", 42)),
                Map.of("td_process", "gpu"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
