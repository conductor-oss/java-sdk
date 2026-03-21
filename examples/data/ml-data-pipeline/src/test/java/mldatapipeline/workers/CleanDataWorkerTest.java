package mldatapipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CleanDataWorkerTest {
    private final CleanDataWorker worker = new CleanDataWorker();

    @Test void taskDefName() { assertEquals("ml_clean_data", worker.getTaskDefName()); }

    @Test void removesNullFeatureRecords() {
        List<Object> nullFeatures = new ArrayList<>(); nullFeatures.add(null); nullFeatures.add(3.0); nullFeatures.add(1.5); nullFeatures.add(0.2);
        Map<String, Object> nullRec = new HashMap<>(); nullRec.put("features", nullFeatures); nullRec.put("label", "setosa");
        Task task = taskWith(Map.of("rawData", List.of(Map.of("features", List.of(5.1, 3.5, 1.4, 0.2), "label", "setosa"), nullRec)));
        TaskResult r = worker.execute(task);
        assertEquals(1, r.getOutputData().get("cleanCount"));
        assertEquals(1, r.getOutputData().get("removedCount"));
    }

    @Test void keepsAllCleanRecords() {
        Task task = taskWith(Map.of("rawData", List.of(
                Map.of("features", List.of(5.1, 3.5, 1.4, 0.2), "label", "setosa"),
                Map.of("features", List.of(7.0, 3.2, 4.7, 1.4), "label", "versicolor"))));
        TaskResult r = worker.execute(task);
        assertEquals(2, r.getOutputData().get("cleanCount"));
        assertEquals(0, r.getOutputData().get("removedCount"));
    }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("rawData", List.of()));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void handlesNullRawData() {
        Map<String, Object> input = new HashMap<>(); input.put("rawData", null);
        Task task = taskWith(input);
        TaskResult r = worker.execute(task);
        assertEquals(0, r.getOutputData().get("cleanCount"));
    }

    @Test void handlesEmptyRawData() {
        Task task = taskWith(Map.of("rawData", List.of()));
        assertEquals(0, worker.execute(task).getOutputData().get("cleanCount"));
    }

    @Test @SuppressWarnings("unchecked")
    void cleanDataPreservesRecords() {
        Task task = taskWith(Map.of("rawData", List.of(Map.of("features", List.of(5.1, 3.5, 1.4, 0.2), "label", "setosa"))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> clean = (List<Map<String, Object>>) r.getOutputData().get("cleanData");
        assertEquals("setosa", clean.get(0).get("label"));
    }

    @Test void removedCountPlusCleanEqualsTotal() {
        List<Object> nullFeatures = new ArrayList<>(); nullFeatures.add(null); nullFeatures.add(3.0); nullFeatures.add(1.5); nullFeatures.add(0.2);
        Map<String, Object> nullRec = new HashMap<>(); nullRec.put("features", nullFeatures); nullRec.put("label", "x");
        Task task = taskWith(Map.of("rawData", List.of(
                Map.of("features", List.of(1.0, 2.0), "label", "a"),
                Map.of("features", List.of(3.0, 4.0), "label", "b"),
                nullRec)));
        TaskResult r = worker.execute(task);
        int clean = (int) r.getOutputData().get("cleanCount");
        int removed = (int) r.getOutputData().get("removedCount");
        assertEquals(3, clean + removed);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
