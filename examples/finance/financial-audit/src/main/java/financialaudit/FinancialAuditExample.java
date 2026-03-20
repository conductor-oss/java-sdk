package financialaudit;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import financialaudit.workers.*;
import java.util.List;
import java.util.Map;
public class FinancialAuditExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 508: Financial Audit ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("fau_define_scope","fau_collect_evidence","fau_test_controls","fau_generate_report","fau_remediate"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DefineScopeWorker(), new CollectEvidenceWorker(), new TestControlsWorker(), new GenerateReportWorker(), new RemediateWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("financial_audit_workflow", 1, Map.of("auditId","AUD-2026-0508","entityName","Acme Corp","auditType","annual_financial","fiscalYear",2025));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
