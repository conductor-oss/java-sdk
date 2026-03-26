package sensordataprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sensordataprocessing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Sensor Data Processing Demo
 *
 * IoT sensor pipeline: collect sensor readings, validate data quality,
 * aggregate readings, analyze patterns, and trigger alerts.
 *
 * Run:
 *   java -jar target/sensor-data-processing-1.0.0.jar --workers
 */
public class SensorDataProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Sensor Data Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sen_collect_readings", "sen_validate_data",
                "sen_aggregate_readings", "sen_analyze_patterns",
                "sen_trigger_alerts"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'sensor_data_processing_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectReadingsWorker(),
                new ValidateDataWorker(),
                new AggregateReadingsWorker(),
                new AnalyzePatternsWorker(),
                new TriggerAlertsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("sensor_data_processing_workflow", 1,
                Map.of("batchId", "BATCH-531-001",
                        "sensorGroupId", "SG-WAREHOUSE-A",
                        "timeWindowMinutes", 60));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
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
