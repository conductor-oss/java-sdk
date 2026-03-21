package userregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActivateWorkerTest {

    private final ActivateWorker worker = new ActivateWorker();

    @Test
    void taskDefName() {
        assertEquals("ur_activate", worker.getTaskDefName());
    }

    @Test
    void activatesUser() {
        Task task = taskWith(Map.of("userId", "USR-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("active"));
    }

    @Test
    void outputContainsActivatedAt() {
        Task task = taskWith(Map.of("userId", "USR-456"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("activatedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
