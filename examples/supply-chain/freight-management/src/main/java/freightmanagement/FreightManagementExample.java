package freightmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import freightmanagement.workers.*;
import java.util.*;
public class FreightManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 667: Freight Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("frm_book","frm_track","frm_deliver","frm_invoice","frm_reconcile"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new BookWorker(), new TrackWorker(), new DeliverWorker(), new InvoiceWorker(), new ReconcileWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("frm_freight_management", 1,
                Map.of("origin","Detroit, MI","destination","Houston, TX","weight",2500,"carrier","FastFreight Express"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
