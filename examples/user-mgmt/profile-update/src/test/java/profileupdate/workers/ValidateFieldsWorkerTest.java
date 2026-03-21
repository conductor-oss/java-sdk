package profileupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateFieldsWorkerTest {

    private final ValidateFieldsWorker worker = new ValidateFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("pfu_validate", worker.getTaskDefName());
    }

    @Test
    void validatesFields() {
        Task task = taskWith(Map.of("userId", "USR-123", "updates", Map.of("name", "Alice")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allValid"));
    }

    @Test
    void passesFieldsThrough() {
        Map<String, Object> updates = Map.of("bio", "Engineer");
        Task task = taskWith(Map.of("userId", "USR-123", "updates", updates));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("validatedFields"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullUpdates() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "USR-123");
        input.put("updates", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> validated = (Map<String, Object>) result.getOutputData().get("validatedFields");
        assertTrue(validated.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
