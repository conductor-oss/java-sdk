package customsclearance;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import customsclearance.workers.*;
import java.util.*;
public class CustomsClearanceExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 666: Customs Clearance ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cst_declare","cst_validate","cst_calculate_duty","cst_clear","cst_release"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DeclareWorker(), new ValidateWorker(), new CalculateDutyWorker(), new ClearWorker(), new ReleaseWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cst_customs_clearance", 1,
                Map.of("shipmentId","SHP-666-INT","origin","Shanghai, CN","destination","Los Angeles, US",
                       "goods",List.of(Map.of("description","Electronic Components","hsCode","8542","value",45000),Map.of("description","Packaging Materials","hsCode","4819","value",5000))));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
