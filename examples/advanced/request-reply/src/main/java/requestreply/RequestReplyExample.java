package requestreply;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import requestreply.workers.RqrSendRequestWorker;
import requestreply.workers.RqrWaitResponseWorker;
import requestreply.workers.RqrCorrelateWorker;
import requestreply.workers.RqrDeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Request-Reply Demo
 *
 * Run:
 *   java -jar target/requestreply-1.0.0.jar
 */
public class RequestReplyExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Request-Reply Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rqr_send_request",
                "rqr_wait_response",
                "rqr_correlate",
                "rqr_deliver"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'rqr_request_reply'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RqrSendRequestWorker(),
                new RqrWaitResponseWorker(),
                new RqrCorrelateWorker(),
                new RqrDeliverWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rqr_request_reply", 1,
                Map.of("requestPayload", java.util.Map.of("action", "check_inventory"), "targetService", "inventory_service", "timeoutMs", 5000));
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