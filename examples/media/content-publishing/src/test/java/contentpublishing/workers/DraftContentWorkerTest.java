package contentpublishing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DraftContentWorkerTest {

    private final DraftContentWorker worker = new DraftContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pub_draft_content", worker.getTaskDefName());
    }

    @Test
    void createsDraftSuccessfully() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-001",
                "authorId", "AUTH-220",
                "contentType", "blog_post",
                "title", "Optimizing Cloud Costs"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("draftVersion"));
        assertEquals(1450, result.getOutputData().get("wordCount"));
    }

    @Test
    void outputContainsReadTime() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-002",
                "authorId", "AUTH-100",
                "title", "Test Article"));
        TaskResult result = worker.execute(task);

        assertEquals("6 min", result.getOutputData().get("readTime"));
    }

    @Test
    void outputContainsSlug() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-003",
                "authorId", "AUTH-100",
                "title", "Another Article"));
        TaskResult result = worker.execute(task);

        assertEquals("optimizing-cloud-costs", result.getOutputData().get("slug"));
    }

    @Test
    void handlesNullTitle() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-004");
        input.put("authorId", "AUTH-100");
        input.put("title", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullAuthorId() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-005");
        input.put("authorId", null);
        input.put("title", "Test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("draftVersion"));
        assertNotNull(result.getOutputData().get("wordCount"));
    }

    @Test
    void draftVersionIsOne() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-006",
                "authorId", "AUTH-300",
                "title", "First Draft"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("draftVersion"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
