package featureengineering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NormalizeFeaturesWorkerTest {

    private final NormalizeFeaturesWorker worker = new NormalizeFeaturesWorker();

    @Test void taskDefName() { assertEquals("fe_normalize_features", worker.getTaskDefName()); }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("transformedFeatures", List.of(Map.of("a", 10, "b", 20), Map.of("a", 30, "b", 40))));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test @SuppressWarnings("unchecked")
    void normalizesToZeroOne() {
        Task task = taskWith(Map.of("transformedFeatures", List.of(Map.of("x", 0, "y", 50), Map.of("x", 100, "y", 100))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> n = (List<Map<String, Object>>) r.getOutputData().get("normalized");
        assertEquals(0.0, ((Number) n.get(0).get("x")).doubleValue());
        assertEquals(1.0, ((Number) n.get(1).get("x")).doubleValue());
        assertEquals(0.0, ((Number) n.get(0).get("y")).doubleValue());
        assertEquals(1.0, ((Number) n.get(1).get("y")).doubleValue());
    }

    @Test @SuppressWarnings("unchecked")
    void handlesConstantFeature() {
        Task task = taskWith(Map.of("transformedFeatures", List.of(Map.of("x", 5), Map.of("x", 5))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> n = (List<Map<String, Object>>) r.getOutputData().get("normalized");
        assertEquals(0.0, ((Number) n.get(0).get("x")).doubleValue());
    }

    @Test void normalizedCountEqualsFeatureCount() {
        Task task = taskWith(Map.of("transformedFeatures", List.of(Map.of("a", 1, "b", 2, "c", 3), Map.of("a", 4, "b", 5, "c", 6))));
        TaskResult r = worker.execute(task);
        assertEquals(3, r.getOutputData().get("normalizedCount"));
    }

    @Test void hasStats() {
        Task task = taskWith(Map.of("transformedFeatures", List.of(Map.of("x", 10), Map.of("x", 20))));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("stats"));
    }

    @Test void handlesEmptyFeatures() {
        Task task = taskWith(Map.of("transformedFeatures", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(0, r.getOutputData().get("normalizedCount"));
    }

    @Test void handlesNullFeatures() {
        Map<String, Object> input = new HashMap<>(); input.put("transformedFeatures", null);
        Task task = taskWith(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
