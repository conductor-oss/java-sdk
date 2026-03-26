package ddosmitigation;

import com.netflix.conductor.client.worker.Worker;
import ddosmitigation.workers.DetectWorker;
import ddosmitigation.workers.ClassifyWorker;
import ddosmitigation.workers.MitigateWorker;
import ddosmitigation.workers.VerifyServiceWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 353: DDoS Mitigation — Automated DDoS Detection and Response
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 353: DDoS Mitigation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ddos_detect",
                "ddos_classify",
                "ddos_mitigate",
                "ddos_verify_service"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DetectWorker(),
                new ClassifyWorker(),
                new MitigateWorker(),
                new VerifyServiceWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("ddos_mitigation_workflow", 1, Map.of(
                "targetService", "api-gateway",
                "trafficAnomaly", "5x-baseline"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  detectResult: " + execution.getOutput().get("detectResult"));
        System.out.println("  verify_serviceResult: " + execution.getOutput().get("verify_serviceResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
