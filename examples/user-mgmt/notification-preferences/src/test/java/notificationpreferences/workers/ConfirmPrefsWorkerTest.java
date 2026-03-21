package notificationpreferences.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmPrefsWorkerTest {

    private final ConfirmPrefsWorker worker = new ConfirmPrefsWorker();

    @Test
    void taskDefName() {
        assertEquals("np_confirm", worker.getTaskDefName());
    }

    @Test
    void confirmsUpdate() {
        Task task = taskWith(Map.of("userId", "USR-123", "channels", List.of("email", "push")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("userId", "USR-456", "channels", List.of()));
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
