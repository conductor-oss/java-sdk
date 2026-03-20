package waitsdk;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import waitsdk.workers.FinalizeWorker;
import waitsdk.workers.InitWorker;

import java.util.List;
import java.util.Map;

/**
 * Complete WAIT Task via SDK
 *
 * Demonstrates a human-in-the-loop pattern where a workflow pauses at a WAIT task
 * and is completed programmatically using the Conductor TaskClient SDK.
 *
 * Workflow: wsdk_init -> WAIT (wait_for_resolution) -> wsdk_finalize
 *
 * Run:
 *   java -jar target/wait-sdk-1.0.0.jar
 *   java -jar target/wait-sdk-1.0.0.jar --workers
 */
public class WaitSdkExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Complete WAIT Task via SDK Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef initTask = new TaskDef();
        initTask.setName("wsdk_init");
        initTask.setRetryCount(0);
        initTask.setTimeoutSeconds(60);
        initTask.setResponseTimeoutSeconds(30);
        initTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("wsdk_finalize");
        finalizeTask.setRetryCount(0);
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(initTask, finalizeTask));

        System.out.println("  Registered: wsdk_init, wsdk_finalize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'wait_sdk_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new InitWorker(), new FinalizeWorker());
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
        System.out.println("Step 4: Starting workflow with ticketId=TKT-001...\n");
        String workflowId = client.startWorkflow("wait_sdk_demo", 1,
                Map.of("ticketId", "TKT-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for workflow to reach the WAIT task
        System.out.println("Step 5: Waiting for workflow to reach WAIT task...");
        Thread.sleep(3000);

        Workflow running = client.getWorkflow(workflowId);
        System.out.println("  Workflow status: " + running.getStatus().name());

        // Find the WAIT task
        Task waitTask = null;
        for (Task t : running.getTasks()) {
            if ("wait_for_resolution".equals(t.getReferenceTaskName())) {
                waitTask = t;
                break;
            }
        }

        if (waitTask == null) {
            System.out.println("\nError: WAIT task not found in workflow.");
            client.stopWorkers();
            System.exit(1);
            return;
        }

        System.out.println("  WAIT task ID: " + waitTask.getTaskId());
        System.out.println("  WAIT task status: " + waitTask.getStatus() + "\n");

        // Step 6 — Complete the WAIT task via SDK
        System.out.println("Step 6: Completing WAIT task via TaskClient SDK...");
        TaskResult taskResult = new TaskResult();
        taskResult.setWorkflowInstanceId(workflowId);
        taskResult.setTaskId(waitTask.getTaskId());
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("resolvedBy", "sdk-automation");
        taskResult.getOutputData().put("resolution", "approved");

        client.getTaskClient().updateTask(taskResult);
        System.out.println("  WAIT task completed.\n");

        // Step 7 — Wait for workflow completion
        System.out.println("Step 7: Waiting for workflow to complete...");
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
