package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadBackendWorkerTest {

    private final LeadBackendWorker worker = new LeadBackendWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_lead_backend", worker.getTaskDefName());
    }

    @Test
    void producesSummaryAndTasks() {
        Map<String, Object> workstream = Map.of(
                "scope", "Build REST API",
                "endpoints", List.of("/api/users", "/api/tasks"),
                "priority", "high"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
        assertNotNull(result.getOutputData().get("apiTask"));
        assertNotNull(result.getOutputData().get("dbTask"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void apiTaskContainsEndpointsAuthValidation() {
        Map<String, Object> workstream = Map.of(
                "scope", "API layer",
                "endpoints", List.of("/api/data"),
                "priority", "high"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        Map<String, Object> apiTask = (Map<String, Object>) result.getOutputData().get("apiTask");
        List<String> endpoints = (List<String>) apiTask.get("endpoints");
        assertEquals(List.of("/api/data"), endpoints);
        assertEquals("JWT", apiTask.get("auth"));
        assertEquals("schema-based", apiTask.get("validation"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void dbTaskContainsTablesAndOrm() {
        Map<String, Object> workstream = Map.of(
                "scope", "DB layer",
                "endpoints", List.of(),
                "priority", "medium"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        Map<String, Object> dbTask = (Map<String, Object>) result.getOutputData().get("dbTask");
        List<String> tables = (List<String>) dbTask.get("tables");
        assertFalse(tables.isEmpty());
        assertEquals("JPA", dbTask.get("orm"));
    }

    @Test
    void summaryContainsScope() {
        Map<String, Object> workstream = Map.of(
                "scope", "Build payment API",
                "endpoints", List.of(),
                "priority", "high"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("Build payment API"));
    }

    @Test
    void handlesNullWorkstream() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("apiTask"));
        assertNotNull(result.getOutputData().get("dbTask"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
