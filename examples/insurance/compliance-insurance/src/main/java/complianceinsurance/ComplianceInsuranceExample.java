package complianceinsurance;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import complianceinsurance.workers.AuditWorker;
import complianceinsurance.workers.AssessWorker;
import complianceinsurance.workers.FileReportsWorker;
import complianceinsurance.workers.TrackWorker;
import complianceinsurance.workers.CertifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 707: Compliance (Insurance)
 */
public class ComplianceInsuranceExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 707: Compliance (Insurance) ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("cpi_audit", "cpi_assess", "cpi_file_reports", "cpi_track", "cpi_certify"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AuditWorker(), new AssessWorker(), new FileReportsWorker(), new TrackWorker(), new CertifyWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("cpi_compliance_insurance", 1, Map.of("companyId", "INS-CO-707", "regulatoryBody", "NAIC", "compliancePeriod", "Q1-2024"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("complianceStatus: %s%n", workflow.getOutput().get("complianceStatus"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
