package userregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateWorkerTest {

    private final CreateWorker worker = new CreateWorker();

    @Test
    void taskDefName() {
        assertEquals("ur_create", worker.getTaskDefName());
    }

    @Test
    void createsUserWithId() {
        Task task = taskWith(Map.of("username", "bob_dev", "email", "bob@test.com", "valid", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().get("userId").toString().startsWith("USR-"));
    }

    @Test
    void outputContainsUsername() {
        Task task = taskWith(Map.of("username", "alice", "email", "a@b.com", "valid", true));
        TaskResult result = worker.execute(task);

        assertEquals("alice", result.getOutputData().get("username"));
    }

    @Test
    void outputContainsCreatedAt() {
        Task task = taskWith(Map.of("username", "charlie", "email", "c@d.com", "valid", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("createdAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
