package contentpublishing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewContentWorkerTest {

    private final ReviewContentWorker worker = new ReviewContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pub_review_content", worker.getTaskDefName());
    }

    @Test
    void reviewsContentSuccessfully() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-001",
                "draftVersion", 1,
                "wordCount", 1450));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(8.5, result.getOutputData().get("reviewScore"));
    }

    @Test
    void outputContainsReviewer() {
        Task task = taskWith(Map.of("contentId", "CNT-002", "draftVersion", 1));
        TaskResult result = worker.execute(task);

        assertEquals("editor-01", result.getOutputData().get("reviewer"));
    }

    @Test
    void outputContainsReviewNotes() {
        Task task = taskWith(Map.of("contentId", "CNT-003", "draftVersion", 2));
        TaskResult result = worker.execute(task);

        assertEquals("Strong intro, needs minor copy edits", result.getOutputData().get("reviewNotes"));
    }

    @Test
    void handlesNullDraftVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-004");
        input.put("draftVersion", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullWordCount() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-005");
        input.put("wordCount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reviewScore"));
    }

    @Test
    void reviewScoreIsNumeric() {
        Task task = taskWith(Map.of("contentId", "CNT-006", "draftVersion", 1, "wordCount", 500));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Number.class, result.getOutputData().get("reviewScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
