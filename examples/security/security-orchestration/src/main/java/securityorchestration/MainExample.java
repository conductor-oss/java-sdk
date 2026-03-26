package securityorchestration;

import com.netflix.conductor.client.worker.Worker;
import securityorchestration.workers.IngestAlertWorker;
import securityorchestration.workers.EnrichWorker;
import securityorchestration.workers.DecideActionWorker;
import securityorchestration.workers.ExecutePlaybookWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 352: Security Orchestration — SOAR Platform with Conductor
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 352: Security Orchestration ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "soar_ingest_alert",
                "soar_enrich",
                "soar_decide_action",
                "soar_execute_playbook"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new IngestAlertWorker(),
                new EnrichWorker(),
                new DecideActionWorker(),
                new ExecutePlaybookWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("security_orchestration_workflow", 1, Map.of(
                "alertId", "ALERT-2024-commission-insurance",
                "alertSource", "crowdstrike"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  ingest_alertResult: " + execution.getOutput().get("ingest_alertResult"));
        System.out.println("  execute_playbookResult: " + execution.getOutput().get("execute_playbookResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
