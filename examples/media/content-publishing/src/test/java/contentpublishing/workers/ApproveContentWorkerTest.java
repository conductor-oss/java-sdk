package contentpublishing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApproveContentWorkerTest {

    private final ApproveContentWorker worker = new ApproveContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pub_approve_content", worker.getTaskDefName());
    }

    @Test
    void approvesContentSuccessfully() {
        Task task = taskWith(Map.of(
                "contentId", "CNT-001",
                "reviewScore", 8.5,
                "reviewNotes", "Good content"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void outputContainsApprover() {
        Task task = taskWith(Map.of("contentId", "CNT-002", "reviewScore", 9.0));
        TaskResult result = worker.execute(task);

        assertEquals("chief-editor", result.getOutputData().get("approver"));
    }

    @Test
    void outputContainsApprovedAt() {
        Task task = taskWith(Map.of("contentId", "CNT-003", "reviewScore", 7.0));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T11:00:00Z", result.getOutputData().get("approvedAt"));
    }

    @Test
    void handlesNullReviewScore() {
        Map<String, Object> input = new HashMap<>();
        input.put("contentId", "CNT-004");
        input.put("reviewScore", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("approved"));
    }

    @Test
    void approvedIsBoolean() {
        Task task = taskWith(Map.of("contentId", "CNT-005", "reviewScore", 8.0));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Boolean.class, result.getOutputData().get("approved"));
    }

    @Test
    void outputHasAllExpectedKeys() {
        Task task = taskWith(Map.of("contentId", "CNT-006", "reviewScore", 8.5));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("approved"));
        assertTrue(result.getOutputData().containsKey("approver"));
        assertTrue(result.getOutputData().containsKey("approvedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
