package notificationpreferences.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdatePrefsWorkerTest {

    private final UpdatePrefsWorker worker = new UpdatePrefsWorker();

    @Test
    void taskDefName() {
        assertEquals("np_update", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void mergesPreferences() {
        Task task = taskWith(Map.of(
                "userId", "USR-123",
                "current", new HashMap<>(Map.of("email", true, "sms", false)),
                "newPrefs", new HashMap<>(Map.of("sms", true))
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> updated = (Map<String, Object>) result.getOutputData().get("updated");
        assertEquals(true, updated.get("sms"));
        assertEquals(true, updated.get("email"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullCurrent() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "USR-123");
        input.put("current", null);
        input.put("newPrefs", Map.of("email", true));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        Map<String, Object> updated = (Map<String, Object>) result.getOutputData().get("updated");
        assertEquals(true, updated.get("email"));
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("userId", "USR-123", "current", Map.of(), "newPrefs", Map.of()));
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
