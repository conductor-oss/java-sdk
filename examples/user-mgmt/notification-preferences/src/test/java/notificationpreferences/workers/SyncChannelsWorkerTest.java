package notificationpreferences.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SyncChannelsWorkerTest {

    private final SyncChannelsWorker worker = new SyncChannelsWorker();

    @Test
    void taskDefName() {
        assertEquals("np_sync_channels", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void syncsActiveChannels() {
        Task task = taskWith(Map.of("userId", "USR-123", "updated", new HashMap<>(Map.of("email", true, "sms", false, "push", true))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> channels = (List<String>) result.getOutputData().get("syncedChannels");
        assertEquals(2, channels.size());
    }

    @Test
    void includesSyncedAt() {
        Task task = taskWith(Map.of("userId", "USR-123", "updated", Map.of("email", true)));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("syncedAt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullPrefs() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "USR-123");
        input.put("updated", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        List<String> channels = (List<String>) result.getOutputData().get("syncedChannels");
        assertTrue(channels.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
