package exceptionhandling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import exceptionhandling.workers.AnalyzeWorker;
import exceptionhandling.workers.AutoProcessWorker;
import exceptionhandling.workers.FinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Exception Handling — Auto-Process or Human Review Based on Risk
 *
 * Demonstrates a workflow that analyzes an item's risk score:
 * - If risk > 7, routes to a WAIT task for human review
 * - Otherwise, auto-processes the item
 * - Both paths converge at a finalize step
 *
 * Run:
 *   java -jar target/exception-handling-1.0.0.jar
 *   java -jar target/exception-handling-1.0.0.jar --workers
 */
public class ExceptionHandlingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Exception Handling Demo: Auto-Process or Human Review ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef analyzeDef = new TaskDef();
        analyzeDef.setName("eh_analyze");
        analyzeDef.setRetryCount(0);
        analyzeDef.setTimeoutSeconds(60);
        analyzeDef.setResponseTimeoutSeconds(30);
        analyzeDef.setOwnerEmail("examples@orkes.io");

        TaskDef autoProcessDef = new TaskDef();
        autoProcessDef.setName("eh_auto_process");
        autoProcessDef.setRetryCount(0);
        autoProcessDef.setTimeoutSeconds(60);
        autoProcessDef.setResponseTimeoutSeconds(30);
        autoProcessDef.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeDef = new TaskDef();
        finalizeDef.setName("eh_finalize");
        finalizeDef.setRetryCount(0);
        finalizeDef.setTimeoutSeconds(60);
        finalizeDef.setResponseTimeoutSeconds(30);
        finalizeDef.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(analyzeDef, autoProcessDef, finalizeDef));
        System.out.println("  Registered: eh_analyze, eh_auto_process, eh_finalize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'exception_handling_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AnalyzeWorker(),
                new AutoProcessWorker(),
                new FinalizeWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run low-risk scenario (auto-process path)
        System.out.println("Step 4: Starting workflow (risk=3, auto-process path)...\n");
        String workflowId = client.startWorkflow("exception_handling_demo", 1,
                Map.of("risk", 3));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
