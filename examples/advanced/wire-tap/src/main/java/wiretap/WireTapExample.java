package wiretap;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import wiretap.workers.WtpReceiveWorker;
import wiretap.workers.WtpMainFlowWorker;
import wiretap.workers.WtpTapAuditWorker;

import java.util.List;
import java.util.Map;

/**
 * Wire Tap Demo
 *
 * Run:
 *   java -jar target/wiretap-1.0.0.jar
 */
public class WireTapExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Wire Tap Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wtp_receive",
                "wtp_main_flow",
                "wtp_tap_audit"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'wtp_wire_tap'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WtpReceiveWorker(),
                new WtpMainFlowWorker(),
                new WtpTapAuditWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wtp_wire_tap", 1,
                Map.of("message", java.util.Map.of("type", "payment", "amount", 299.99), "auditLevel", "detailed"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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