package outboxpattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import outboxpattern.workers.*;
import java.util.List;
import java.util.Map;

public class OutboxPatternExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 328: Outbox Pattern ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ob_write_with_outbox", "ob_poll_outbox", "ob_publish_event", "ob_mark_published"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new WriteWithOutboxWorker(), new PollOutboxWorker(), new PublishEventWorker(), new MarkPublishedWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("outbox_pattern_workflow", 1,
                Map.of("entityId", "ORD-500", "entityData", Map.of("total", 250), "eventType", "ORDER_CREATED"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Published: " + wf.getOutput().get("published"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
