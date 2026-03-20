package hierarchicalagents;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import hierarchicalagents.workers.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hierarchical Agents — Manager-Lead-Worker Project Pipeline
 *
 * Demonstrates a three-tier agent hierarchy:
 *   Manager (plan) -> FORK(
 *     [Backend Lead -> API Worker -> DB Worker],
 *     [Frontend Lead -> UI Worker -> Styling Worker]
 *   ) -> JOIN -> Manager (merge)
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/hierarchical-agents-1.0.0.jar
 */
public class HierarchicalAgentsExample {

    private static final List<String> TASK_NAMES = List.of(
            "hier_manager_plan",
            "hier_lead_backend",
            "hier_worker_api",
            "hier_worker_db",
            "hier_lead_frontend",
            "hier_worker_ui",
            "hier_worker_styling",
            "hier_manager_merge"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new ManagerPlanWorker(),
                new LeadBackendWorker(),
                new WorkerApiWorker(),
                new WorkerDbWorker(),
                new LeadFrontendWorker(),
                new WorkerUiWorker(),
                new WorkerStylingWorker(),
                new ManagerMergeWorker()
        );
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Hierarchical Agents: Manager-Lead-Worker Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'hierarchical_agents'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Use the Conductor CLI or UI to start workflows.\n");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Allow workers to start polling
        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting hierarchical agents workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("project", "TaskManager Pro");
        input.put("deadline", "2 weeks");

        String workflowId = client.startWorkflow("hierarchical_agents", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            Map<String, Object> report = (Map<String, Object>) output.get("projectReport");
            if (report != null) {
                System.out.println("--- Project Report ---");
                System.out.println("  Status          : " + report.get("status"));
                System.out.println("  Total lines     : " + report.get("totalLinesOfCode"));

                Map<String, Object> hierarchy = (Map<String, Object>) report.get("hierarchy");
                if (hierarchy != null) {
                    System.out.println("  Hierarchy       : " + hierarchy.get("manager") + " manager, "
                            + hierarchy.get("leads") + " leads, " + hierarchy.get("workers") + " workers");
                }
            }
        }

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            workflow.getTasks().stream()
                    .filter(t -> t.getStatus().name().equals("FAILED"))
                    .forEach(t -> System.out.println("  Failed task: " + t.getReferenceTaskName()
                            + " — " + t.getReasonForIncompletion()));
            System.exit(1);
        }
    }
}
