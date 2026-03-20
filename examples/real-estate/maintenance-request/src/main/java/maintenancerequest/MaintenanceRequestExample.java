package maintenancerequest;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import maintenancerequest.workers.*;
import java.util.List;
import java.util.Map;
public class MaintenanceRequestExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 686: Maintenance Request ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mtr_submit","mtr_classify","mtr_assign","mtr_complete","mtr_invoice"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SubmitWorker(),new ClassifyWorker(),new AssignWorker(),new CompleteWorker(),new InvoiceWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("mtr_maintenance_request",1,Map.of("tenantId","TNT-60","propertyId","UNIT-3C","description","Kitchen sink leaking"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Request ID: "+wf.getOutput().get("requestId")+"\n  Total cost: "+wf.getOutput().get("totalCost"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
