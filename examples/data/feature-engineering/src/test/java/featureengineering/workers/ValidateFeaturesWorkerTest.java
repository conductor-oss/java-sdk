package featureengineering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateFeaturesWorkerTest {

    private final ValidateFeaturesWorker worker = new ValidateFeaturesWorker();

    @Test void taskDefName() { assertEquals("fe_validate_features", worker.getTaskDefName()); }

    @Test void passesValidFeatures() {
        Task task = taskWith(Map.of("normalizedFeatures", List.of(Map.of("a", 0.0, "b", 0.5), Map.of("a", 1.0, "b", 0.3))));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("passed"));
        assertEquals(true, r.getOutputData().get("allInRange"));
        assertEquals(false, r.getOutputData().get("hasNulls"));
    }

    @Test void failsOutOfRange() {
        Task task = taskWith(Map.of("normalizedFeatures", List.of(Map.of("a", 1.5, "b", 0.5))));
        TaskResult r = worker.execute(task);
        assertEquals(false, r.getOutputData().get("passed"));
        assertEquals(false, r.getOutputData().get("allInRange"));
    }

    @Test void failsWithNulls() {
        Map<String, Object> record = new HashMap<>();
        record.put("a", 0.5); record.put("b", null);
        Task task = taskWith(Map.of("normalizedFeatures", List.of(record)));
        TaskResult r = worker.execute(task);
        assertEquals(false, r.getOutputData().get("passed"));
        assertEquals(true, r.getOutputData().get("hasNulls"));
    }

    @Test @SuppressWarnings("unchecked")
    void returnsFeatureVector() {
        Task task = taskWith(Map.of("normalizedFeatures", List.of(Map.of("x", 0.1, "y", 0.9))));
        TaskResult r = worker.execute(task);
        List<String> fv = (List<String>) r.getOutputData().get("featureVector");
        assertTrue(fv.contains("x"));
        assertTrue(fv.contains("y"));
    }

    @Test void emptyFeaturesPassesValidation() {
        Task task = taskWith(Map.of("normalizedFeatures", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("passed"));
    }

    @Test void handlesNullFeatures() {
        Map<String, Object> input = new HashMap<>(); input.put("normalizedFeatures", null);
        Task task = taskWith(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void boundaryValuesPass() {
        Task task = taskWith(Map.of("normalizedFeatures", List.of(Map.of("a", 0.0, "b", 1.0))));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("passed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
