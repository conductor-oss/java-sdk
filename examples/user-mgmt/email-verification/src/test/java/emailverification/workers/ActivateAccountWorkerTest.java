package emailverification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActivateAccountWorkerTest {

    private final ActivateAccountWorker worker = new ActivateAccountWorker();

    @Test
    void taskDefName() {
        assertEquals("emv_activate", worker.getTaskDefName());
    }

    @Test
    void activatesAccount() {
        Task task = taskWith(Map.of("userId", "USR-D4E5F6", "verified", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("activated"));
    }

    @Test
    void includesActivatedAt() {
        Task task = taskWith(Map.of("userId", "USR-123", "verified", true));
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
