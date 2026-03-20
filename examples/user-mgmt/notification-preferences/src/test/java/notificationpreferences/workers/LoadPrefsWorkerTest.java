package notificationpreferences.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadPrefsWorkerTest {

    private final LoadPrefsWorker worker = new LoadPrefsWorker();

    @Test
    void taskDefName() {
        assertEquals("np_load", worker.getTaskDefName());
    }

    @Test
    void loadsCurrentPreferences() {
        Task task = taskWith(Map.of("userId", "USR-NP001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("current"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void currentPrefsContainExpectedKeys() {
        Task task = taskWith(Map.of("userId", "USR-NP001"));
        TaskResult result = worker.execute(task);

        Map<String, Object> current = (Map<String, Object>) result.getOutputData().get("current");
        assertTrue(current.containsKey("email"));
        assertTrue(current.containsKey("sms"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
