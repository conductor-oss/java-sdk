package slackintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrackDeliveryWorkerTest {

    private final TrackDeliveryWorker worker = new TrackDeliveryWorker();

    @Test
    void taskDefName() {
        assertEquals("slk_track_delivery", worker.getTaskDefName());
    }

    @Test
    void tracksDeliverySuccessfully() {
        Task task = taskWith(Map.of("messageId", "msg-123", "channel", "engineering"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
        assertNotNull(result.getOutputData().get("trackedAt"));
    }

    @Test
    void handlesNullMessageId() {
        Map<String, Object> input = new HashMap<>();
        input.put("messageId", null);
        input.put("channel", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void handlesNullChannel() {
        Map<String, Object> input = new HashMap<>();
        input.put("messageId", "msg-456");
        input.put("channel", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void alwaysReportsDelivered() {
        Task task = taskWith(Map.of("messageId", "msg-any", "channel", "any"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test
    void outputContainsTrackedAt() {
        Task task = taskWith(Map.of("messageId", "msg-789", "channel", "ops"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("trackedAt"));
    }

    @Test
    void tracksDifferentChannels() {
        Task task1 = taskWith(Map.of("messageId", "msg-1", "channel", "alerts"));
        Task task2 = taskWith(Map.of("messageId", "msg-2", "channel", "dev"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(true, r1.getOutputData().get("delivered"));
        assertEquals(true, r2.getOutputData().get("delivered"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
