package optionaltasks;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import optionaltasks.workers.OptionalEnrichWorker;
import optionaltasks.workers.RequiredWorker;
import optionaltasks.workers.SummarizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Optional Task Demo — Task-Level Error Handling with Optional Flag
 *
 * Demonstrates Conductor's optional task feature where a task marked
 * "optional": true in the workflow definition will not cause the workflow
 * to fail if the task itself fails. The subsequent tasks can check
 * whether the optional task produced output and handle accordingly.
 *
 * Workflow: opt_required -> opt_optional_enrich (optional) -> opt_summarize
 *
 * Run:
 *   java -jar target/optional-tasks-1.0.0.jar
 *   java -jar target/optional-tasks-1.0.0.jar --workers
 */
public class OptionalTasksExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Optional Task Demo: Task-Level Error Handling ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef requiredTask = new TaskDef();
        requiredTask.setName("opt_required");
        requiredTask.setTimeoutSeconds(60);
        requiredTask.setResponseTimeoutSeconds(30);
        requiredTask.setOwnerEmail("examples@orkes.io");

        TaskDef enrichTask = new TaskDef();
        enrichTask.setName("opt_optional_enrich");
        enrichTask.setTimeoutSeconds(60);
        enrichTask.setResponseTimeoutSeconds(30);
        enrichTask.setOwnerEmail("examples@orkes.io");

        TaskDef summarizeTask = new TaskDef();
        summarizeTask.setName("opt_summarize");
        summarizeTask.setTimeoutSeconds(60);
        summarizeTask.setResponseTimeoutSeconds(30);
        summarizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(requiredTask, enrichTask, summarizeTask));

        System.out.println("  Registered: opt_required, opt_optional_enrich, opt_summarize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'optional_tasks_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RequiredWorker(),
                new OptionalEnrichWorker(),
                new SummarizeWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow with data='hello'...\n");
        String workflowId = client.startWorkflow("optional_tasks_demo", 1,
                Map.of("data", "hello"));
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
