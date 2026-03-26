package sharednothingarchitecture;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sharednothingarchitecture.workers.*;
import java.util.List;
import java.util.Map;

public class SharedNothingArchitectureExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 315: Shared Nothing Architecture ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sn_service_a", "sn_service_b", "sn_service_c", "sn_aggregate"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ServiceAWorker(), new ServiceBWorker(), new ServiceCWorker(), new AggregateWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("shared_nothing_workflow", 1,
                Map.of("requestId", "REQ-700", "data", Map.of("value", 42)));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
