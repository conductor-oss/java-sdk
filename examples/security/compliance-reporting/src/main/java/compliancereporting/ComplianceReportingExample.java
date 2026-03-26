package compliancereporting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import compliancereporting.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Automated Compliance Report Generation
 *
 * Pattern: collect-evidence -> map-controls -> assess-gaps -> generate-report
 */
public class ComplianceReportingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Compliance Reporting Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "cr_collect_evidence", "cr_map_controls", "cr_assess_gaps", "cr_generate_report"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectEvidenceWorker(), new MapControlsWorker(),
                new AssessGapsWorker(), new GenerateReportWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }

        Thread.sleep(2000);
        String workflowId = client.startWorkflow("compliance_reporting_workflow", 1,
                Map.of("framework", "SOC2-TypeII", "period", "2024-Q1"));

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
