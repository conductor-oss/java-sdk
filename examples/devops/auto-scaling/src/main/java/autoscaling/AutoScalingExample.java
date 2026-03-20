package autoscaling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import autoscaling.workers.*;

import java.util.*;

/**
 * Auto-Scaling — Intelligent Scaling Orchestration
 *
 * Analyzes service metrics, plans scaling action, executes scaling,
 * and verifies the result.
 *
 * Pattern: analyze -> plan -> execute -> verify
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/auto-scaling-1.0.0.jar
 */
public class AutoScalingExample {

    private static final List<String> TASK_NAMES = List.of(
            "as_analyze", "as_plan", "as_execute", "as_verify"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new Analyze(),
                new Plan(),
                new Execute(),
                new Verify()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Auto-Scaling Demo: Intelligent Scaling Orchestration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        System.out.println("Step 2: Registering workflow 'auto_scaling_workflow'...");
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

        System.out.println("Step 4: Starting auto-scaling workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("service", "api-server");
        input.put("metric", "cpu");
        input.put("threshold", 80);

        String workflowId = client.startWorkflow("auto_scaling_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Scaling Results ---");
            System.out.println("  Action   : " + output.get("action"));
            System.out.println("  Verified : " + output.get("verified"));
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
