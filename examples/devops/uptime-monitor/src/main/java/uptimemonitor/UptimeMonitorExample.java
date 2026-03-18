package uptimemonitor;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import uptimemonitor.workers.*;

import java.util.*;

/**
 * Uptime Monitor — Endpoint Health Check Pipeline
 *
 * Checks multiple endpoints in parallel using FORK_JOIN_DYNAMIC, aggregates results,
 * and routes through failure notifications (Slack, email, status page) or records
 * healthy status. Escalates to SMS/PagerDuty when consecutive failures exceed threshold.
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/uptime-monitor-1.0.0.jar
 */
public class UptimeMonitorExample {

    private static final List<String> TASK_NAMES = List.of(
            "uptime_prepare_checks",
            "uptime_check_endpoint",
            "uptime_aggregate_results",
            "uptime_send_slack_alert",
            "uptime_send_email_alert",
            "uptime_update_status_page",
            "uptime_check_escalation",
            "uptime_send_sms_alert",
            "uptime_page_oncall",
            "uptime_record_healthy",
            "uptime_store_metrics"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new PrepareChecks(),
                new CheckEndpoint(),
                new AggregateResults(),
                new SendSlackAlert(),
                new SendEmailAlert(),
                new UpdateStatusPage(),
                new CheckEscalation(),
                new SendSmsAlert(),
                new PageOncall(),
                new RecordHealthy(),
                new StoreMetrics()
        );
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Uptime Monitor Demo: Endpoint Health Check Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'uptime_monitor'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Use the Conductor CLI or UI to start workflows.\n");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join(); // Block forever
            return;
        }

        // Allow workers to start polling
        Thread.sleep(2000);

        // Step 4 — Run the monitor with REAL public endpoints
        System.out.println("Step 4: Running uptime monitor with real endpoints...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("endpoints", List.of(
                Map.of("url", "https://www.google.com", "name", "Google", "expectedStatus", 200, "timeout", 5000),
                Map.of("url", "https://github.com", "name", "GitHub", "expectedStatus", 200, "timeout", 5000),
                Map.of("url", "https://www.cloudflare.com", "name", "Cloudflare", "expectedStatus", 200, "timeout", 5000),
                Map.of("url", "https://down.example.invalid", "name", "Intentional Failure", "expectedStatus", 200, "timeout", 3000)
        ));
        input.put("notificationChannels", Map.of(
                "slack", Map.of("webhook", "", "channel", "#ops-alerts"),
                "email", Map.of("recipients", List.of("oncall@example.com", "platform-team@example.com")),
                "sms", Map.of("phones", List.of("+1555000111", "+1555000222")),
                "pagerduty", Map.of("serviceKey", "abc123", "escalationPolicy", "P1234")
        ));
        input.put("escalationThreshold", 3);

        String workflowId = client.startWorkflow("uptime_monitor", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Monitor Results ---");
            String overallStatus = String.valueOf(output.get("overallStatus")).toUpperCase();
            System.out.println("  Overall status : " + overallStatus);
            System.out.println("  Has failures   : " + output.get("hasFailures"));

            Map<String, Object> summary = (Map<String, Object>) output.get("summary");
            if (summary != null) {
                System.out.println("  Endpoints      : " + summary.get("totalEndpoints") + " total, "
                        + summary.get("healthy") + " healthy, "
                        + summary.get("degraded") + " degraded, "
                        + summary.get("down") + " down");
                System.out.println("  Avg response   : " + summary.get("avgResponseTimeMs") + "ms");
            }

            List<Map<String, Object>> failures = (List<Map<String, Object>>) output.get("failures");
            if (failures != null && !failures.isEmpty()) {
                System.out.println("  Failures       :");
                for (var f : failures) {
                    System.out.println("    - " + f.get("name") + ": "
                            + String.join(", ", ((List<String>) f.get("failedChecks")))
                            + " failed (" + f.get("status") + ")");
                }
            }
            System.out.println("  Metrics stored : " + output.get("metricsStored") + " data points");
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nWorkflow did not complete (status: " + status + ")");
            workflow.getTasks().stream()
                    .filter(t -> t.getStatus().name().equals("FAILED"))
                    .forEach(t -> System.out.println("  Failed task: " + t.getReferenceTaskName()
                            + " — " + t.getReasonForIncompletion()));
            System.out.println("Result: WORKFLOW_ERROR");
            System.exit(1);
        }

        // Report based on business outcome, not workflow status
        boolean hasFailures = output != null && Boolean.TRUE.equals(output.get("hasFailures"));
        if (hasFailures) {
            System.out.println("\nResult: UNHEALTHY — failures detected (workflow completed successfully)");
            System.exit(1);
        } else {
            System.out.println("\nResult: HEALTHY — all endpoints passed");
            System.exit(0);
        }
    }
}
