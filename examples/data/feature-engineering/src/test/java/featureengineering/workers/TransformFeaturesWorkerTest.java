package featureengineering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformFeaturesWorkerTest {

    private final TransformFeaturesWorker worker = new TransformFeaturesWorker();

    @Test void taskDefName() { assertEquals("fe_transform_features", worker.getTaskDefName()); }

    @Test void returnsCompleted() {
        Task task = taskWith(Map.of("features", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", 1, "is_active", 1, "balance", 10000))));
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    @Test @SuppressWarnings("unchecked")
    void addsLogIncome() {
        Task task = taskWith(Map.of("features", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", 1, "is_active", 1, "balance", 10000))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> t = (List<Map<String, Object>>) r.getOutputData().get("transformed");
        assertNotNull(t.get(0).get("log_income"));
        assertTrue(((Number) t.get(0).get("log_income")).doubleValue() > 0);
    }

    @Test @SuppressWarnings("unchecked")
    void addsAgeSquared() {
        Task task = taskWith(Map.of("features", List.of(Map.of("age", 5, "income", 100, "tenure_months", 12, "num_products", 1, "has_credit_card", 0, "is_active", 1, "balance", 50))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> t = (List<Map<String, Object>>) r.getOutputData().get("transformed");
        assertEquals(25, ((Number) t.get(0).get("age_squared")).intValue());
    }

    @Test @SuppressWarnings("unchecked")
    void incomePerProductHandlesZero() {
        Task task = taskWith(Map.of("features", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 0, "has_credit_card", 1, "is_active", 1, "balance", 10000))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> t = (List<Map<String, Object>>) r.getOutputData().get("transformed");
        assertEquals(0.0, ((Number) t.get(0).get("income_per_product")).doubleValue());
    }

    @Test void transformedCountIncludesDerived() {
        Task task = taskWith(Map.of("features", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 24, "num_products", 2, "has_credit_card", 1, "is_active", 1, "balance", 10000))));
        TaskResult r = worker.execute(task);
        assertEquals(12, r.getOutputData().get("transformedCount")); // 7 original + 5 derived
    }

    @Test @SuppressWarnings("unchecked")
    void tenureYearsCalculated() {
        Task task = taskWith(Map.of("features", List.of(Map.of("age", 30, "income", 50000, "tenure_months", 48, "num_products", 2, "has_credit_card", 1, "is_active", 1, "balance", 10000))));
        TaskResult r = worker.execute(task);
        List<Map<String, Object>> t = (List<Map<String, Object>>) r.getOutputData().get("transformed");
        assertEquals(4.0, ((Number) t.get(0).get("tenure_years")).doubleValue());
    }

    @Test void handlesEmptyFeatures() {
        Task task = taskWith(Map.of("features", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(0, r.getOutputData().get("transformedCount"));
    }

    @Test void handlesNullFeatures() {
        Map<String, Object> input = new HashMap<>(); input.put("features", null);
        Task task = taskWith(input);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
