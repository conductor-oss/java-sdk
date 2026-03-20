package datasampling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FlagForReviewWorkerTest {

    private final FlagForReviewWorker worker = new FlagForReviewWorker();

    @Test
    void taskDefName() {
        assertEquals("sm_flag_for_review", worker.getTaskDefName());
    }

    @Test
    void flagsDatasetForReview() {
        List<String> issues = List.of("Record 0: missing name", "Record 2: negative value -5");
        Task task = taskWith(Map.of("totalRecords", 100, "qualityScore", 0.4, "issues", issues));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("approved"));
        assertEquals("FLAGGED_FOR_REVIEW", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsApprovedFalse() {
        Task task = taskWith(Map.of("totalRecords", 50, "qualityScore", 0.3, "issues", List.of("issue1")));
        TaskResult result = worker.execute(task);

        assertFalse((Boolean) result.getOutputData().get("approved"));
    }

    @Test
    void outputContainsFlaggedStatus() {
        Task task = taskWith(Map.of("totalRecords", 200, "qualityScore", 0.2, "issues", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("FLAGGED_FOR_REVIEW", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyIssuesList() {
        Task task = taskWith(Map.of("totalRecords", 10, "qualityScore", 0.0, "issues", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("approved"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("approved"));
        assertEquals("FLAGGED_FOR_REVIEW", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullIssues() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalRecords", 10);
        input.put("qualityScore", 0.5);
        input.put("issues", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("approved"));
    }

    @Test
    void handlesNullTotalRecordsAndQualityScore() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalRecords", null);
        input.put("qualityScore", null);
        input.put("issues", List.of("some issue"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FLAGGED_FOR_REVIEW", result.getOutputData().get("status"));
    }

    @Test
    void handlesMultipleIssues() {
        List<String> issues = List.of(
                "Record 0: missing name",
                "Record 1: negative value -10",
                "Record 3: non-numeric value 'abc'");
        Task task = taskWith(Map.of("totalRecords", 5, "qualityScore", 0.4, "issues", issues));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("approved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
