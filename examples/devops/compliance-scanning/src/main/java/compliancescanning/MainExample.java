package compliancescanning;

import com.netflix.conductor.client.worker.Worker;
import compliancescanning.workers.DiscoverResourcesWorker;
import compliancescanning.workers.ScanPoliciesWorker;
import compliancescanning.workers.GenerateReportWorker;
import compliancescanning.workers.RemediateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 480: Compliance Scanning — Automated Infrastructure Compliance
 *
 * Pattern: discoverresources -> scanpolicies -> generatereport -> remediate
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 480: Compliance Scanning ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "cs_discover_resources",
                "cs_scan_policies",
                "cs_generate_report",
                "cs_remediate"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DiscoverResourcesWorker(),
                new ScanPoliciesWorker(),
                new GenerateReportWorker(),
                new RemediateWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("compliance_scanning_workflow", 1, Map.of(
                "environment", "production",
                "framework", "CIS-AWS"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  discover_resourcesResult: " + execution.getOutput().get("discover_resourcesResult"));
        System.out.println("  remediateResult: " + execution.getOutput().get("remediateResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
