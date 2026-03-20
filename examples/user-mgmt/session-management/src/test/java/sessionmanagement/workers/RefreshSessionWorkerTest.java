package sessionmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RefreshSessionWorkerTest {
    private final RefreshSessionWorker worker = new RefreshSessionWorker();

    @Test void taskDefName() { assertEquals("ses_refresh", worker.getTaskDefName()); }

    @Test void refreshesSession() {
        Task task = taskWith(Map.of("sessionId", "SES-ABC"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("refreshed"));
    }

    @Test void newExpiresIn() {
        Task task = taskWith(Map.of("sessionId", "SES-ABC"));
        TaskResult result = worker.execute(task);
        assertEquals(3600, result.getOutputData().get("newExpiresIn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
