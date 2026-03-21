package fallbacktasks;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import fallbacktasks.workers.CacheLookupWorker;
import fallbacktasks.workers.PrimaryApiWorker;
import fallbacktasks.workers.SecondaryApiWorker;

import java.util.List;
import java.util.Map;

/**
 * Fallback Tasks -- Alternative Paths on Failure
 *
 * Demonstrates a workflow that calls a primary API and uses a SWITCH task
 * to route to fallback workers when the primary is unavailable or errors.
 *
 * Flow:
 *   primary_api -> SWITCH on apiStatus:
 *     "unavailable" -> secondary_api
 *     "error"       -> cache_lookup
 *     default       -> success (primary data used)
 *
 * Run:
 *   java -jar target/fallback-tasks-1.0.0.jar
 *   java -jar target/fallback-tasks-1.0.0.jar --workers
 */
public class FallbackTasksExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Fallback Tasks Demo: Alternative Paths on Failure ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef primaryTask = new TaskDef();
        primaryTask.setName("fb_primary_api");
        primaryTask.setTimeoutSeconds(60);
        primaryTask.setResponseTimeoutSeconds(30);
        primaryTask.setOwnerEmail("examples@orkes.io");

        TaskDef secondaryTask = new TaskDef();
        secondaryTask.setName("fb_secondary_api");
        secondaryTask.setTimeoutSeconds(60);
        secondaryTask.setResponseTimeoutSeconds(30);
        secondaryTask.setOwnerEmail("examples@orkes.io");

        TaskDef cacheTask = new TaskDef();
        cacheTask.setName("fb_cache_lookup");
        cacheTask.setTimeoutSeconds(60);
        cacheTask.setResponseTimeoutSeconds(30);
        cacheTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(primaryTask, secondaryTask, cacheTask));

        System.out.println("  Registered: fb_primary_api, fb_secondary_api, fb_cache_lookup\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'fallback_tasks_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrimaryApiWorker(),
                new SecondaryApiWorker(),
                new CacheLookupWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4a -- Primary API available (happy path)
        System.out.println("Step 4a: Starting workflow (available=true) -- happy path...\n");
        String wfIdOk = client.startWorkflow("fallback_tasks_demo", 1,
                Map.of("available", true));
        System.out.println("  Workflow ID: " + wfIdOk);
        Workflow wfOk = client.waitForWorkflow(wfIdOk, "COMPLETED", 30000);
        System.out.println("  Status: " + wfOk.getStatus().name());
        System.out.println("  Output: " + wfOk.getOutput() + "\n");

        // Step 4b -- Primary API unavailable (fallback to secondary)
        System.out.println("Step 4b: Starting workflow (available=false) -- fallback to secondary...\n");
        String wfIdFb = client.startWorkflow("fallback_tasks_demo", 1,
                Map.of("available", false));
        System.out.println("  Workflow ID: " + wfIdFb);
        Workflow wfFb = client.waitForWorkflow(wfIdFb, "COMPLETED", 30000);
        System.out.println("  Status: " + wfFb.getStatus().name());
        System.out.println("  Output: " + wfFb.getOutput() + "\n");

        client.stopWorkers();

        boolean bothPassed = "COMPLETED".equals(wfOk.getStatus().name())
                && "COMPLETED".equals(wfFb.getStatus().name());

        if (bothPassed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
