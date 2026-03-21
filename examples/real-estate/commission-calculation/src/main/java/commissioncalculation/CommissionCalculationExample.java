package commissioncalculation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import commissioncalculation.workers.*;
import java.util.List;
import java.util.Map;
public class CommissionCalculationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 688: Commission Calculation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cmc_base","cmc_tiers","cmc_deductions","cmc_finalize"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new BaseCommissionWorker(),new TiersWorker(),new DeductionsWorker(),new FinalizeWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cmc_commission_calculation",1,Map.of("agentId","AGT-200","salePrice",525000,"transactionId","TXN-688"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Net commission: "+wf.getOutput().get("netCommission")+"\n  Payment ID: "+wf.getOutput().get("paymentId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
