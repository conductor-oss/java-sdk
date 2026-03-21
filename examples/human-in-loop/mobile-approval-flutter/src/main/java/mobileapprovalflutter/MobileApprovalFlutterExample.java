package mobileapprovalflutter;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import mobileapprovalflutter.workers.MobFinalizeWorker;
import mobileapprovalflutter.workers.MobSendPushWorker;
import mobileapprovalflutter.workers.MobSubmitWorker;

import java.util.List;
import java.util.Map;

/**
 * Mobile Approval with Push Notifications
 *
 * Demonstrates a human-in-the-loop workflow where:
 * 1. A request is submitted (mob_submit)
 * 2. A push notification is sent via FCM (mob_send_push)
 * 3. The workflow WAITs for a mobile response (WAIT task)
 * 4. Once the user responds, the result is finalized (mob_finalize)
 *
 * Run:
 *   java -jar target/mobile-approval-flutter-1.0.0.jar
 *   java -jar target/mobile-approval-flutter-1.0.0.jar --workers
 */
public class MobileApprovalFlutterExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Mobile Approval with Push Notifications Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef submitTask = new TaskDef();
        submitTask.setName("mob_submit");
        submitTask.setTimeoutSeconds(60);
        submitTask.setResponseTimeoutSeconds(30);
        submitTask.setOwnerEmail("examples@orkes.io");

        TaskDef sendPushTask = new TaskDef();
        sendPushTask.setName("mob_send_push");
        sendPushTask.setTimeoutSeconds(60);
        sendPushTask.setResponseTimeoutSeconds(30);
        sendPushTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("mob_finalize");
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(submitTask, sendPushTask, finalizeTask));
        System.out.println("  Registered: mob_submit, mob_send_push, mob_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'mobile_approval_flutter'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MobSubmitWorker(),
                new MobSendPushWorker(),
                new MobFinalizeWorker()
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
        String workflowId = client.startWorkflow("mobile_approval_flutter", 1,
                Map.of("requestId", "REQ-001", "userId", "user-42"));
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  Workflow will WAIT for mobile response (WAIT task).\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        System.out.println("  (In production, the mobile app would complete the WAIT task)\n");
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
