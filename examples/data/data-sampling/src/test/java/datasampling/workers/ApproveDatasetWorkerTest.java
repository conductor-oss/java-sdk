package datasampling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApproveDatasetWorkerTest {

    private final ApproveDatasetWorker worker = new ApproveDatasetWorker();

    @Test
    void taskDefName() {
        assertEquals("sm_approve_dataset", worker.getTaskDefName());
    }

    @Test
    void approvesDatasetSuccessfully() {
        Task task = taskWith(Map.of("totalRecords", 100, "qualityScore", 0.95));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
        assertEquals("APPROVED", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsApprovedTrue() {
        Task task = taskWith(Map.of("totalRecords", 50, "qualityScore", 1.0));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("approved"));
    }

    @Test
    void outputContainsApprovedStatus() {
        Task task = taskWith(Map.of("totalRecords", 200, "qualityScore", 0.85));
        TaskResult result = worker.execute(task);

        assertEquals("APPROVED", result.getOutputData().get("status"));
    }

    @Test
    void handlesSmallDataset() {
        Task task = taskWith(Map.of("totalRecords", 1, "qualityScore", 1.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
        assertEquals("APPROVED", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullTotalRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalRecords", null);
        input.put("qualityScore", 0.9);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesNullQualityScore() {
        Map<String, Object> input = new HashMap<>();
        input.put("totalRecords", 10);
        input.put("qualityScore", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("APPROVED", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
