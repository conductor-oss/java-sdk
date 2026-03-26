package profileupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifyChangesWorkerTest {

    private final NotifyChangesWorker worker = new NotifyChangesWorker();

    @Test
    void taskDefName() {
        assertEquals("pfu_notify", worker.getTaskDefName());
    }

    @Test
    void notifiesUser() {
        Task task = taskWith(Map.of("userId", "USR-123", "changes", List.of("name")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("userId", "USR-456", "changes", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
