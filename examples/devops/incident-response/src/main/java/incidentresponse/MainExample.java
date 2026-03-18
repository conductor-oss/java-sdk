package incidentresponse;

import com.netflix.conductor.client.worker.Worker;
import incidentresponse.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example incident-response: Incident Response — Automated Incident Management
 *
 * Pattern: create-incident -> notify-oncall -> gather-diagnostics -> auto-remediate
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example incident-response: Incident Response ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ir_create_incident", "ir_notify_oncall", "ir_gather_diagnostics", "ir_auto_remediate"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CreateIncidentWorker(),
                new NotifyOncallWorker(),
                new GatherDiagnosticsWorker(),
                new AutoRemediateWorker()
        );
        helper.startWorkers(workers);


        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = helper.startWorkflow("incident_response_workflow", 1, Map.of(
                "alertName", "high-error-rate",
                "service", "api-gateway",
                "severity", "P1"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  incidentId: " + execution.getOutput().get("incidentId"));
        System.out.println("  remediated: " + execution.getOutput().get("remediated"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
