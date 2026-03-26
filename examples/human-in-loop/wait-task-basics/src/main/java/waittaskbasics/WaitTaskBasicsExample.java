package waittaskbasics;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import waittaskbasics.workers.WaitAfterWorker;
import waittaskbasics.workers.WaitBeforeWorker;

import java.util.List;
import java.util.Map;

/**
 * WAIT Task Basics — Pause and Resume a Workflow
 *
 * Demonstrates Conductor's WAIT system task. The workflow runs:
 *   wait_before (SIMPLE) -> WAIT task -> wait_after (SIMPLE)
 *
 * The WAIT task pauses the workflow until it is completed externally
 * via the Task API. This models human-in-the-loop approvals, external
 * callbacks, or any scenario requiring an external signal.
 *
 * Run:
 *   java -jar target/wait-task-basics-1.0.0.jar
 *   java -jar target/wait-task-basics-1.0.0.jar --workers
 */
public class WaitTaskBasicsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== WAIT Task Basics: Pause and Resume a Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions for the SIMPLE tasks
        System.out.println("Step 1: Registering task definitions...");

        TaskDef waitBeforeTask = new TaskDef();
        waitBeforeTask.setName("wait_before");
        waitBeforeTask.setRetryCount(0);
        waitBeforeTask.setTimeoutSeconds(60);
        waitBeforeTask.setResponseTimeoutSeconds(30);
        waitBeforeTask.setOwnerEmail("examples@orkes.io");

        TaskDef waitAfterTask = new TaskDef();
        waitAfterTask.setName("wait_after");
        waitAfterTask.setRetryCount(0);
        waitAfterTask.setTimeoutSeconds(60);
        waitAfterTask.setResponseTimeoutSeconds(30);
        waitAfterTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(waitBeforeTask, waitAfterTask));

        System.out.println("  Registered: wait_before, wait_after");
        System.out.println("  (WAIT is a system task — no task definition needed)\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'wait_task_basics_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new WaitBeforeWorker(), new WaitAfterWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wait_task_basics_demo", 1,
                Map.of("requestId", "req-12345"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for the WAIT task to be reached
        System.out.println("Step 5: Waiting for WAIT task to be reached...");
        Task waitTask = null;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            Workflow wf = client.getWorkflow(workflowId);
            for (Task t : wf.getTasks()) {
                if ("wait_for_approval_ref".equals(t.getReferenceTaskName())
                        && Task.Status.IN_PROGRESS.equals(t.getStatus())) {
                    waitTask = t;
                    break;
                }
            }
            if (waitTask != null) break;
            Thread.sleep(500);
        }

        if (waitTask == null) {
            System.out.println("  ERROR: WAIT task not reached within timeout.");
            client.stopWorkers();
            System.exit(1);
        }
        System.out.println("  WAIT task reached (taskId: " + waitTask.getTaskId() + ")\n");

        // Step 6 — Complete the WAIT task externally (perform approval)
        System.out.println("Step 6: Completing WAIT task externally (performing approval)...");
        TaskResult waitResult = new TaskResult();
        waitResult.setWorkflowInstanceId(workflowId);
        waitResult.setTaskId(waitTask.getTaskId());
        waitResult.setStatus(TaskResult.Status.COMPLETED);
        waitResult.getOutputData().put("approval", "approved");
        waitResult.getOutputData().put("approvedBy", "admin");

        client.getTaskClient().updateTask(waitResult);
        System.out.println("  WAIT task completed with approval=approved\n");

        // Step 7 — Wait for workflow completion
        System.out.println("Step 7: Waiting for workflow completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 15000);
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
