package rollingupdate;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import rollingupdate.workers.*;

import java.util.*;

/**
 * Rolling Update — Zero-Downtime Update Orchestration
 *
 * Analyzes current deployment, plans the rolling strategy, executes the update
 * batch by batch, and verifies all replicas are healthy.
 *
 * Pattern: analyze -> plan -> execute -> verify
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/rolling-update-1.0.0.jar
 */
public class RollingUpdateExample {

    private static final List<String> TASK_NAMES = List.of(
            "ru_analyze", "ru_plan", "ru_execute", "ru_verify"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new Analyze(),
                new PlanUpdate(),
                new ExecuteUpdate(),
                new VerifyUpdate()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Rolling Update Demo: Zero-Downtime Update Orchestration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        System.out.println("Step 2: Registering workflow 'rolling_update_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting rolling update workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("service", "user-service");
        input.put("newVersion", "2.5.0");

        String workflowId = client.startWorkflow("rolling_update_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Update Results ---");
            System.out.println("  analyzeResult: " + output.get("analyzeResult"));
            System.out.println("  verifyResult : " + output.get("verifyResult"));
        }

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
        } else {
            System.out.println("\nResult: FAILED (status: " + status + ")");
            System.exit(1);
        }
    }
}
