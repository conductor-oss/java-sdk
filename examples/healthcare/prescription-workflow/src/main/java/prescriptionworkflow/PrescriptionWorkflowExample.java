package prescriptionworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import prescriptionworkflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Prescription Workflow Demo
 *
 * prx_verify -> prx_check_interactions -> prx_fill -> prx_dispense -> prx_track
 *
 * Run:
 *   java -jar target/prescription-workflow-1.0.0.jar
 */
public class PrescriptionWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Prescription Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "prx_verify", "prx_check_interactions", "prx_fill", "prx_dispense", "prx_track"));
        System.out.println("  Registered: prx_verify, prx_check_interactions, prx_fill, prx_dispense, prx_track\n");

        System.out.println("Step 2: Registering workflow 'prescription_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new VerifyWorker(),
                new CheckInteractionsWorker(),
                new FillWorker(),
                new DispenseWorker(),
                new TrackWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("prescription_workflow", 1,
                Map.of("prescriptionId", "RX-20240301-001",
                        "patientId", "PAT-10234",
                        "medication", "Atorvastatin",
                        "dosage", "20mg daily"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
