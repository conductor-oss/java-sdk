package compliancevendor;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import compliancevendor.workers.*;
import java.util.*;
public class ComplianceVendorExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 665: Vendor Compliance ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("vcm_assess","vcm_audit","vcm_certify","vcm_monitor"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AssessWorker(), new AuditWorker(), new CertifyWorker(), new MonitorWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("vcm_vendor_compliance", 1,
                Map.of("vendorId","VND-665-001","vendorName","SecureData Partners","complianceStandard","ISO-27001"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
