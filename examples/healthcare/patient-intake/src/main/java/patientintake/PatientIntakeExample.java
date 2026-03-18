package patientintake;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import patientintake.workers.RegisterWorker;
import patientintake.workers.VerifyInsuranceWorker;
import patientintake.workers.TriageWorker;
import patientintake.workers.AssignWorker;

import java.util.List;
import java.util.Map;

/**
 * Patient Intake Demo
 *
 * Demonstrates a sequential patient intake workflow:
 *   pit_register -> pit_verify_insurance -> pit_triage -> pit_assign
 *
 * Run:
 *   java -jar target/patient-intake-1.0.0.jar
 */
public class PatientIntakeExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Patient Intake Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pit_register", "pit_verify_insurance",
                "pit_triage", "pit_assign"));
        System.out.println("  Registered: pit_register, pit_verify_insurance, pit_triage, pit_assign\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'patient_intake_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RegisterWorker(),
                new VerifyInsuranceWorker(),
                new TriageWorker(),
                new AssignWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("patient_intake_workflow", 1,
                Map.of("patientId", "PAT-10234",
                        "name", "Sarah Johnson",
                        "chiefComplaint", "Severe headache and dizziness",
                        "insuranceId", "INS-BC-55012"));
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
