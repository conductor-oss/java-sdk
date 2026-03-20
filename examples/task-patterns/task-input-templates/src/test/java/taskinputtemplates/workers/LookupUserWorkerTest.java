package taskinputtemplates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupUserWorkerTest {

    private final LookupUserWorker worker = new LookupUserWorker();

    @Test
    void taskDefName() {
        assertEquals("tpl_lookup_user", worker.getTaskDefName());
    }

    @Test
    void looksUpKnownAdminUser() {
        Task task = taskWith(Map.of("userId", "U-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, String> user = (Map<String, String>) result.getOutputData().get("user");
        assertEquals("Alice", user.get("name"));
        assertEquals("admin", user.get("role"));
        assertEquals("Engineering", user.get("department"));
        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void looksUpKnownViewerUser() {
        Task task = taskWith(Map.of("userId", "U-2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, String> user = (Map<String, String>) result.getOutputData().get("user");
        assertEquals("Bob", user.get("name"));
        assertEquals("viewer", user.get("role"));
        assertEquals("Marketing", user.get("department"));
    }

    @Test
    void returnsGuestForUnknownUser() {
        Task task = taskWith(Map.of("userId", "U-999"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, String> user = (Map<String, String>) result.getOutputData().get("user");
        assertEquals("Unknown", user.get("name"));
        assertEquals("guest", user.get("role"));
    }

    @Test
    void outputContainsTimestamp() {
        Task task = taskWith(Map.of("userId", "U-1"));
        TaskResult result = worker.execute(task);

        String timestamp = (String) result.getOutputData().get("timestamp");
        assertNotNull(timestamp);
        assertFalse(timestamp.isBlank());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
