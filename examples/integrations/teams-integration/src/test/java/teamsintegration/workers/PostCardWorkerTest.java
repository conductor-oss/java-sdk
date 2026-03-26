package teamsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PostCardWorkerTest {

    private final PostCardWorker worker = new PostCardWorker();

    @Test
    void taskDefName() {
        assertEquals("tms_post_card", worker.getTaskDefName());
    }

    @Test
    void postsCardToChannel() {
        Task task = taskWith(Map.of("channelId", "ch-alerts", "card", Map.of("type", "AdaptiveCard")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("messageId"));
        assertNotNull(result.getOutputData().get("postedAt"));
    }

    @Test
    void messageIdContainsTeamsPrefix() {
        Task task = taskWith(Map.of("channelId", "ch-dev", "card", Map.of()));
        TaskResult result = worker.execute(task);

        String messageId = (String) result.getOutputData().get("messageId");
        assertTrue(messageId.startsWith("teams-msg-"));
    }

    @Test
    void handlesNullChannelId() {
        Map<String, Object> input = new HashMap<>();
        input.put("channelId", null);
        input.put("card", Map.of());
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
    void deterministicMessageIdForSameChannel() {
        Task t1 = taskWith(Map.of("channelId", "ch-x", "card", Map.of()));
        Task t2 = taskWith(Map.of("channelId", "ch-x", "card", Map.of()));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);

        assertEquals(r1.getOutputData().get("messageId"), r2.getOutputData().get("messageId"));
    }

    @Test
    void outputContainsPostedAt() {
        Task task = taskWith(Map.of("channelId", "ch-general", "card", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("postedAt"));
    }

    @Test
    void differentChannelsProduceDifferentIds() {
        Task t1 = taskWith(Map.of("channelId", "ch-a", "card", Map.of()));
        Task t2 = taskWith(Map.of("channelId", "ch-b", "card", Map.of()));
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);

        assertNotEquals(r1.getOutputData().get("messageId"), r2.getOutputData().get("messageId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
