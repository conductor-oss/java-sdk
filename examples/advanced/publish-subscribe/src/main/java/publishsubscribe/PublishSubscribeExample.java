package publishsubscribe;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import publishsubscribe.workers.PbsPublishWorker;
import publishsubscribe.workers.PbsSubscriber1Worker;
import publishsubscribe.workers.PbsSubscriber2Worker;
import publishsubscribe.workers.PbsSubscriber3Worker;
import publishsubscribe.workers.PbsConfirmWorker;

import java.util.List;
import java.util.Map;

/**
 * Publish-Subscribe Demo
 *
 * Run:
 *   java -jar target/publishsubscribe-1.0.0.jar
 */
public class PublishSubscribeExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Publish-Subscribe Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pbs_publish",
                "pbs_subscriber_1",
                "pbs_subscriber_2",
                "pbs_subscriber_3",
                "pbs_confirm"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'pbs_publish_subscribe'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PbsPublishWorker(),
                new PbsSubscriber1Worker(),
                new PbsSubscriber2Worker(),
                new PbsSubscriber3Worker(),
                new PbsConfirmWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("pbs_publish_subscribe", 1,
                Map.of("event", java.util.Map.of("type", "order_completed", "orderId", "ORD-888"), "topic", "order_events"));
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