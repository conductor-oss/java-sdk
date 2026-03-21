package compliancemonitoring;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import compliancemonitoring.workers.*;
import java.util.List;
import java.util.Map;

public class ComplianceMonitoringExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 428: Compliance Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cpm_scan_resources", "cpm_evaluate_policies", "cpm_log_compliant", "cpm_remediate"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ScanResourcesWorker(), new EvaluatePoliciesWorker(), new LogCompliantWorker(), new RemediateWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("compliance_monitoring_428", 1, Map.of("framework","SOC2","scope","production","autoRemediate",true));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
