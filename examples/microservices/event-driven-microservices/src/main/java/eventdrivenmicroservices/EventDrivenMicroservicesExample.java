package eventdrivenmicroservices;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventdrivenmicroservices.workers.*;
import java.util.List;
import java.util.Map;

public class EventDrivenMicroservicesExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 307: Event-Driven Microservices ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("edm_emit_event", "edm_process_event", "edm_update_projection", "edm_notify_subscribers"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new EmitEventWorker(), new ProcessEventWorker(), new UpdateProjectionWorker(), new NotifySubscribersWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("event_driven_microservices", 1,
                Map.of("eventType", "ORDER_PLACED", "payload", Map.of("orderId", "ORD-789", "amount", 150), "source", "order-service"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
