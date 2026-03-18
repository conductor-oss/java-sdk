package escalationchain;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import escalationchain.workers.EscFinalizeWorker;
import escalationchain.workers.EscSubmitWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Level Escalation Chain: Analyst -> Manager -> VP
 *
 * Demonstrates a multi-step escalation pattern using a WAIT task.
 * The workflow submits a request, pauses at a WAIT task, and the
 * main loop demonstrates an escalation chain where each level has a
 * timeout. If the current level does not respond, it escalates to
 * the next level. When a level responds, the WAIT task is completed
 * with the decision and the workflow finalizes.
 *
 * Flow:
 *   esc_submit (SIMPLE) -> esc_approval (WAIT) -> esc_finalize (SIMPLE)
 *
 * Run:
 *   java -jar target/escalation-chain-1.0.0.jar
 *   java -jar target/escalation-chain-1.0.0.jar --workers
 */
public class EscalationChainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Level Escalation Chain: Analyst -> Manager -> VP ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef escSubmitTask = new TaskDef();
        escSubmitTask.setName("esc_submit");
        escSubmitTask.setRetryCount(0);
        escSubmitTask.setTimeoutSeconds(60);
        escSubmitTask.setResponseTimeoutSeconds(30);
        escSubmitTask.setOwnerEmail("examples@orkes.io");

        TaskDef escFinalizeTask = new TaskDef();
        escFinalizeTask.setName("esc_finalize");
        escFinalizeTask.setRetryCount(0);
        escFinalizeTask.setTimeoutSeconds(60);
        escFinalizeTask.setResponseTimeoutSeconds(30);
        escFinalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(escSubmitTask, escFinalizeTask));

        System.out.println("  Registered: esc_submit, esc_finalize");
        System.out.println("  (WAIT is a system task -- no task definition needed)\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'escalation_chain_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new EscSubmitWorker(), new EscFinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("escalation_chain_demo", 1,
                Map.of("requestId", "REQ-CHAIN"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for the WAIT task to be reached
        System.out.println("Step 5: Waiting for WAIT task to be reached...");
        Task waitTask = null;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15000) {
            Workflow wf = client.getWorkflow(workflowId);
            for (Task t : wf.getTasks()) {
                if ("wait_ref".equals(t.getReferenceTaskName())
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

        // Step 6 -- Run escalation chain: Analyst -> Manager -> VP
        System.out.println("Step 6: Running escalation chain...\n");

        record EscalationLevel(String name, String email, long timeoutMs, boolean responds) {}
        List<EscalationLevel> levels = List.of(
                new EscalationLevel("Analyst", "analyst@co.com", 1500, false),
                new EscalationLevel("Manager", "manager@co.com", 1500, false),
                new EscalationLevel("VP", "vp@co.com", 1500, true)
        );

        for (int i = 0; i < levels.size(); i++) {
            EscalationLevel level = levels.get(i);
            System.out.println("  Level: " + level.name() + " (" + level.email() + ")");
            System.out.println("  Waiting " + level.timeoutMs() + "ms for response...");
            Thread.sleep(level.timeoutMs());

            if (level.responds()) {
                System.out.println("  " + level.name() + " responds -- approved!\n");

                TaskResult waitResult = new TaskResult();
                waitResult.setWorkflowInstanceId(workflowId);
                waitResult.setTaskId(waitTask.getTaskId());
                waitResult.setStatus(TaskResult.Status.COMPLETED);
                waitResult.getOutputData().put("decision", "approved");
                waitResult.getOutputData().put("respondedAt", level.name());
                waitResult.getOutputData().put("respondedBy", level.email());
                waitResult.getOutputData().put("escalationLevel", i + 1);

                client.getTaskClient().updateTask(waitResult);
                break;
            } else {
                System.out.println("  No response -- escalating to next level\n");
            }
        }

        // Step 7 -- Wait for workflow completion
        System.out.println("Step 7: Waiting for workflow completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 15000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Decision: " + workflow.getOutput().get("decision"));
        System.out.println("  Responded at: " + workflow.getOutput().get("respondedAt") + " level");

        client.stopWorkers();

        System.out.println("\n--- Escalation chain pattern ---");
        System.out.println("  Level 1: Analyst    (respond within 4 hours)");
        System.out.println("  Level 2: Manager    (respond within 8 hours)");
        System.out.println("  Level 3: VP         (respond within 24 hours)");
        System.out.println("  Level 4: Auto-approve (if nobody responds in 48 hours)");

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
