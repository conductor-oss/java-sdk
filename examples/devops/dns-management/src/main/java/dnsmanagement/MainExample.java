package dnsmanagement;

import com.netflix.conductor.client.worker.Worker;
import dnsmanagement.workers.PlanWorker;
import dnsmanagement.workers.ValidateWorker;
import dnsmanagement.workers.ApplyWorker;
import dnsmanagement.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 345: DNS Management — Automated DNS Record Orchestration
 *
 * Pattern: plan -> validate -> apply -> verify
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 345: DNS Management ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "dns_plan",
                "dns_validate",
                "dns_apply",
                "dns_verify"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new PlanWorker(),
                new ValidateWorker(),
                new ApplyWorker(),
                new VerifyWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("dns_management_workflow", 1, Map.of(
                "domain", "api.example.com",
                "recordType", "CNAME",
                "target", "lb-new.example.com"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  planResult: " + execution.getOutput().get("planResult"));
        System.out.println("  verifyResult: " + execution.getOutput().get("verifyResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
