package publichealth;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import publichealth.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 527: Public Health — Surveillance, Detect Outbreak, SWITCH(Alert/Monitor), Respond
 *
 * Performs a public health surveillance workflow with conditional alert or monitoring via SWITCH.
 */
public class PublicHealthExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 527: Public Health ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("phw_surveillance", "phw_detect_outbreak", "phw_alert", "phw_monitor", "phw_respond"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new SurveillanceWorker(),
                new DetectOutbreakWorker(),
                new AlertWorker(),
                new MonitorWorker(),
                new RespondWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("phw_public_health", 1, Map.of(
                    "region", "Metro-District-5",
                    "disease", "influenza",
                    "caseCount", 45
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Action taken: %s%n", workflow.getOutput().get("action"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
