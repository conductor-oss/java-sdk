package sensordataprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateReadingsWorkerTest {

    private final AggregateReadingsWorker worker = new AggregateReadingsWorker();

    @Test
    void taskDefName() {
        assertEquals("sen_aggregate_readings", worker.getTaskDefName());
    }

    @Test
    void aggregatesReadings() {
        Task task = taskWith(Map.of("batchId", "B-001", "validReadings", 1176));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsAvgTemperature() {
        Task task = taskWith(Map.of("validReadings", 1000));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("aggregatedMetrics");
        assertEquals(73.8, metrics.get("avgTemperature"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsMaxTemperature() {
        Task task = taskWith(Map.of("validReadings", 500));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("aggregatedMetrics");
        assertEquals(89.2, metrics.get("maxTemperature"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsMinTemperature() {
        Task task = taskWith(Map.of("validReadings", 500));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("aggregatedMetrics");
        assertEquals(68.1, metrics.get("minTemperature"));
    }

    @Test
    void outputContainsTimeRange() {
        Task task = taskWith(Map.of("validReadings", 1000));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> timeRange = (Map<String, Object>) result.getOutputData().get("timeRange");
        assertEquals("2026-03-08T09:00:00Z", timeRange.get("start"));
        assertEquals("2026-03-08T10:00:00Z", timeRange.get("end"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsStdDevTemperature() {
        Task task = taskWith(Map.of("validReadings", 200));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("aggregatedMetrics");
        assertEquals(4.2, metrics.get("stdDevTemperature"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("aggregatedMetrics"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
