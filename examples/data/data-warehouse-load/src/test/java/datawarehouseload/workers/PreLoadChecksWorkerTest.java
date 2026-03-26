package datawarehouseload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PreLoadChecksWorkerTest {

    private final PreLoadChecksWorker worker = new PreLoadChecksWorker();

    @Test
    void taskDefName() {
        assertEquals("wh_pre_load_checks", worker.getTaskDefName());
    }

    @Test
    void passesWithValidRecords() {
        Task task = taskWith(Map.of("recordCount", 5, "stagingTable", "stg_test", "schema", "analytics"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("passed"));
        assertEquals(5, result.getOutputData().get("validCount"));
    }

    @Test
    void checksContainAllFields() {
        Task task = taskWith(Map.of("recordCount", 3));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertEquals(true, checks.get("nullCheck"));
        assertEquals(true, checks.get("typeCheck"));
        assertEquals(true, checks.get("uniqueCheck"));
    }

    @Test
    void handlesZeroRecords() {
        Task task = taskWith(Map.of("recordCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
    }

    @Test
    void handlesMissingRecordCount() {
        Task task = taskWith(Map.of("stagingTable", "stg_test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("validCount"));
    }

    @Test
    void validCountMatchesInput() {
        Task task = taskWith(Map.of("recordCount", 42));
        TaskResult result = worker.execute(task);

        assertEquals(42, result.getOutputData().get("validCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
