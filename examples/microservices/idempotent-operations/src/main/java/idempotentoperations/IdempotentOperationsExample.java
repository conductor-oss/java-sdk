package idempotentoperations;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import idempotentoperations.workers.*;
import java.util.List;
import java.util.Map;

public class IdempotentOperationsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 327: Idempotent Operations ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("io_generate_key", "io_check_duplicate", "io_execute", "io_record_completion"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new GenerateKeyWorker(), new CheckDuplicateWorker(), new ExecuteWorker(), new RecordCompletionWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("idempotent_operations_workflow", 1,
                Map.of("operationId", "OP-123", "action", "charge-payment", "data", Map.of("amount", 99.99)));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Duplicate: " + wf.getOutput().get("isDuplicate"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
