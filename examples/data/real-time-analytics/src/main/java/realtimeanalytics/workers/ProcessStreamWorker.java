package realtimeanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Processes the event stream: computes window metrics, flags anomalies.
 * Input: events (list), windowSize
 * Output: processed (list), processedCount, windowMetrics
 */
public class ProcessStreamWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ry_process_stream";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("events", List.of());

        List<Map<String, Object>> processed = new ArrayList<>();
        int anomalyCount = 0;
        int errorCount = 0;
        long totalLatency = 0;
        Set<String> uniqueUsers = new LinkedHashSet<>();

        for (Map<String, Object> e : events) {
            int latency = ((Number) e.getOrDefault("latency", 0)).intValue();
            String type = (String) e.getOrDefault("type", "");
            boolean isAnomaly = latency > 500 || "error".equals(type);

            Map<String, Object> p = new LinkedHashMap<>(e);
            p.put("processed", true);
            p.put("isAnomaly", isAnomaly);
            processed.add(p);

            if (isAnomaly) anomalyCount++;
            if ("error".equals(type)) errorCount++;
            totalLatency += latency;
            uniqueUsers.add((String) e.getOrDefault("userId", ""));
        }

        int avgLatency = events.isEmpty() ? 0 : (int) (totalLatency / events.size());

        Map<String, Object> windowMetrics = new LinkedHashMap<>();
        windowMetrics.put("eventCount", events.size());
        windowMetrics.put("avgLatency", avgLatency + "ms");
        windowMetrics.put("anomalyCount", anomalyCount);
        windowMetrics.put("errorCount", errorCount);
        windowMetrics.put("uniqueUsers", uniqueUsers.size());

        System.out.println("  [process] Processed " + processed.size() + " events - avg latency: "
                + avgLatency + "ms, anomalies: " + anomalyCount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", processed);
        result.getOutputData().put("processedCount", processed.size());
        result.getOutputData().put("windowMetrics", windowMetrics);
        return result;
    }
}
