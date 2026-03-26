package profileupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProfileWorkerTest {

    private final UpdateProfileWorker worker = new UpdateProfileWorker();

    @Test
    void taskDefName() {
        assertEquals("pfu_update", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void updatesFields() {
        Task task = taskWith(Map.of("userId", "USR-123", "validatedFields", Map.of("name", "Alice", "bio", "Dev")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> fields = (List<String>) result.getOutputData().get("updatedFields");
        assertEquals(2, fields.size());
    }

    @Test
    void includesUpdatedAt() {
        Task task = taskWith(Map.of("userId", "USR-123", "validatedFields", Map.of("name", "Bob")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("updatedAt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullFields() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "USR-123");
        input.put("validatedFields", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        List<String> fields = (List<String>) result.getOutputData().get("updatedFields");
        assertTrue(fields.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
