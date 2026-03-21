package dischargeplanning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dischargeplanning.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 482: Discharge Planning
 *
 * Assess discharge readiness, create a discharge plan, coordinate services,
 * educate the patient, and schedule follow-up care.
 */
public class DischargePlanningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 482: Discharge Planning ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dsc_assess_readiness", "dsc_create_plan",
                "dsc_coordinate", "dsc_educate", "dsc_schedule_followup"));
        System.out.println("  Registered 5 tasks.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AssessReadinessWorker(),
                new CreateDischargePlanWorker(),
                new CoordinateWorker(),
                new EducateWorker(),
                new ScheduleFollowupWorker()
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
        String workflowId = client.startWorkflow("discharge_planning_workflow", 1,
                Map.of("patientId", "PAT-10234",
                        "admissionId", "ADM-88201",
                        "diagnosis", "Acute myocardial infarction"));
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
