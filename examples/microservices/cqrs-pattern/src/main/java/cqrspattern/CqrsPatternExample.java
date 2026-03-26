package cqrspattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cqrspattern.workers.*;
import java.util.List;
import java.util.Map;

public class CqrsPatternExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 308: CQRS Pattern ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cqrs_validate_command", "cqrs_persist_event", "cqrs_update_read_model", "cqrs_query_read_model"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ValidateCommandWorker(), new PersistEventWorker(), new UpdateReadModelWorker(), new QueryReadModelWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("cqrs_command_workflow", 1,
                Map.of("command", "UPDATE_ITEM", "aggregateId", "ITEM-100", "data", Map.of("price", 29.99)));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
