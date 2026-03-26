package compliancenonprofit;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import compliancenonprofit.workers.*;
import java.util.List;
import java.util.Map;
/** Example 760: Nonprofit Compliance — Audit, Verify Filings, Check Requirements, Report, Submit */
public class ComplianceNonprofitExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 760: Nonprofit Compliance ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("cnp_audit", "cnp_verify_filings", "cnp_check_requirements", "cnp_report", "cnp_submit"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AuditWorker(), new VerifyFilingsWorker(), new CheckRequirementsWorker(), new ReportWorker(), new SubmitWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("compliance_nonprofit_760", 1, Map.of("organizationName", "HopeWorks Foundation", "fiscalYear", 2025, "ein", "12-3456789"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
