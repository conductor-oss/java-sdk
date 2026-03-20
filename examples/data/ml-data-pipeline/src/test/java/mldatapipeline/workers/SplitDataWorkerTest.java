package mldatapipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SplitDataWorkerTest {
    private final SplitDataWorker worker = new SplitDataWorker();

    @Test void taskDefName() { assertEquals("ml_split_data", worker.getTaskDefName()); }

    @Test void splits80_20ByDefault() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) data.add(Map.of("features", List.of(1.0), "label", "a"));
        Task task = taskWith(Map.of("cleanData", data));
        TaskResult r = worker.execute(task);
        assertEquals(8, r.getOutputData().get("trainSize"));
        assertEquals(2, r.getOutputData().get("testSize"));
    }

    @Test void respectsCustomRatio() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) data.add(Map.of("features", List.of(1.0), "label", "a"));
        Task task = taskWith(Map.of("cleanData", data, "splitRatio", 0.5));
        TaskResult r = worker.execute(task);
        assertEquals(5, r.getOutputData().get("trainSize"));
        assertEquals(5, r.getOutputData().get("testSize"));
    }

    @Test void trainPlusTestEqualsTotal() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 9; i++) data.add(Map.of("features", List.of(1.0), "label", "a"));
        Task task = taskWith(Map.of("cleanData", data, "splitRatio", 0.8));
        TaskResult r = worker.execute(task);
        int train = (int) r.getOutputData().get("trainSize");
        int test = (int) r.getOutputData().get("testSize");
        assertEquals(9, train + test);
    }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("cleanData", List.of()));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void handlesNullData() {
        Map<String, Object> input = new HashMap<>(); input.put("cleanData", null);
        Task task = taskWith(input);
        assertEquals(0, worker.execute(task).getOutputData().get("trainSize"));
    }

    @Test @SuppressWarnings("unchecked")
    void trainDataContainsFirstRecords() {
        List<Map<String, Object>> data = List.of(Map.of("label", "first"), Map.of("label", "second"));
        Task task = taskWith(Map.of("cleanData", data, "splitRatio", 0.5));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> train = (List<Map<String, Object>>) r.getOutputData().get("trainData");
        assertEquals("first", train.get(0).get("label"));
    }

    @Test void emptyDataSplitWorks() {
        Task task = taskWith(Map.of("cleanData", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(0, r.getOutputData().get("trainSize"));
        assertEquals(0, r.getOutputData().get("testSize"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
