package streamprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import streamprocessing.workers.*;

import java.util.List;
import java.util.Map;

public class StreamProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Stream Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("st_ingest_stream", "st_window_events", "st_aggregate_windows", "st_detect_anomalies", "st_emit_results"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IngestStreamWorker(), new WindowEventsWorker(), new AggregateWindowsWorker(),
                new DetectAnomaliesWorker(), new EmitResultsWorker());
        client.startWorkers(workers);

        if (workersOnly) { System.out.println("Worker-only mode. Press Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...");
        String wfId = client.startWorkflow("stream_processing", 1, Map.of(
                "events", List.of(
                        Map.of("ts", 1000, "value", 10, "source", "sensor-1"),
                        Map.of("ts", 2000, "value", 12, "source", "sensor-1"),
                        Map.of("ts", 3000, "value", 11, "source", "sensor-2"),
                        Map.of("ts", 6000, "value", 50, "source", "sensor-1"),
                        Map.of("ts", 7000, "value", 55, "source", "sensor-2"),
                        Map.of("ts", 8000, "value", 48, "source", "sensor-1"),
                        Map.of("ts", 11000, "value", 9, "source", "sensor-1"),
                        Map.of("ts", 12000, "value", 13, "source", "sensor-2"),
                        Map.of("ts", 14000, "value", 10, "source", "sensor-1")),
                "windowSizeMs", 5000));

        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
    }
}
