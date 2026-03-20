package sessionmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidateSessionWorkerTest {
    private final ValidateSessionWorker worker = new ValidateSessionWorker();

    @Test void taskDefName() { assertEquals("ses_validate", worker.getTaskDefName()); }

    @Test void validatesSession() {
        Task task = taskWith(Map.of("sessionId", "SES-ABC", "userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test void includesRemainingTtl() {
        Task task = taskWith(Map.of("sessionId", "SES-ABC", "userId", "USR-123"));
        TaskResult result = worker.execute(task);
        assertEquals(3200, result.getOutputData().get("remainingTtl"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
