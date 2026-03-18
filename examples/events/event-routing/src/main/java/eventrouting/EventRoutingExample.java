package eventrouting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventrouting.workers.ReceiveEventWorker;
import eventrouting.workers.ExtractTypeWorker;
import eventrouting.workers.UserProcessorWorker;
import eventrouting.workers.OrderProcessorWorker;
import eventrouting.workers.SystemProcessorWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Routing Demo
 *
 * Demonstrates event-based routing using a SWITCH task: receive an event,
 * extract its domain/subType, then route to the appropriate processor
 * (user, order, or system as default).
 *   eo_receive_event -> eo_extract_type -> SWITCH(domain) {
 *       user  -> eo_user_processor
 *       order -> eo_order_processor
 *       *     -> eo_system_processor
 *   }
 *
 * Run:
 *   java -jar target/event-routing-1.0.0.jar
 */
public class EventRoutingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Routing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "eo_receive_event", "eo_extract_type",
                "eo_user_processor", "eo_order_processor",
                "eo_system_processor"));
        System.out.println("  Registered: eo_receive_event, eo_extract_type, eo_user_processor, eo_order_processor, eo_system_processor\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_routing_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveEventWorker(),
                new ExtractTypeWorker(),
                new UserProcessorWorker(),
                new OrderProcessorWorker(),
                new SystemProcessorWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_routing_wf", 1,
                Map.of("eventId", "evt-fixed-001",
                        "eventDomain", "user.profile_update",
                        "eventData", Map.of(
                                "userId", "U-3301",
                                "action", "profile_update",
                                "changes", Map.of(
                                        "displayName", "Alice Johnson",
                                        "avatar", "new-avatar.png"))));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
