package tenantscreening;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tenantscreening.workers.*;
import java.util.List;
import java.util.Map;
public class TenantScreeningExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 684: Tenant Screening ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tsc_apply","tsc_background","tsc_credit","tsc_decision"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ApplyWorker(),new BackgroundCheckWorker(),new CreditCheckWorker(),new DecisionWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tsc_tenant_screening",1,Map.of("applicantName","Jane Smith","propertyId","UNIT-4B","monthlyRent",1800));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Decision: "+wf.getOutput().get("decision")+"\n  Score: "+wf.getOutput().get("score"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
