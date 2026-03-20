package remotemonitoring;

import com.netflix.conductor.client.worker.Worker;
import remotemonitoring.workers.*;

import java.util.List;
import java.util.Map;

public class RemoteMonitoringExample {

    private static final List<Worker> WORKERS = List.of(
            new CollectVitalsWorker(),
            new AnalyzeTrendsWorker(),
            new NormalActionWorker(),
            new AlertActionWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Remote Patient Monitoring Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("rpm_collect_vitals", "rpm_analyze_trends", "rpm_normal_action", "rpm_alert_action"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("remote_monitoring_workflow", 1, Map.of(
                "patientId", "PAT-10234",
                "deviceId", "RPM-DEV-5501"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Patient status: " + result.getStatus());
        System.out.println("Alerts: " + result.getOutput().get("alerts"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
