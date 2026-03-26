package workflowversioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowversioning.workers.VerCalcWorker;
import workflowversioning.workers.VerBonusWorker;
import workflowversioning.workers.VerAuditWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Versioning — Run multiple versions simultaneously.
 *
 * Three versions of "versioned_workflow":
 *   V1: ver_calc only (value*2)
 *   V2: ver_calc + ver_bonus (value*2 + 10)
 *   V3: ver_calc + ver_bonus + ver_audit
 *
 * Run:
 *   java -jar target/workflow-versioning-1.0.0.jar
 */
public class WorkflowVersioningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Versioning: Run Multiple Versions Simultaneously ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ver_calc", "ver_bonus", "ver_audit"));
        System.out.println("  Registered: ver_calc, ver_bonus, ver_audit\n");

        // Step 2 — Register all 3 workflow versions
        System.out.println("Step 2: Registering workflow versions...");
        client.registerWorkflow("workflow-v1.json");
        System.out.println("  Registered: versioned_workflow v1 (calc only)");
        client.registerWorkflow("workflow-v2.json");
        System.out.println("  Registered: versioned_workflow v2 (calc + bonus)");
        client.registerWorkflow("workflow-v3.json");
        System.out.println("  Registered: versioned_workflow v3 (calc + bonus + audit)\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new VerCalcWorker(),
                new VerBonusWorker(),
                new VerAuditWorker()
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

        Map<String, Object> input = Map.of("value", 50);

        // Step 4 — Start all 3 versions
        System.out.println("Step 4: Starting all 3 workflow versions with input: " + input + "\n");

        String id1 = client.startWorkflow("versioned_workflow", 1, input);
        System.out.println("  V1 Workflow ID: " + id1);

        String id2 = client.startWorkflow("versioned_workflow", 2, input);
        System.out.println("  V2 Workflow ID: " + id2);

        String id3 = client.startWorkflow("versioned_workflow", 3, input);
        System.out.println("  V3 Workflow ID: " + id3 + "\n");

        // Step 5 — Wait for all to complete
        System.out.println("Step 5: Waiting for all versions to complete...\n");

        Workflow wf1 = client.waitForWorkflow(id1, "COMPLETED", 30000);
        String status1 = wf1.getStatus().name();
        System.out.println("  V1 Status: " + status1 + " | Output: " + wf1.getOutput());

        Workflow wf2 = client.waitForWorkflow(id2, "COMPLETED", 30000);
        String status2 = wf2.getStatus().name();
        System.out.println("  V2 Status: " + status2 + " | Output: " + wf2.getOutput());

        Workflow wf3 = client.waitForWorkflow(id3, "COMPLETED", 30000);
        String status3 = wf3.getStatus().name();
        System.out.println("  V3 Status: " + status3 + " | Output: " + wf3.getOutput());

        client.stopWorkers();

        boolean allPassed = "COMPLETED".equals(status1)
                && "COMPLETED".equals(status2)
                && "COMPLETED".equals(status3);

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
