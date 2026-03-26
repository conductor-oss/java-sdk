package waitrestapi;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import waitrestapi.workers.HandleDecisionWorker;
import waitrestapi.workers.PrepareWorker;

import java.util.List;
import java.util.Map;

/**
 * Complete WAIT Task via REST API — Human-in-the-Loop Approval
 *
 * Demonstrates a workflow with a WAIT task that pauses execution until
 * an external signal (REST API call) completes it with a decision.
 * A SWITCH task then routes based on the decision (approved/rejected).
 *
 * Flow: prepare -> WAIT (approval gate) -> SWITCH (approved/rejected) -> handle_decision
 *
 * Run:
 *   java -jar target/wait-rest-api-1.0.0.jar
 *   java -jar target/wait-rest-api-1.0.0.jar --workers
 */
public class WaitRestApiExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Complete WAIT Task via REST API Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareTask = new TaskDef();
        prepareTask.setName("wapi_prepare");
        prepareTask.setRetryCount(0);
        prepareTask.setTimeoutSeconds(60);
        prepareTask.setResponseTimeoutSeconds(30);
        prepareTask.setOwnerEmail("examples@orkes.io");

        TaskDef handleDecisionTask = new TaskDef();
        handleDecisionTask.setName("wapi_handle_decision");
        handleDecisionTask.setRetryCount(0);
        handleDecisionTask.setTimeoutSeconds(60);
        handleDecisionTask.setResponseTimeoutSeconds(30);
        handleDecisionTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareTask, handleDecisionTask));

        System.out.println("  Registered: wapi_prepare, wapi_handle_decision\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'wait_rest_api_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PrepareWorker(), new HandleDecisionWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // === Scenario 1: Approve ===
        System.out.println("=== Scenario 1: Approve via REST API ===\n");

        System.out.println("Step 4a: Starting workflow...");
        String approveWorkflowId = client.startWorkflow("wait_rest_api_demo", 1, Map.of());
        System.out.println("  Workflow ID: " + approveWorkflowId + "\n");

        // Wait for workflow to reach WAIT task
        Thread.sleep(3000);

        // Find the WAIT task and complete it with "approved"
        System.out.println("Step 5a: Completing WAIT task with decision=approved...");
        Workflow approveWf = client.getWorkflowClient().getWorkflow(approveWorkflowId, true);
        Task waitTask = findTaskByRefName(approveWf, "wait_ref");
        if (waitTask != null) {
            TaskResult taskResult = new TaskResult();
            taskResult.setWorkflowInstanceId(approveWorkflowId);
            taskResult.setTaskId(waitTask.getTaskId());
            taskResult.setStatus(TaskResult.Status.COMPLETED);
            taskResult.getOutputData().put("decision", "approved");
            client.getTaskClient().updateTask(taskResult);
            System.out.println("  WAIT task completed with decision=approved\n");
        }

        System.out.println("Step 6a: Waiting for workflow completion...");
        Workflow approveResult = client.waitForWorkflow(approveWorkflowId, "COMPLETED", 30000);
        System.out.println("  Status: " + approveResult.getStatus().name());
        System.out.println("  Output: " + approveResult.getOutput() + "\n");

        // === Scenario 2: Reject ===
        System.out.println("=== Scenario 2: Reject via REST API ===\n");

        System.out.println("Step 4b: Starting workflow...");
        String rejectWorkflowId = client.startWorkflow("wait_rest_api_demo", 1, Map.of());
        System.out.println("  Workflow ID: " + rejectWorkflowId + "\n");

        // Wait for workflow to reach WAIT task
        Thread.sleep(3000);

        // Find the WAIT task and complete it with "rejected"
        System.out.println("Step 5b: Completing WAIT task with decision=rejected...");
        Workflow rejectWf = client.getWorkflowClient().getWorkflow(rejectWorkflowId, true);
        Task waitTask2 = findTaskByRefName(rejectWf, "wait_ref");
        if (waitTask2 != null) {
            TaskResult taskResult = new TaskResult();
            taskResult.setWorkflowInstanceId(rejectWorkflowId);
            taskResult.setTaskId(waitTask2.getTaskId());
            taskResult.setStatus(TaskResult.Status.COMPLETED);
            taskResult.getOutputData().put("decision", "rejected");
            client.getTaskClient().updateTask(taskResult);
            System.out.println("  WAIT task completed with decision=rejected\n");
        }

        System.out.println("Step 6b: Waiting for workflow completion...");
        Workflow rejectResult = client.waitForWorkflow(rejectWorkflowId, "COMPLETED", 30000);
        System.out.println("  Status: " + rejectResult.getStatus().name());
        System.out.println("  Output: " + rejectResult.getOutput() + "\n");

        client.stopWorkers();

        boolean passed = "COMPLETED".equals(approveResult.getStatus().name())
                && "COMPLETED".equals(rejectResult.getStatus().name());

        if (passed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }

    private static Task findTaskByRefName(Workflow workflow, String refName) {
        return workflow.getTasks().stream()
                .filter(t -> refName.equals(t.getReferenceTaskName()))
                .findFirst()
                .orElse(null);
    }
}
