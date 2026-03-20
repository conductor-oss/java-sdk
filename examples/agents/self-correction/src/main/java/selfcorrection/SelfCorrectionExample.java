package selfcorrection;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import selfcorrection.workers.GenerateCodeWorker;
import selfcorrection.workers.RunTestsWorker;
import selfcorrection.workers.DiagnoseWorker;
import selfcorrection.workers.FixWorker;
import selfcorrection.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Self-Correction Demo
 *
 * Demonstrates a self-correcting code pipeline:
 * generate code, run tests, and if tests fail, diagnose errors,
 * fix the code, then deliver the corrected version.
 *   sc_generate_code -> sc_run_tests -> SWITCH(pass->deliver, fail->diagnose->fix->deliver)
 *
 * Run:
 *   java -jar target/self-correction-1.0.0.jar
 */
public class SelfCorrectionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Self-Correction Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sc_generate_code", "sc_run_tests",
                "sc_diagnose", "sc_fix", "sc_deliver"));
        System.out.println("  Registered: sc_generate_code, sc_run_tests, sc_diagnose, sc_fix, sc_deliver\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'self_correction'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GenerateCodeWorker(),
                new RunTestsWorker(),
                new DiagnoseWorker(),
                new FixWorker(),
                new DeliverWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("self_correction", 1,
                Map.of("requirement", "Write a fibonacci function with proper error handling"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
