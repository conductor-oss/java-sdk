package sensordataprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzePatternsWorkerTest {

    private final AnalyzePatternsWorker worker = new AnalyzePatternsWorker();

    @Test
    void taskDefName() {
        assertEquals("sen_analyze_patterns", worker.getTaskDefName());
    }

    @Test
    void detectsAnomalyWhenMaxTempAbove85() {
        Task task = taskWith(Map.of("aggregatedMetrics", Map.of("maxTemperature", 89.2)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anomalies = (List<Map<String, Object>>) result.getOutputData().get("anomalies");
        assertEquals(1, anomalies.size());
        assertEquals("high_temperature", anomalies.get(0).get("type"));
    }

    @Test
    void noAnomalyWhenMaxTempBelow85() {
        Task task = taskWith(Map.of("aggregatedMetrics", Map.of("maxTemperature", 80.0)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<?> anomalies = (List<?>) result.getOutputData().get("anomalies");
        assertTrue(anomalies.isEmpty());
    }

    @Test
    void noAnomalyWhenMaxTempExactly85() {
        Task task = taskWith(Map.of("aggregatedMetrics", Map.of("maxTemperature", 85.0)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<?> anomalies = (List<?>) result.getOutputData().get("anomalies");
        assertTrue(anomalies.isEmpty());
    }

    @Test
    void trendIsAlwaysRising() {
        Task task = taskWith(Map.of("aggregatedMetrics", Map.of("maxTemperature", 70.0)));
        TaskResult result = worker.execute(task);

        assertEquals("rising", result.getOutputData().get("trend"));
    }

    @Test
    void outputContainsForecast() {
        Task task = taskWith(Map.of("aggregatedMetrics", Map.of("maxTemperature", 72.0)));
        TaskResult result = worker.execute(task);

        assertEquals(75.2, result.getOutputData().get("forecastNext1h"));
    }

    @Test
    void handlesNullMetrics() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregatedMetrics", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<?> anomalies = (List<?>) result.getOutputData().get("anomalies");
        assertTrue(anomalies.isEmpty());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rising", result.getOutputData().get("trend"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
