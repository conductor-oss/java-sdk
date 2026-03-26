package slackapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import slackapproval.workers.FinalizeWorker;
import slackapproval.workers.PostSlackWorker;
import slackapproval.workers.SubmitWorker;

import java.util.List;
import java.util.Map;

/**
 * Slack Interactive Button Approval -- human-in-the-loop workflow
 *
 * Demonstrates a Conductor workflow that:
 * 1. Submits a request (sa_submit)
 * 2. Posts a Slack Block Kit message with approve/reject buttons (sa_post_slack)
 * 3. WAITs for a human to click a button in Slack (slack_response WAIT task)
 * 4. Finalizes the decision (sa_finalize)
 *
 * In production, a Slack webhook handler would call the Conductor API to
 * complete the WAIT task with the user's decision.
 *
 * Run:
 *   java -jar target/slack-approval-1.0.0.jar
 *   java -jar target/slack-approval-1.0.0.jar --workers
 */
public class SlackApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Slack Interactive Button Approval Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef submitTask = new TaskDef();
        submitTask.setName("sa_submit");
        submitTask.setRetryCount(0);
        submitTask.setTimeoutSeconds(60);
        submitTask.setResponseTimeoutSeconds(30);
        submitTask.setOwnerEmail("examples@orkes.io");

        TaskDef postSlackTask = new TaskDef();
        postSlackTask.setName("sa_post_slack");
        postSlackTask.setRetryCount(0);
        postSlackTask.setTimeoutSeconds(60);
        postSlackTask.setResponseTimeoutSeconds(30);
        postSlackTask.setOwnerEmail("examples@orkes.io");

        TaskDef waitTask = new TaskDef();
        waitTask.setName("slack_response");
        waitTask.setRetryCount(0);
        waitTask.setTimeoutSeconds(3600);
        waitTask.setResponseTimeoutSeconds(3600);
        waitTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("sa_finalize");
        finalizeTask.setRetryCount(0);
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(submitTask, postSlackTask, waitTask, finalizeTask));

        System.out.println("  Registered: sa_submit, sa_post_slack, slack_response (WAIT), sa_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'slack_approval_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SubmitWorker(),
                new PostSlackWorker(),
                new FinalizeWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("slack_approval_demo", 1,
                Map.of(
                        "requestor", "alice@example.com",
                        "reason", "Access to production database",
                        "channel", "#approvals"
                ));
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  The workflow will pause at the WAIT task (slack_response).");
        System.out.println("  In production, a Slack webhook would complete this task.\n");

        // Step 5 -- Wait for the workflow to reach the WAIT state
        System.out.println("Step 5: Waiting for workflow to reach WAIT state...");
        Workflow workflow = client.waitForWorkflow(workflowId, "RUNNING", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        if ("RUNNING".equals(status)) {
            System.out.println("  Workflow is paused at WAIT task, awaiting Slack interaction.");
            System.out.println("\n  To perform approval, use the Conductor API:");
            System.out.println("  POST /api/tasks/{taskId} with { \"status\": \"COMPLETED\", \"outputData\": { \"decision\": \"approved\" } }");
        }

        client.stopWorkers();

        System.out.println("\nResult: " + ("RUNNING".equals(status) ? "PASSED (waiting for human input)" : status));
        System.exit("RUNNING".equals(status) ? 0 : 1);
    }
}
