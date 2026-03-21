package claimcheck.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    private final ProcessWorker worker = new ProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("clc_process", worker.getTaskDefName());
    }

    @Test
    void processesPayloadWithMetrics() {
        Map<String, Object> payload = Map.of(
                "reportId", "RPT-001",
                "data", List.of(
                        Map.of("metric", "cpu_usage", "values", List.of(45, 52, 68, 71, 55)),
                        Map.of("metric", "memory_usage", "values", List.of(62, 64, 70, 68, 65))
                ));
        Task task = taskWith(Map.of("retrievedPayload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals(2, result.getOutputData().get("metricsAnalyzed"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void computesCorrectAverages() {
        Map<String, Object> payload = Map.of(
                "data", List.of(
                        Map.of("metric", "test_metric", "values", List.of(10, 20, 30))
                ));
        Task task = taskWith(Map.of("retrievedPayload", payload));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> summary = (List<Map<String, Object>>) result.getOutputData().get("summary");
        assertEquals(1, summary.size());
        assertEquals("test_metric", summary.get(0).get("metric"));
        assertEquals(20.0, (double) summary.get(0).get("avg"), 0.001);
    }

    @SuppressWarnings("unchecked")
    @Test
    void summaryContainsAllMetrics() {
        Map<String, Object> payload = Map.of(
                "data", List.of(
                        Map.of("metric", "a", "values", List.of(1)),
                        Map.of("metric", "b", "values", List.of(2)),
                        Map.of("metric", "c", "values", List.of(3))
                ));
        Task task = taskWith(Map.of("retrievedPayload", payload));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> summary = (List<Map<String, Object>>) result.getOutputData().get("summary");
        assertEquals(3, summary.size());
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("retrievedPayload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals(0, result.getOutputData().get("metricsAnalyzed"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of("retrievedPayload", Map.of("data", List.of())));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("metricsAnalyzed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesEmptyValuesInMetric() {
        Map<String, Object> payload = Map.of(
                "data", List.of(
                        Map.of("metric", "empty_metric", "values", List.of())
                ));
        Task task = taskWith(Map.of("retrievedPayload", payload));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> summary = (List<Map<String, Object>>) result.getOutputData().get("summary");
        assertEquals(1, summary.size());
        assertEquals(0.0, (double) summary.get(0).get("avg"), 0.001);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
