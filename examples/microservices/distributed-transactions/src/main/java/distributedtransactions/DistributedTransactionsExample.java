package distributedtransactions;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import distributedtransactions.workers.*;
import java.util.List;
import java.util.Map;

public class DistributedTransactionsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 329: Distributed Transactions ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dtx_prepare_order", "dtx_prepare_payment", "dtx_prepare_inventory", "dtx_commit_all", "dtx_rollback_all"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new PrepareOrderWorker(), new PreparePaymentWorker(), new PrepareInventoryWorker(), new CommitAllWorker(), new RollbackAllWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("distributed_transaction_workflow", 1,
                Map.of("orderId", "ORD-700", "items", List.of("item-a", "item-b"), "paymentMethod", "credit-card"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Committed: " + wf.getOutput().get("committed"));
        System.out.println("  Transaction: " + wf.getOutput().get("transactionId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
