package featureengineering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractFeaturesWorkerTest {

    private final ExtractFeaturesWorker worker = new ExtractFeaturesWorker();

    @Test void taskDefName() { assertEquals("fe_extract_features", worker.getTaskDefName()); }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("rawData", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", true, "is_active", true, "balance", 10000))));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test void extractsSevenFeatures() {
        Task task = taskWith(Map.of("rawData", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", true, "is_active", true, "balance", 10000))));
        TaskResult r = worker.execute(task);
        assertEquals(7, r.getOutputData().get("featureCount"));
    }

    @Test @SuppressWarnings("unchecked")
    void convertsBooleanToInt() {
        Task task = taskWith(Map.of("rawData", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", true, "is_active", false, "balance", 10000))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> features = (List<Map<String, Object>>) r.getOutputData().get("features");
        assertEquals(1, features.get(0).get("has_credit_card"));
        assertEquals(0, features.get(0).get("is_active"));
    }

    @Test @SuppressWarnings("unchecked")
    void preservesNumericValues() {
        Task task = taskWith(Map.of("rawData", List.of(Map.of("age", 35, "income", 75000, "tenure_months", 48, "num_products", 3, "has_credit_card", true, "is_active", true, "balance", 12000))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> features = (List<Map<String, Object>>) r.getOutputData().get("features");
        assertEquals(35, features.get(0).get("age"));
        assertEquals(75000, features.get(0).get("income"));
    }

    @Test void handlesMultipleRecords() {
        Task task = taskWith(Map.of("rawData", List.of(
                Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", true, "is_active", true, "balance", 10000),
                Map.of("age", 40, "income", 60000, "tenure_months", 36, "num_products", 3, "has_credit_card", false, "is_active", true, "balance", 20000))));
        TaskResult r = worker.execute(task);
        @SuppressWarnings("unchecked") List<?> features = (List<?>) r.getOutputData().get("features");
        assertEquals(2, features.size());
    }

    @Test void handlesEmptyData() {
        Task task = taskWith(Map.of("rawData", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(0, r.getOutputData().get("featureCount"));
    }

    @Test void handlesNullData() {
        Map<String, Object> input = new HashMap<>(); input.put("rawData", null);
        Task task = taskWith(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
