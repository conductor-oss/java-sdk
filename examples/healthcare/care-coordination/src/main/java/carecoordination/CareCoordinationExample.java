package carecoordination;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import carecoordination.workers.AssessNeedsWorker;
import carecoordination.workers.CreatePlanWorker;
import carecoordination.workers.AssignTeamWorker;
import carecoordination.workers.MonitorWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 481: Care Coordination
 *
 * Assess patient needs, create a care plan, assign a care team,
 * and monitor ongoing progress.
 *
 * Run:
 *   java -jar target/care-coordination-1.0.0.jar
 */
public class CareCoordinationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 481: Care Coordination ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ccr_assess_needs", "ccr_create_plan",
                "ccr_assign_team", "ccr_monitor"));
        System.out.println("  Registered: ccr_assess_needs, ccr_create_plan, ccr_assign_team, ccr_monitor\n");

        System.out.println("Step 2: Registering workflow 'care_coordination_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AssessNeedsWorker(),
                new CreatePlanWorker(),
                new AssignTeamWorker(),
                new MonitorWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("care_coordination_workflow", 1,
                Map.of("patientId", "PAT-10234",
                        "condition", "Type 2 Diabetes with complications",
                        "acuity", "high"));
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
