package eventsourcing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventsourcing.workers.*;
import java.util.List;
import java.util.Map;

public class EventSourcingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 309: Event Sourcing ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("es_validate_event", "es_append_event", "es_rebuild_state", "es_publish_event"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ValidateEventWorker(), new AppendEventWorker(), new RebuildStateWorker(), new PublishEventWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("event_sourcing_workflow", 1,
                Map.of("aggregateId", "ACCT-200", "eventType", "DEPOSIT", "eventData", Map.of("amount", 500)));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
