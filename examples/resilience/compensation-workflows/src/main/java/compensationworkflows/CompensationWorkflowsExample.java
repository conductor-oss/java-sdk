package compensationworkflows;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import compensationworkflows.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Compensation Workflows -- Undo Completed Steps
 *
 * When a workflow step fails, run a separate compensation workflow
 * to undo all previously completed steps in reverse order.
 *
 * Main workflow: Step A -> Step B -> Step C (C can fail based on failAtStep input)
 * Compensation workflow: Undo B -> Undo A (reverse order, skip C since it never completed)
 *
 * Run:
 *   java -jar target/compensation-workflows-1.0.0.jar
 *   java -jar target/compensation-workflows-1.0.0.jar --workers
 */
public class CompensationWorkflowsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Compensation Workflows: Undo Completed Steps ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        List<TaskDef> taskDefs = List.of(
                createTaskDef("comp_step_a"),
                createTaskDef("comp_step_b"),
                createTaskDef("comp_step_c"),
                createTaskDef("comp_undo_a"),
                createTaskDef("comp_undo_b")
        );
        client.registerTaskDefs(taskDefs);
        System.out.println("  Registered 5 task definitions.\n");

        // Step 2 -- Register both workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Registered: compensatable_workflow");
        client.registerWorkflow("compensation-workflow.json");
        System.out.println("  Registered: compensation_workflow\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CompStepAWorker(),
                new CompStepBWorker(),
                new CompStepCWorker(),
                new CompUndoBWorker(),
                new CompUndoAWorker()
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

        // Scenario 1 -- All steps succeed
        System.out.println("--- Scenario 1: All steps succeed ---");
        String wfId1 = client.startWorkflow("compensatable_workflow", 1,
                Map.of("failAtStep", "none"));
        System.out.println("  Workflow ID: " + wfId1);
        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Output: " + wf1.getOutput() + "\n");

        // Scenario 2 -- Step C fails, then run compensation
        System.out.println("--- Scenario 2: Step C fails -> run compensation ---");
        String wfId2 = client.startWorkflow("compensatable_workflow", 1,
                Map.of("failAtStep", "C"));
        System.out.println("  Workflow ID: " + wfId2);
        Workflow wf2 = client.waitForWorkflow(wfId2, "FAILED", 30000);
        System.out.println("  Main workflow: " + wf2.getStatus().name());

        System.out.println("\n  Starting compensation workflow...");
        String compId = client.startWorkflow("compensation_workflow", 1, Map.of(
                "failedWorkflowId", wfId2,
                "stepAResult", "resource-A-created",
                "stepBResult", "record-B-inserted"
        ));
        Workflow compWf = client.waitForWorkflow(compId, "COMPLETED", 30000);
        System.out.println("  Compensation: " + compWf.getStatus().name());

        System.out.println("\n--- Compensation Pattern ---");
        System.out.println("  Forward: Step A -> Step B -> Step C (fails)");
        System.out.println("  Reverse: Undo B -> Undo A (skip C, it never completed)");
        System.out.println("  Key: compensation runs in reverse order of completed steps");

        client.stopWorkers();

        boolean passed = "COMPLETED".equals(wf1.getStatus().name())
                && "FAILED".equals(wf2.getStatus().name())
                && "COMPLETED".equals(compWf.getStatus().name());

        if (passed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }

    private static TaskDef createTaskDef(String name) {
        TaskDef def = new TaskDef();
        def.setName(name);
        def.setRetryCount(0);
        def.setTimeoutSeconds(60);
        def.setResponseTimeoutSeconds(30);
        def.setOwnerEmail("examples@orkes.io");
        return def;
    }
}
