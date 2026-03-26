package mldatapipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {
    private final CollectDataWorker worker = new CollectDataWorker();

    @Test void taskDefName() { assertEquals("ml_collect_data", worker.getTaskDefName()); }

    @Test void returnsCompleted() { assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus()); }

    @Test void returnsTenRecords() { assertEquals(10, worker.execute(taskWith(Map.of())).getOutputData().get("recordCount")); }

    @Test @SuppressWarnings("unchecked")
    void containsThreeClasses() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        List<Map<String, Object>> data = (List<Map<String, Object>>) r.getOutputData().get("data");
        Set<String> labels = new HashSet<>();
        for (Map<String, Object> d : data) labels.add((String) d.get("label"));
        assertEquals(3, labels.size());
        assertTrue(labels.contains("setosa"));
        assertTrue(labels.contains("versicolor"));
        assertTrue(labels.contains("virginica"));
    }

    @Test @SuppressWarnings("unchecked")
    void containsNullFeatureRecord() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        List<Map<String, Object>> data = (List<Map<String, Object>>) r.getOutputData().get("data");
        boolean hasNull = data.stream().anyMatch(d -> {
            List<Object> features = (List<Object>) d.get("features");
            return features.stream().anyMatch(Objects::isNull);
        });
        assertTrue(hasNull);
    }

    @Test @SuppressWarnings("unchecked")
    void eachRecordHasFourFeatures() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        List<Map<String, Object>> data = (List<Map<String, Object>>) r.getOutputData().get("data");
        for (Map<String, Object> d : data) {
            List<?> features = (List<?>) d.get("features");
            assertEquals(4, features.size());
        }
    }

    @Test @SuppressWarnings("unchecked")
    void recordCountMatchesDataSize() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        List<?> data = (List<?>) r.getOutputData().get("data");
        assertEquals(data.size(), r.getOutputData().get("recordCount"));
    }

    @Test void dataOutputNotNull() { assertNotNull(worker.execute(taskWith(Map.of())).getOutputData().get("data")); }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
