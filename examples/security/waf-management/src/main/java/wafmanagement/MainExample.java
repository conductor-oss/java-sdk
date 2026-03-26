package wafmanagement;

import com.netflix.conductor.client.worker.Worker;
import wafmanagement.workers.AnalyzeTrafficWorker;
import wafmanagement.workers.UpdateRulesWorker;
import wafmanagement.workers.DeployRulesWorker;
import wafmanagement.workers.VerifyProtectionWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 364: WAF Management — Web Application Firewall Rule Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 364: WAF Management ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "waf_analyze_traffic",
                "waf_update_rules",
                "waf_deploy_rules",
                "waf_verify_protection"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new AnalyzeTrafficWorker(),
                new UpdateRulesWorker(),
                new DeployRulesWorker(),
                new VerifyProtectionWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("waf_management_workflow", 1, Map.of(
                "application", "web-portal",
                "threatType", "OWASP-Top10"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  analyze_trafficResult: " + execution.getOutput().get("analyze_trafficResult"));
        System.out.println("  verify_protectionResult: " + execution.getOutput().get("verify_protectionResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
