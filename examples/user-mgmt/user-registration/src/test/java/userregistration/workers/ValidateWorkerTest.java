package userregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {

    private final ValidateWorker worker = new ValidateWorker();

    @Test
    void taskDefName() {
        assertEquals("ur_validate", worker.getTaskDefName());
    }

    @Test
    void validUsernameAndEmail() {
        Task task = taskWith(Map.of("username", "bob_dev", "email", "bob@example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void shortUsernameIsInvalid() {
        Task task = taskWith(Map.of("username", "ab", "email", "a@b.com"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void emailWithoutAtIsInvalid() {
        Task task = taskWith(Map.of("username", "validuser", "email", "noemail"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("valid"));
    }

    @Test
    void checksOutputPresent() {
        Task task = taskWith(Map.of("username", "alice", "email", "alice@test.com"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("checks"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
