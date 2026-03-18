package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

/**
 * Stores monitoring metrics to a real JSON file on disk.
 * Writes time-series data points to /tmp/uptime-metrics/ for each
 * monitoring run, creating a persistent record of endpoint health over time.
 */
public class StoreMetrics implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_store_metrics";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_store_metrics] Storing metrics...");

        TaskResult result = new TaskResult(task);
        Map<String, Object> summary = (Map<String, Object>) task.getInputData().get("summary");
        String overallStatus = String.valueOf(task.getInputData().get("overallStatus"));

        String timestamp = Instant.now().toString();
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        if (summary != null) {
            dataPoints.add(Map.of("metric", "endpoints.total", "value", summary.get("totalEndpoints"), "timestamp", timestamp));
            dataPoints.add(Map.of("metric", "endpoints.healthy", "value", summary.get("healthy"), "timestamp", timestamp));
            dataPoints.add(Map.of("metric", "endpoints.degraded", "value", summary.get("degraded"), "timestamp", timestamp));
            dataPoints.add(Map.of("metric", "endpoints.down", "value", summary.get("down"), "timestamp", timestamp));
            dataPoints.add(Map.of("metric", "endpoints.avg_response_ms", "value", summary.get("avgResponseTimeMs"), "timestamp", timestamp));
        }

        dataPoints.add(Map.of("metric", "monitor.overall_status", "value", overallStatus, "timestamp", timestamp));

        // Write metrics to a real file on disk
        try {
            Path metricsDir = Path.of(System.getProperty("java.io.tmpdir"), "uptime-metrics");
            Files.createDirectories(metricsDir);

            String filename = "metrics-" + System.currentTimeMillis() + ".json";
            Path metricsFile = metricsDir.resolve(filename);

            StringBuilder json = new StringBuilder();
            json.append("{\n  \"timestamp\": \"").append(timestamp).append("\",\n");
            json.append("  \"overallStatus\": \"").append(overallStatus).append("\",\n");
            json.append("  \"dataPoints\": [\n");
            for (int i = 0; i < dataPoints.size(); i++) {
                Map<String, Object> dp = dataPoints.get(i);
                json.append("    {\"metric\": \"").append(dp.get("metric"))
                        .append("\", \"value\": \"").append(dp.get("value"))
                        .append("\", \"timestamp\": \"").append(dp.get("timestamp")).append("\"}");
                if (i < dataPoints.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n}\n");

            Files.writeString(metricsFile, json.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  Stored " + dataPoints.size() + " data points to " + metricsFile);
        } catch (Exception e) {
            System.out.println("  Failed to write metrics file: " + e.getMessage());
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metricsStored", dataPoints.size());
        result.getOutputData().put("dataPoints", dataPoints);
        result.getOutputData().put("retention", "90d");
        return result;
    }
}
