package soc2automation;

import com.netflix.conductor.client.worker.Worker;
import soc2automation.workers.CollectControlsWorker;
import soc2automation.workers.TestEffectivenessWorker;
import soc2automation.workers.IdentifyExceptionsWorker;
import soc2automation.workers.GenerateEvidenceWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 361: SOC2 Automation — Continuous SOC 2 Compliance Monitoring
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 361: SOC2 Automation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "soc2_collect_controls",
                "soc2_test_effectiveness",
                "soc2_identify_exceptions",
                "soc2_generate_evidence"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectControlsWorker(),
                new TestEffectivenessWorker(),
                new IdentifyExceptionsWorker(),
                new GenerateEvidenceWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("soc2_automation_workflow", 1, Map.of(
                "trustServiceCriteria", "security",
                "period", "2024-Q1"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  collect_controlsResult: " + execution.getOutput().get("collect_controlsResult"));
        System.out.println("  generate_evidenceResult: " + execution.getOutput().get("generate_evidenceResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
