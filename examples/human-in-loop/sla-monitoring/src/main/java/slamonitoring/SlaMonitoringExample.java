package slamonitoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import slamonitoring.workers.SlaProcessWorker;
import slamonitoring.workers.SlaRecordMetricsWorker;

import java.util.List;
import java.util.Map;

/**
 * SLA Monitoring -- Track Time-to-Approval SLA Metrics
 *
 * Demonstrates a workflow that:
 *   1. Processes a request (sla_process)
 *   2. Waits for human approval (WAIT task: sla_wait)
 *   3. Records SLA metrics (sla_record_metrics) -- calculates wait duration
 *      and checks whether it met the SLA threshold
 *
 * Run:
 *   java -jar target/sla-monitoring-1.0.0.jar
 *   java -jar target/sla-monitoring-1.0.0.jar --workers
 */
public class SlaMonitoringExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SLA Monitoring Demo: Track Time-to-Approval SLA Metrics ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef processTask = new TaskDef();
        processTask.setName("sla_process");
        processTask.setTimeoutSeconds(60);
        processTask.setResponseTimeoutSeconds(30);
        processTask.setOwnerEmail("examples@orkes.io");

        TaskDef recordMetricsTask = new TaskDef();
        recordMetricsTask.setName("sla_record_metrics");
        recordMetricsTask.setTimeoutSeconds(60);
        recordMetricsTask.setResponseTimeoutSeconds(30);
        recordMetricsTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(processTask, recordMetricsTask));

        System.out.println("  Registered: sla_process, sla_record_metrics\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'sla_monitoring_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new SlaProcessWorker(), new SlaRecordMetricsWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow with a 5-second SLA
        System.out.println("Step 4: Starting workflow (slaMs=5000)...\n");
        String workflowId = client.startWorkflow("sla_monitoring_demo", 1,
                Map.of("slaMs", 5000));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion (WAIT task requires external signal)...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
