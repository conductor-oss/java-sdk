package messagebroker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import messagebroker.workers.MbrReceiveWorker;
import messagebroker.workers.MbrRouteWorker;
import messagebroker.workers.MbrDeliverWorker;
import messagebroker.workers.MbrAcknowledgeWorker;
import messagebroker.workers.MbrLogWorker;

import java.util.List;
import java.util.Map;

/**
 * Message Broker Demo
 *
 * Run:
 *   java -jar target/messagebroker-1.0.0.jar
 */
public class MessageBrokerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Message Broker Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mbr_receive",
                "mbr_route",
                "mbr_deliver",
                "mbr_acknowledge",
                "mbr_log"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'mbr_message_broker'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MbrReceiveWorker(),
                new MbrRouteWorker(),
                new MbrDeliverWorker(),
                new MbrAcknowledgeWorker(),
                new MbrLogWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("mbr_message_broker", 1,
                Map.of("message", java.util.Map.of("orderId", "ORD-814"), "topic", "orders", "priority", "high"));
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