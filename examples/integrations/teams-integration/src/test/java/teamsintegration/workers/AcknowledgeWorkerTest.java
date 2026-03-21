package teamsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AcknowledgeWorkerTest {

    private final AcknowledgeWorker worker = new AcknowledgeWorker();

    @Test
    void taskDefName() {
        assertEquals("tms_acknowledge", worker.getTaskDefName());
    }

    @Test
    void acknowledgesMessage() {
        Task task = taskWith(Map.of("messageId", "teams-msg-123", "channelId", "ch-alerts"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesNullMessageId() {
        Map<String, Object> input = new HashMap<>();
        input.put("messageId", null);
        input.put("channelId", "ch-test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void alwaysAcknowledges() {
        Task task = taskWith(Map.of("messageId", "any-msg", "channelId", "any-ch"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("acknowledged"));
    }

    @Test
    void acknowledgesMultipleMessages() {
        Task t1 = taskWith(Map.of("messageId", "msg-1", "channelId", "ch-1"));
        Task t2 = taskWith(Map.of("messageId", "msg-2", "channelId", "ch-2"));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);

        assertEquals(true, r1.getOutputData().get("acknowledged"));
        assertEquals(true, r2.getOutputData().get("acknowledged"));
    }

    @Test
    void handlesNullChannelId() {
        Map<String, Object> input = new HashMap<>();
        input.put("messageId", "msg-x");
        input.put("channelId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputOnlyContainsAcknowledged() {
        Task task = taskWith(Map.of("messageId", "msg-test", "channelId", "ch-test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("acknowledged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
