package contentpublishing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DistributeContentWorkerTest {

    private final DistributeContentWorker worker = new DistributeContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pub_distribute_content", worker.getTaskDefName());
    }

    @Test
    void distributesSuccessfully() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-001",
                "publishUrl", "https://blog.example.com/optimizing-cloud-costs",
                "title", "Optimizing Cloud Costs"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsChannels() {
        Task task = taskWith(Map.of("contentId", "CNT-002", "title", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) result.getOutputData().get("channels");
        assertEquals(4, channels.size());
        assertTrue(channels.contains("twitter"));
        assertTrue(channels.contains("linkedin"));
    }

    @Test
    void outputContainsScheduledPosts() {
        Task task = taskWith(Map.of("contentId", "CNT-003", "title", "Post"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("scheduledPosts"));
    }

    @Test
    void outputContainsDistributedAt() {
        Task task = taskWith(Map.of("contentId", "CNT-004", "title", "Article"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T12:15:00Z", result.getOutputData().get("distributedAt"));
    }

    @Test
    void handlesNullTitle() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-005");
        input.put("title", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("channels"));
    }

    @Test
    void channelsContainsNewsletter() {
        Task task = taskWith(Map.of("contentId", "CNT-006", "title", "Newsletter Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) result.getOutputData().get("channels");
        assertTrue(channels.contains("newsletter"));
        assertTrue(channels.contains("rss"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
