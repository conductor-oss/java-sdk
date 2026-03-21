package polltimeout;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import polltimeout.workers.PollNormalTaskWorker;

import java.util.List;
import java.util.Map;

/**
 * Poll Timeout — Handle missing workers.
 *
 * Demonstrates how pollTimeoutSeconds defines how long a task waits
 * in the queue for a worker to pick it up. If no worker polls within
 * that window, the task times out.
 *
 * Run:
 *   java -jar target/poll-timeout-1.0.0.jar
 *   java -jar target/poll-timeout-1.0.0.jar --workers
 */
public class PollTimeoutExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Poll Timeout Demo: Handle Missing Workers ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition with pollTimeoutSeconds
        System.out.println("Step 1: Registering task definition with pollTimeoutSeconds...");
        TaskDef taskDef = new TaskDef();
        taskDef.setName("poll_normal_task");
        taskDef.setRetryCount(2);
        taskDef.setTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        taskDef.setPollTimeoutSeconds(30);
        taskDef.setOwnerEmail("examples@orkes.io");
        client.registerTaskDefs(List.of(taskDef));
        System.out.println("  Registered: poll_normal_task");
        System.out.println("    pollTimeoutSeconds: 30");
        System.out.println("    timeoutSeconds: 60\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'poll_timeout_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PollNormalTaskWorker());
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
        String workflowId = client.startWorkflow("poll_timeout_demo", 1,
                Map.of("mode", "normal"));
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
