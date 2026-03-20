package emergencyresponse;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import emergencyresponse.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 525: Emergency Response — Detect, Classify Severity, Dispatch, Coordinate, Debrief
 *
 * Runs an emergency response workflow from detection to post-incident debrief.
 */
public class EmergencyResponseExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 525: Emergency Response ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("emr_detect", "emr_classify_severity", "emr_dispatch", "emr_coordinate", "emr_debrief"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DetectWorker(),
                new ClassifySeverityWorker(),
                new DispatchWorker(),
                new CoordinateWorker(),
                new DebriefWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("emr_emergency_response", 1, Map.of(
                    "incidentType", "structure-fire",
                    "location", "123 Oak Ave",
                    "reportedBy", "911-dispatch"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Outcome: %s%n", workflow.getOutput().get("outcome"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
