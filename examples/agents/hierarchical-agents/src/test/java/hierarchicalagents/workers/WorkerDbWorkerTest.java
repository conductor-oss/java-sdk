package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkerDbWorkerTest {

    private final WorkerDbWorker worker = new WorkerDbWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_worker_db", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void producesTablesWithNameAndStatus() {
        Map<String, Object> dbTask = Map.of(
                "tables", List.of("users", "projects"),
                "orm", "JPA"
        );
        Task task = taskWith(Map.of("task", dbTask));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> dbResult = (Map<String, Object>) result.getOutputData().get("result");
        List<Map<String, Object>> tables = (List<Map<String, Object>>) dbResult.get("tables");
        assertEquals(2, tables.size());
        assertEquals("users", tables.get(0).get("name"));
        assertEquals("created", tables.get(0).get("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resultContainsMigrationsSeedDataAndLinesOfCode() {
        Map<String, Object> dbTask = Map.of("tables", List.of("accounts"));
        Task task = taskWith(Map.of("task", dbTask));
        TaskResult result = worker.execute(task);

        Map<String, Object> dbResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(3, dbResult.get("migrations"));
        assertEquals(true, dbResult.get("seedData"));
        assertEquals(180, dbResult.get("linesOfCode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void acceptsApiOutputContext() {
        Map<String, Object> dbTask = Map.of("tables", List.of("items"));
        Map<String, Object> apiOutput = Map.of("endpoints", List.of());
        Task task = taskWith(Map.of("task", dbTask, "apiOutput", apiOutput));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> dbResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(dbResult.get("tables"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullTask() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> dbResult = (Map<String, Object>) result.getOutputData().get("result");
        List<?> tables = (List<?>) dbResult.get("tables");
        assertTrue(tables.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
