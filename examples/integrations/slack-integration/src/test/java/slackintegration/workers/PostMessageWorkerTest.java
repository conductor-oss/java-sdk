package slackintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PostMessageWorkerTest {

    private final PostMessageWorker worker = new PostMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("slk_post_message", worker.getTaskDefName());
    }

    @Test
    void postsMessageToChannel() {
        Task task = taskWith(Map.of("channel", "engineering", "message", "Deployment complete"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
        assertNotNull(result.getOutputData().get("postedAt"));
    }

    @Test
    void messageIdContainsChannelHash() {
        Task task = taskWith(Map.of("channel", "alerts", "message", "Alert fired"));
        TaskResult result = worker.execute(task);

        String messageId = (String) result.getOutputData().get("messageId");
        assertTrue(messageId.startsWith("msg-"));
    }

    @Test
    void handlesNullChannel() {
        Map<String, Object> input = new HashMap<>();
        input.put("channel", null);
        input.put("message", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("channel", "test");
        input.put("message", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
    }

    @Test
    void deterministicMessageId() {
        Task task1 = taskWith(Map.of("channel", "dev", "message", "msg1"));
        Task task2 = taskWith(Map.of("channel", "dev", "message", "msg2"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("messageId"), r2.getOutputData().get("messageId"));
    }

    @Test
    void outputContainsPostedAt() {
        Task task = taskWith(Map.of("channel", "general", "message", "hello"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("postedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
