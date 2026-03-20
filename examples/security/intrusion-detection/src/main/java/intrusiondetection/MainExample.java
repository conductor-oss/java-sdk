package intrusiondetection;

import com.netflix.conductor.client.worker.Worker;
import intrusiondetection.workers.AnalyzeEventsWorker;
import intrusiondetection.workers.CorrelateThreatsWorker;
import intrusiondetection.workers.AssessSeverityWorker;
import intrusiondetection.workers.RespondWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 359: Intrusion Detection — Automated Threat Detection and Response
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 359: Intrusion Detection ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "id_analyze_events",
                "id_correlate_threats",
                "id_assess_severity",
                "id_respond"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new AnalyzeEventsWorker(),
                new CorrelateThreatsWorker(),
                new AssessSeverityWorker(),
                new RespondWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("intrusion_detection_workflow", 1, Map.of(
                "sourceIp", "198.51.100.42",
                "eventType", "repeated-auth-failure"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  analyze_eventsResult: " + execution.getOutput().get("analyze_eventsResult"));
        System.out.println("  respondResult: " + execution.getOutput().get("respondResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
