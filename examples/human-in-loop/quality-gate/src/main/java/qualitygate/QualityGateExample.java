package qualitygate;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import qualitygate.workers.DeployWorker;
import qualitygate.workers.RunTestsWorker;

import java.util.List;
import java.util.Map;

/**
 * Quality Gate -- Automated Tests, Human Sign-Off, Deploy
 *
 * Demonstrates a workflow that:
 *   1. Runs automated tests (qg_run_tests)
 *   2. Switches on allPassed -- if false, terminates; otherwise continues
 *   3. Waits for QA human sign-off (WAIT task: qa_signoff)
 *   4. Deploys the application (qg_deploy)
 *
 * Run:
 *   java -jar target/quality-gate-1.0.0.jar
 *   java -jar target/quality-gate-1.0.0.jar --workers
 */
public class QualityGateExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Quality Gate Demo: Automated Tests -> Human Sign-Off -> Deploy ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef runTestsTask = new TaskDef();
        runTestsTask.setName("qg_run_tests");
        runTestsTask.setTimeoutSeconds(60);
        runTestsTask.setResponseTimeoutSeconds(30);
        runTestsTask.setOwnerEmail("examples@orkes.io");

        TaskDef deployTask = new TaskDef();
        deployTask.setName("qg_deploy");
        deployTask.setTimeoutSeconds(60);
        deployTask.setResponseTimeoutSeconds(30);
        deployTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(runTestsTask, deployTask));

        System.out.println("  Registered: qg_run_tests, qg_deploy\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'quality_gate_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new RunTestsWorker(), new DeployWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("quality_gate_demo", 1, Map.of());
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion (WAIT task requires external signal)...");
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
