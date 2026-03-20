package eventdedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckSeenWorkerTest {

    private final CheckSeenWorker worker = new CheckSeenWorker();

    @Test
    void taskDefName() {
        assertEquals("dd_check_seen", worker.getTaskDefName());
    }

    @Test
    void returnsDuplicateStatus() {
        Task task = taskWith(Map.of("hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("duplicate", result.getOutputData().get("status"));
    }

    @Test
    void returnsCheckedAtTimestamp() {
        Task task = taskWith(Map.of("hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("checkedAt"));
    }

    @Test
    void statusIsDuplicate() {
        Task task = taskWith(Map.of("hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        String status = (String) result.getOutputData().get("status");
        assertEquals("duplicate", status);
    }

    @Test
    void outputContainsStatusAndCheckedAt() {
        Task task = taskWith(Map.of("hash", "sha256_fixed_hash_001"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("status"));
        assertTrue(result.getOutputData().containsKey("checkedAt"));
    }

    @Test
    void handlesAnyHashValue() {
        Task task = taskWith(Map.of("hash", "some_other_hash_value"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("duplicate", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullHash() {
        Map<String, Object> input = new HashMap<>();
        input.put("hash", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("duplicate", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("duplicate", result.getOutputData().get("status"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("checkedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
