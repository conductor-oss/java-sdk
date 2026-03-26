package realtimeanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckAlertsWorkerTest {

    private final CheckAlertsWorker worker = new CheckAlertsWorker();

    @Test
    void taskDefName() {
        assertEquals("ry_check_alerts", worker.getTaskDefName());
    }

    @Test
    void triggersAnomalyAlert() {
        Map<String, Object> agg = new HashMap<>();
        agg.put("currentWindow", Map.of("anomalyCount", 3, "errorCount", 0));
        agg.put("p99Latency", "300ms");
        Task task = taskWith(Map.of("aggregates", agg));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("alertCount"));
    }

    @Test
    void triggersErrorAlert() {
        Map<String, Object> agg = new HashMap<>();
        agg.put("currentWindow", Map.of("anomalyCount", 0, "errorCount", 2));
        agg.put("p99Latency", "200ms");
        Task task = taskWith(Map.of("aggregates", agg));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("alertCount"));
    }

    @Test
    void triggersHighLatencyAlert() {
        Map<String, Object> agg = new HashMap<>();
        agg.put("currentWindow", Map.of("anomalyCount", 0, "errorCount", 0));
        agg.put("p99Latency", "820ms");
        Task task = taskWith(Map.of("aggregates", agg));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("alertCount"));
    }

    @Test
    void noAlertsWhenAllClear() {
        Map<String, Object> agg = new HashMap<>();
        agg.put("currentWindow", Map.of("anomalyCount", 0, "errorCount", 0));
        agg.put("p99Latency", "200ms");
        Task task = taskWith(Map.of("aggregates", agg));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("alertCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void multipleAlerts() {
        Map<String, Object> agg = new HashMap<>();
        agg.put("currentWindow", Map.of("anomalyCount", 3, "errorCount", 2));
        agg.put("p99Latency", "820ms");
        Task task = taskWith(Map.of("aggregates", agg));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("alertCount"));
        List<Map<String, String>> alerts = (List<Map<String, String>>) result.getOutputData().get("alerts");
        assertEquals(3, alerts.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
