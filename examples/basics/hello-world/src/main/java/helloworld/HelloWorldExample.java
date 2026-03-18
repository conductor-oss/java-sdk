package helloworld;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import helloworld.workers.GreetWorker;

import java.util.List;
import java.util.Map;

/**
 * Hello World — Your First Conductor Workflow and Worker
 *
 * The simplest possible Conductor example:
 * - Register a workflow with a single SIMPLE task
 * - Create a worker that handles that task
 * - Start the workflow and watch it complete
 *
 * Run:
 *   java -jar target/hello-world-1.0.0.jar
 */
public class HelloWorldExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Hello World: Your First Conductor Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");
        client.registerTaskDefs(List.of("greet"));
        System.out.println("  Registered: greet\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'hello_world_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start worker
        System.out.println("Step 3: Starting worker...");
        List<Worker> workers = List.of(new GreetWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("hello_world_workflow", 1,
                Map.of("name", "Developer"));
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
