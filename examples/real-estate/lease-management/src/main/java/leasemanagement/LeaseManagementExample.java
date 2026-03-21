package leasemanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import leasemanagement.workers.*;
import java.util.List;
import java.util.Map;
public class LeaseManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 685: Lease Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lse_create","lse_sign","lse_activate","lse_renew","lse_terminate"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CreateLeaseWorker(),new SignLeaseWorker(),new ActivateLeaseWorker(),new RenewLeaseWorker(),new TerminateLeaseWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("lse_lease_management",1,Map.of("tenantId","TNT-50","propertyId","UNIT-5A","action","renew"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Lease ID: "+wf.getOutput().get("leaseId")+"\n  Action: "+wf.getOutput().get("action"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
