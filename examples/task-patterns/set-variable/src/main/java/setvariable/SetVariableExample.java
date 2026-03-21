package setvariable;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import setvariable.workers.ApplyRulesWorker;
import setvariable.workers.FinalizeWorker;
import setvariable.workers.ProcessItemsWorker;

import java.util.List;
import java.util.Map;

/**
 * SET_VARIABLE System Task — Storing Intermediate Workflow State
 *
 * Demonstrates how SET_VARIABLE stores intermediate results as workflow-level
 * variables accessible via ${workflow.variables.key}. This lets later tasks
 * read accumulated state without direct task-to-task wiring.
 *
 * Flow:
 *   sv_process_items  ->  SET_VARIABLE (store totals)
 *       -> sv_apply_rules  ->  SET_VARIABLE (store rules)
 *           -> sv_finalize (reads all variables, produces decision)
 *
 * Run:
 *   java -jar target/set-variable-1.0.0.jar
 *   java -jar target/set-variable-1.0.0.jar --workers
 */
public class SetVariableExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SET_VARIABLE: Storing Intermediate Workflow State ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("sv_process_items", "sv_apply_rules", "sv_finalize"));
        System.out.println("  Registered: sv_process_items, sv_apply_rules, sv_finalize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'set_variable_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessItemsWorker(),
                new ApplyRulesWorker(),
                new FinalizeWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        List<Map<String, Object>> items = List.of(
                Map.of("name", "Server License", "amount", 2500),
                Map.of("name", "Support Plan", "amount", 800),
                Map.of("name", "Training", "amount", 1200)
        );
        String workflowId = client.startWorkflow("set_variable_demo", 1,
                Map.of("items", items));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        if (workflow.getVariables() != null && !workflow.getVariables().isEmpty()) {
            System.out.println("  Workflow Variables: " + workflow.getVariables());
        }

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
