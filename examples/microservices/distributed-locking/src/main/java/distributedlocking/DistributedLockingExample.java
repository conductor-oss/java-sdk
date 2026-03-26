package distributedlocking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import distributedlocking.workers.*;
import java.util.List;
import java.util.Map;

public class DistributedLockingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 326: Distributed Locking ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dl_acquire_lock", "dl_execute_critical", "dl_release_lock"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new AcquireLockWorker(), new ExecuteCriticalWorker(), new ReleaseLockWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("distributed_locking_workflow", 1,
                Map.of("resourceId", "inventory-item-500", "operation", "decrement-stock", "ttlSeconds", 30));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Result: " + wf.getOutput().get("result"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
