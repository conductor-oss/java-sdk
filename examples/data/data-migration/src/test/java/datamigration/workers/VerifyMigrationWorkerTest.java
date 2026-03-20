package datamigration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyMigrationWorkerTest {

    private final VerifyMigrationWorker worker = new VerifyMigrationWorker();

    @Test
    void taskDefName() {
        assertEquals("mi_verify_migration", worker.getTaskDefName());
    }

    @Test
    void successWhenRecordsLoaded() {
        Task task = taskWith(Map.of("sourceCount", 5, "loadedCount", 4));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SUCCESS", result.getOutputData().get("status"));
        assertTrue(((String) result.getOutputData().get("summary")).contains("4/5"));
        assertEquals(true, result.getOutputData().get("checksumMatch"));
    }

    @Test
    void failedWhenNoRecordsLoaded() {
        Task task = taskWith(Map.of("sourceCount", 5, "loadedCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals("FAILED", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullCounts() {
        Map<String, Object> input = new HashMap<>();
        input.put("sourceCount", null);
        input.put("loadedCount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FAILED", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
