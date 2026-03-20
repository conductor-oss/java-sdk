package contentpublishing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PublishContentWorkerTest {

    private final PublishContentWorker worker = new PublishContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pub_publish_content", worker.getTaskDefName());
    }

    @Test
    void publishesSuccessfully() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-001",
                "formattedUrl", "https://cms.example.com/content/511-formatted"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://blog.example.com/optimizing-cloud-costs",
                result.getOutputData().get("publishUrl"));
    }

    @Test
    void outputContainsPublishedAt() {
        Task task = taskWith(Map.of("contentId", "CNT-002"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T12:00:00Z", result.getOutputData().get("publishedAt"));
    }

    @Test
    void outputContainsCacheInvalidated() {
        Task task = taskWith(Map.of("contentId", "CNT-003"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("cacheInvalidated"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void publishUrlIsString() {
        Task task = taskWith(Map.of("contentId", "CNT-004"));
        TaskResult result = worker.execute(task);

        assertInstanceOf(String.class, result.getOutputData().get("publishUrl"));
    }

    @Test
    void cacheInvalidatedIsBoolean() {
        Task task = taskWith(Map.of("contentId", "CNT-005"));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Boolean.class, result.getOutputData().get("cacheInvalidated"));
    }

    @Test
    void outputHasAllExpectedKeys() {
        Task task = taskWith(Map.of("contentId", "CNT-006"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("publishUrl"));
        assertTrue(result.getOutputData().containsKey("publishedAt"));
        assertTrue(result.getOutputData().containsKey("cacheInvalidated"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
