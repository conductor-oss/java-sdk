package cloudwatchintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cloudwatchintegration.workers.PutMetricWorker;
import cloudwatchintegration.workers.CreateAlarmWorker;
import cloudwatchintegration.workers.CheckStatusWorker;
import cloudwatchintegration.workers.CwNotifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 450: CloudWatch Integration
 *
 * Runs an AWS CloudWatch integration workflow:
 * put metric -> create alarm -> check status -> notify.
 *
 * Run:
 *   java -jar target/cloudwatch-integration-1.0.0.jar
 */
public class CloudwatchIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 450: CloudWatch Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cw_put_metric", "cw_create_alarm", "cw_check_status", "cw_notify"));
        System.out.println("  Registered: cw_put_metric, cw_create_alarm, cw_check_status, cw_notify\n");

        System.out.println("Step 2: Registering workflow \'cloudwatch_integration_450\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PutMetricWorker(),
                new CreateAlarmWorker(),
                new CheckStatusWorker(),
                new CwNotifyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("cloudwatch_integration_450", 1,
                Map.of("namespace", "Custom/AppMetrics",
                        "metricName", "CPUUtilization",
                        "value", 92,
                        "threshold", 80,
                        "notifyEmail", "ops@example.com"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Metric Published: " + workflow.getOutput().get("metricPublished"));
        System.out.println("  Alarm Name: " + workflow.getOutput().get("alarmName"));
        System.out.println("  Alarm State: " + workflow.getOutput().get("alarmState"));
        System.out.println("  Notified: " + workflow.getOutput().get("notified"));

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
