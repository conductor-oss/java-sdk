package approvalcomments.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyFeedbackWorkerTest {

    @Test
    void taskDefName() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        assertEquals("ac_apply_feedback", worker.getTaskDefName());
    }

    @Test
    void appliesFeedbackWithAllFields() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(Map.of(
                "decision", "approved",
                "comments", "Looks great, minor typo on page 3",
                "attachments", List.of("review-notes.pdf", "screenshot.png"),
                "attachmentCount", 2,
                "rating", 4,
                "tags", List.of("urgent", "legal-review")
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesApprovedDecision() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(Map.of(
                "decision", "approved",
                "comments", "All good",
                "attachments", List.of(),
                "rating", 5,
                "tags", List.of()
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesRejectedDecision() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(Map.of(
                "decision", "rejected",
                "comments", "Does not meet requirements",
                "attachments", List.of("rejection-reasons.pdf"),
                "rating", 1,
                "tags", List.of("needs-rework")
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesMissingFields() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesNullDecision() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("decision", null);
        input.put("comments", null);
        input.put("attachments", null);
        input.put("rating", null);
        input.put("tags", null);
        Task task = createTask(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesEmptyComments() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(Map.of(
                "decision", "approved",
                "comments", "",
                "attachments", List.of(),
                "rating", 3,
                "tags", List.of()
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesMultipleAttachments() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(Map.of(
                "decision", "approved",
                "comments", "See attachments for details",
                "attachments", List.of("file1.pdf", "file2.docx", "file3.png"),
                "attachmentCount", 3,
                "rating", 4,
                "tags", List.of("reviewed", "documentation")
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void handlesNonNumericRating() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("decision", "approved");
        input.put("comments", "ok");
        input.put("attachments", List.of());
        input.put("rating", "not-a-number");
        input.put("tags", List.of());
        Task task = createTask(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void outputAlwaysContainsAppliedKey() {
        ApplyFeedbackWorker worker = new ApplyFeedbackWorker();
        Task task = createTask(Map.of("decision", "approved"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("applied"));
    }

    private Task createTask(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
