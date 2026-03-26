package taskinputtemplates;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskinputtemplates.workers.BuildContextWorker;
import taskinputtemplates.workers.ExecuteActionWorker;
import taskinputtemplates.workers.LookupUserWorker;

import java.util.List;
import java.util.Map;

/**
 * Task Input Templates — Dynamic Parameter Wiring
 *
 * Demonstrates how ${} template expressions wire data into task inputs:
 * - Map workflow inputs directly into task parameters
 * - Combine static values with dynamic references
 * - Build nested objects from multiple task outputs
 * - Flatten nested output into individual parameters
 *
 * Run:
 *   java -jar target/task-input-templates-1.0.0.jar
 */
public class TaskInputTemplatesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Task Input Templates: Dynamic Parameter Wiring ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("tpl_lookup_user", "tpl_build_context", "tpl_execute_action"));
        System.out.println("  Registered: tpl_lookup_user, tpl_build_context, tpl_execute_action\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'input_templates_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LookupUserWorker(),
                new BuildContextWorker(),
                new ExecuteActionWorker()
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

        // Step 4 — Test admin user (should succeed with write permission)
        System.out.println("Step 4: Testing admin user (write action)...\n");
        String adminId = client.startWorkflow("input_templates_demo", 1, Map.of(
                "userId", "U-1",
                "action", "write",
                "metadata", Map.of("source", "dashboard")
        ));
        System.out.println("  Workflow ID: " + adminId + "\n");

        Workflow adminWf = client.waitForWorkflow(adminId, "COMPLETED", 30000);
        String adminStatus = adminWf.getStatus().name();
        System.out.println("  Status: " + adminStatus);
        System.out.println("  Admin result: " + adminWf.getOutput().get("result") + "\n");

        // Step 5 — Test viewer user (should be denied write permission)
        System.out.println("Step 5: Testing viewer user (write action)...\n");
        String viewerId = client.startWorkflow("input_templates_demo", 1, Map.of(
                "userId", "U-2",
                "action", "write",
                "metadata", Map.of("source", "api")
        ));
        System.out.println("  Workflow ID: " + viewerId + "\n");

        Workflow viewerWf = client.waitForWorkflow(viewerId, "COMPLETED", 30000);
        String viewerStatus = viewerWf.getStatus().name();
        System.out.println("  Status: " + viewerStatus);
        System.out.println("  Viewer result: " + viewerWf.getOutput().get("result") + "\n");

        client.stopWorkers();

        if ("COMPLETED".equals(adminStatus) && "COMPLETED".equals(viewerStatus)
                && "success".equals(adminWf.getOutput().get("result"))
                && "permission_denied".equals(viewerWf.getOutput().get("result"))) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
