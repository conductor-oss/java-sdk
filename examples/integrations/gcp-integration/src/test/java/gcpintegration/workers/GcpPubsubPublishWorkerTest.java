package gcpintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GcpPubsubPublishWorkerTest {

    private final GcpPubsubPublishWorker worker = new GcpPubsubPublishWorker();

    @Test
    void taskDefName() {
        assertEquals("gcp_pubsub_publish", worker.getTaskDefName());
    }

    @Test
    void publishesToTopic() {
        Task task = taskWith(Map.of(
                "topic", "projects/my-proj/topics/data-events",
                "message", Map.of("id", "evt-6001")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("projects/my-proj/topics/data-events", result.getOutputData().get("topic"));
    }

    @Test
    void outputContainsMessageId() {
        Task task = taskWith(Map.of("topic", "projects/p/topics/t"));
        TaskResult result = worker.execute(task);

        assertEquals("pubsub-msg-fixed-001", result.getOutputData().get("messageId"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("projects/default/topics/default", result.getOutputData().get("topic"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
    }

    @Test
    void messageIdIsDeterministic() {
        Task t1 = taskWith(Map.of("topic", "t1"));
        Task t2 = taskWith(Map.of("topic", "t2"));
        assertEquals(
                worker.execute(t1).getOutputData().get("messageId"),
                worker.execute(t2).getOutputData().get("messageId"));
    }

    @Test
    void topicPassedThrough() {
        Task task = taskWith(Map.of("topic", "projects/prod/topics/orders"));
        TaskResult result = worker.execute(task);

        assertEquals("projects/prod/topics/orders", result.getOutputData().get("topic"));
    }

    @Test
    void completesWithMapMessage() {
        Task task = taskWith(Map.of(
                "topic", "projects/p/topics/t",
                "message", Map.of("event", "signup")));
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
