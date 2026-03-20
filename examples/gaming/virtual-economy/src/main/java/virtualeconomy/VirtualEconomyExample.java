package virtualeconomy;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import virtualeconomy.workers.*;
import java.util.List;
import java.util.Map;
/** Example 750: Virtual Economy — Transaction, Validate, Update Balance, Audit, Report */
public class VirtualEconomyExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 750: Virtual Economy ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("vec_transaction", "vec_validate", "vec_update_balance", "vec_audit", "vec_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TransactionWorker(), new ValidateWorker(), new UpdateBalanceWorker(), new AuditWorker(), new ReportWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("virtual_economy_750", 1, Map.of("playerId", "P-042", "type", "earn", "amount", 500, "currency", "gold_coins"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
