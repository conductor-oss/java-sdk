package systemtasks;

import com.netflix.conductor.common.run.Workflow;

import java.util.List;
import java.util.Map;

/**
 * System Tasks: INLINE and JSON_JQ_TRANSFORM Without Workers
 *
 * Demonstrates system tasks that run directly on the Conductor server.
 * No workers are needed — the server executes all task logic itself.
 *
 * System tasks used:
 *   - INLINE: Executes JavaScript expressions to look up user data and calculate bonuses
 *   - JSON_JQ_TRANSFORM: Uses JQ expressions to reshape and format output
 *
 * This is useful when you need lightweight data transformations, calculations,
 * or formatting within a workflow without deploying any worker processes.
 *
 * Run:
 *   java -jar target/system-tasks-1.0.0.jar
 */
public class SystemTasksExample {

    private static final String WORKFLOW_NAME = "system_tasks_demo";
    private static final int WORKFLOW_VERSION = 1;

    private static final List<String> USER_IDS = List.of("user-1", "user-2", "user-3");

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== System Tasks: INLINE and JSON_JQ_TRANSFORM Without Workers ===\n");

        // Explain the key concept
        System.out.println("KEY CONCEPT: System tasks run on the Conductor server itself.");
        System.out.println("No workers are needed — no polling, no task runners, no worker processes.");
        System.out.println("INLINE tasks execute JavaScript, JSON_JQ_TRANSFORM reshapes data with JQ.\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register workflow (no task defs needed for system tasks)
        System.out.println("Step 1: Registering workflow '" + WORKFLOW_NAME + "'...");
        System.out.println("  (No task definitions needed — system tasks are built into Conductor)");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        if (workersOnly) {
            System.out.println("Worker-only mode: No workers to start!");
            System.out.println("System tasks don't use workers — they execute on the Conductor server.");
            System.out.println("Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Step 2 — Start workflows for each user
        System.out.println("Step 2: Starting workflows for " + USER_IDS.size() + " users...\n");

        boolean allPassed = true;

        for (String userId : USER_IDS) {
            System.out.println("  --- Processing " + userId + " ---");

            String workflowId = client.startWorkflow(WORKFLOW_NAME, WORKFLOW_VERSION,
                    Map.of("userId", userId));
            System.out.println("  Workflow ID: " + workflowId);

            // Step 3 — Wait for completion (should be near-instant for system tasks)
            Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
            String status = workflow.getStatus().name();
            System.out.println("  Status: " + status);

            if ("COMPLETED".equals(status)) {
                System.out.println("  Output: " + workflow.getOutput());
            } else {
                System.out.println("  FAILED — check Conductor UI for details");
                allPassed = false;
            }
            System.out.println();
        }

        // Summary
        System.out.println("=== Summary ===");
        System.out.println("Tasks executed: 3 per workflow (lookup_user, calculate_bonus, format_output)");
        System.out.println("Workers used: 0 (all system tasks)");
        System.out.println("Task types: INLINE (JavaScript), JSON_JQ_TRANSFORM (JQ)");

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
