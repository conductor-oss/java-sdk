package conductorui;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;
import conductorui.workers.StepOneWorker;
import conductorui.workers.StepTwoWorker;
import conductorui.workers.StepThreeWorker;

import java.util.List;
import java.util.Map;

/**
 * Conductor UI Guide — Monitoring and Debugging Workflows
 *
 * Creates a multi-step workflow designed to be explored through the Conductor UI.
 * After execution, prints a guide explaining how to inspect the workflow via
 * the UI and REST API.
 *
 * Run:
 *   java -jar target/conductor-ui-1.0.0.jar
 */
public class ConductorUiExample {

    private static final String WORKFLOW_NAME = "ui_demo_workflow";

    private static final List<String> TASK_NAMES = List.of(
            "ui_step_one", "ui_step_two", "ui_step_three");

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Conductor UI Guide: Monitoring and Debugging Workflows ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow '" + WORKFLOW_NAME + "'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new StepOneWorker(),
                new StepTwoWorker(),
                new StepThreeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow with sample input...");
        Map<String, Object> input = Map.of(
                "userId", "user-42",
                "action", "signup");
        System.out.println("  Input: " + input + "\n");
        String workflowId = client.startWorkflow(WORKFLOW_NAME, 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        // Step 6 — Print execution details from the API
        System.out.println("--- Execution Details (from API) ---\n");
        System.out.println("  Workflow ID:   " + workflow.getWorkflowId());
        System.out.println("  Workflow Name: " + workflow.getWorkflowName());
        System.out.println("  Status:        " + workflow.getStatus());
        System.out.println("  Input:         " + workflow.getInput());
        System.out.println("  Output:        " + workflow.getOutput());
        System.out.println();

        List<Task> tasks = workflow.getTasks();
        if (tasks != null) {
            System.out.println("  Tasks (" + tasks.size() + "):");
            for (Task task : tasks) {
                long durationMs = 0;
                if (task.getStartTime() > 0 && task.getEndTime() > 0) {
                    durationMs = task.getEndTime() - task.getStartTime();
                }
                System.out.println("    - " + task.getTaskDefName()
                        + " (ref: " + task.getReferenceTaskName() + ")");
                System.out.println("      Status:   " + task.getStatus());
                System.out.println("      Input:    " + task.getInputData());
                System.out.println("      Output:   " + task.getOutputData());
                System.out.println("      Duration: " + durationMs + " ms");
                System.out.println();
            }
        }

        // Step 7 — Print the UI exploration guide
        printUiGuide(workflowId);

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }

    private static void printUiGuide(String workflowId) {
        String baseUrl = System.getenv("CONDUCTOR_BASE_URL") != null
                ? System.getenv("CONDUCTOR_BASE_URL")
                : "http://localhost:8080/api";
        String uiUrl = baseUrl.replace("/api", "").replace(":8080", ":1234");

        System.out.println("=== UI Exploration Guide ===\n");

        System.out.println("Now that the workflow has completed, explore it in the Conductor UI:\n");

        System.out.println("1. WORKFLOW DEFINITIONS TAB");
        System.out.println("   Open the UI at: " + uiUrl);
        System.out.println("   Navigate to 'Workflow Definitions' to see '" + WORKFLOW_NAME + "'.");
        System.out.println("   You can view the workflow's JSON definition, its tasks, and wiring.\n");

        System.out.println("2. EXECUTIONS TAB");
        System.out.println("   Navigate to 'Executions' and search for this workflow.");
        System.out.println("   Workflow ID: " + workflowId);
        System.out.println("   You can filter by workflow name '" + WORKFLOW_NAME + "' or paste the ID.\n");

        System.out.println("3. TASK INSPECTION");
        System.out.println("   Click on the workflow execution to see the visual graph.");
        System.out.println("   Click on each task node to inspect:");
        System.out.println("   - Input:   What data was passed to the task");
        System.out.println("   - Output:  What the worker returned");
        System.out.println("   - Timing:  Start time, end time, and duration");
        System.out.println("   - Status:  SCHEDULED -> IN_PROGRESS -> COMPLETED");
        System.out.println("   - Logs:    Any worker logs attached to the task\n");

        System.out.println("4. REST API EQUIVALENTS");
        System.out.println("   You can retrieve the same data programmatically:\n");
        System.out.println("   Get workflow execution:");
        System.out.println("     GET " + baseUrl + "/workflow/" + workflowId + "?includeTasks=true\n");
        System.out.println("   Get workflow definition:");
        System.out.println("     GET " + baseUrl + "/metadata/workflow/" + WORKFLOW_NAME + "\n");
        System.out.println("   Search executions:");
        System.out.println("     GET " + baseUrl + "/workflow/search?query=workflowType=" + WORKFLOW_NAME + "\n");
    }
}
