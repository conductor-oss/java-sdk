package datacatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyDataWorkerTest {

    private final ClassifyDataWorker worker = new ClassifyDataWorker();

    @Test
    void taskDefName() {
        assertEquals("cg_classify_data", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("assets", List.of(
                Map.of("name", "users", "schema", "public", "columns", List.of("id", "name")))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void detectsPII() {
        Task task = taskWith(Map.of("assets", List.of(
                Map.of("name", "users", "schema", "public", "columns", List.of("id", "email", "phone")))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> classified = (List<Map<String, Object>>) result.getOutputData().get("classified");
        assertTrue((Boolean) classified.get(0).get("hasPII"));
        assertEquals("high", classified.get(0).get("sensitivity"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void noPIIWhenNoSensitiveColumns() {
        Task task = taskWith(Map.of("assets", List.of(
                Map.of("name", "products", "schema", "public", "columns", List.of("id", "name", "price")))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> classified = (List<Map<String, Object>>) result.getOutputData().get("classified");
        assertFalse((Boolean) classified.get(0).get("hasPII"));
        assertEquals("low", classified.get(0).get("sensitivity"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void categorizesAnalyticsSchema() {
        Task task = taskWith(Map.of("assets", List.of(
                Map.of("name", "events", "schema", "analytics", "columns", List.of("id")))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> classified = (List<Map<String, Object>>) result.getOutputData().get("classified");
        assertEquals("analytics", classified.get(0).get("category"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void categorizesReportingSchema() {
        Task task = taskWith(Map.of("assets", List.of(
                Map.of("name", "revenue", "schema", "reporting", "columns", List.of("date")))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> classified = (List<Map<String, Object>>) result.getOutputData().get("classified");
        assertEquals("reporting", classified.get(0).get("category"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void classificationSummaryHasCounts() {
        Task task = taskWith(Map.of("assets", List.of(
                Map.of("name", "t1", "schema", "public", "columns", List.of("id", "email")),
                Map.of("name", "t2", "schema", "analytics", "columns", List.of("id")),
                Map.of("name", "t3", "schema", "reporting", "columns", List.of("date")))));
        TaskResult result = worker.execute(task);
        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("classificationSummary");
        assertEquals(1, summary.get("operational"));
        assertEquals(1, summary.get("analytics"));
        assertEquals(1, summary.get("reporting"));
        assertEquals(1, summary.get("piiAssets"));
    }

    @Test
    void handlesNullAssets() {
        Map<String, Object> input = new HashMap<>();
        input.put("assets", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
